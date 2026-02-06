from fastapi import FastAPI, APIRouter, HTTPException, Depends, Security, status
from fastapi.security.api_key import APIKeyHeader
from datetime import datetime, timezone
from typing import Optional
from pydantic import BaseModel, Field, ConfigDict
import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry
import yfinance as yf
from alpaca.data.historical import StockHistoricalDataClient
from alpaca.data.requests import StockBarsRequest
from alpaca.data.timeframe import TimeFrame
from alpaca.data.enums import DataFeed
import os
import logging
from dotenv import load_dotenv
import hashlib
import hmac
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded
from starlette.requests import Request

logger = logging.getLogger(__name__)

# ─── Environment ────────────────────────────────────────────────────────────
load_dotenv()  # <-- this reads .env into os.environ

KEY = os.getenv("ALPACA_API_KEY")
SECRET = os.getenv("ALPACA_API_SECRET")
ACCESS_KEY_HASH = os.getenv("ACCESS_KEY_HASH")

if not KEY or not SECRET or not ACCESS_KEY_HASH:
    raise RuntimeError("ALPACA_API_KEY, ALPACA_API_SECRET and ACCESS_KEY_HASH must be set in .env")

# Paper trading keys start with "PK", live keys start with "AK"
ALPACA_TRADING_BASE_URL = (
    "https://paper-api.alpaca.markets" if KEY.startswith("PK")
    else "https://api.alpaca.markets"
)

# ─── API-key security setup ──────────────────────────────────────────────────────
API_KEY_NAME = "X-ACCESS-KEY"
api_key_header = APIKeyHeader(name=API_KEY_NAME, auto_error=False)


def verify_api_key(provided_key: str = Security(api_key_header)):
    if not provided_key:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Missing API key")

    # compute SHA256 hex of what the client gave us
    provided_hash = hashlib.sha256(provided_key.encode()).hexdigest()

    # constant-time compare
    if hmac.compare_digest(provided_hash, ACCESS_KEY_HASH):
        return provided_key

    raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Invalid API key")


# ─── Rate limiting ───────────────────────────────────────────────────────────────
limiter = Limiter(key_func=get_remote_address)

# ─── App instantiation ───────────────────────────────────────────────────────────
app = FastAPI(
    title="Data Acquisition API for TradingSandbox",
    description="API for fetching stock data from Alpaca",
    version="1.0.0",
)

app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

# ─── Health check (no auth required) ─────────────────────────────────────────────
@app.get("/health", include_in_schema=False)
def health_check():
    """Health check endpoint for service orchestration (no auth required)"""
    return {"status": "ok"}

# ─── Protected router (API key required) ─────────────────────────────────────────
api_router = APIRouter(dependencies=[Depends(verify_api_key)])

# Initialize Alpaca client for historical bars
client = StockHistoricalDataClient(api_key=KEY, secret_key=SECRET)

# Create a requests session with retry logic for transient SSL errors
def create_retry_session(retries=3, backoff_factor=0.5):
    session = requests.Session()
    retry = Retry(
        total=retries,
        read=retries,
        connect=retries,
        backoff_factor=backoff_factor,
        status_forcelist=(500, 502, 503, 504),
        allowed_methods=["GET"],
    )
    adapter = HTTPAdapter(max_retries=retry)
    session.mount("https://", adapter)
    session.mount("http://", adapter)
    return session

http_session = create_retry_session()


# ─── Models ────────────────────────────────────────────────────────────────────
class SimplifiedTrade(BaseModel):
    price: float = Field(..., alias="p")
    timestamp: datetime = Field(..., alias="t")
    volume: int = Field(..., alias="s")

    model_config = ConfigDict(
        populate_by_name=True,
        # alias population is on by default in V2
    )


class BarData(BaseModel):
    symbol: str
    start_date: datetime
    end_date: datetime
    timeframe: str = "1Hour"
    bars: dict


class MarketCapResponse(BaseModel):
    symbol: str
    market_cap: float
    currency: str
    timestamp: datetime


class MarketStatus(BaseModel):
    is_open: bool  # true if market is open now
    next_open: datetime  # when the market will next open
    next_close: datetime  # when the market will next close


class StockSuggestion(BaseModel):
    symbol: str
    name: str
    exchange: str


class StockSearchResult(BaseModel):
    suggestions: list[StockSuggestion]


# ─── Endpoints ─────────────────────────────────────────────────────────────────
@api_router.get("/")
@limiter.limit("30/minute")
def root(request: Request):
    return {
        "message": "Welcome to TradingSandbox Data Acquisition, Powered by Alpaca Markets :)"
    }


@api_router.get("/latest-trade/{symbol}", response_model=SimplifiedTrade, response_model_by_alias=False)
@limiter.limit("30/minute")
def get_latest_trade(symbol: str, request: Request):
    """Get the latest trade data for a specific stock symbol"""
    url = f"https://data.alpaca.markets/v2/stocks/{symbol}/trades/latest?feed=iex&currency=USD"
    headers = {
        "accept": "application/json",
        "APCA-API-KEY-ID": KEY,
        "APCA-API-SECRET-KEY": SECRET
    }

    try:
        response = http_session.get(url, headers=headers, timeout=10)
        if response.status_code != 200:
            raise HTTPException(
                status_code=response.status_code,
                detail="Failed to fetch data from Alpaca"
            )

        data = response.json()
        trade_obj = data["trade"]
        return trade_obj
    except requests.exceptions.RequestException as e:
        logger.exception("Error fetching latest trade for %s: %s", symbol, str(e))
        raise HTTPException(
            status_code=503,
            detail="Alpaca API temporarily unavailable"
        )


@api_router.get("/bars/{symbol}", response_model=BarData)
@limiter.limit("20/minute")
def get_stock_bars(
        symbol: str,
        start_date: str,
        end_date: str,
        request: Request,
        timeframe: Optional[str] = "1Day"
):
    """Get historical bar data for a specific stock symbol

    - timeframe options: 1Min, 5Min, 15Min, 1Hour, 1Day, 1Week, 1Month
    - date format: YYYY-MM-DD
    """
    # Map string timeframe to TimeFrame enum
    timeframe_map = {
        "1Min": TimeFrame.Minute,
        "5Min": TimeFrame.Minute,
        "15Min": TimeFrame.Minute,
        "1Hour": TimeFrame.Hour,
        "1Day": TimeFrame.Day,
        "1Week": TimeFrame.Week,
        "1Month": TimeFrame.Month
    }

    # Parse dates
    try:
        start = datetime.fromisoformat(start_date)
        end = datetime.fromisoformat(end_date)
    except ValueError:
        raise HTTPException(
            status_code=400,
            detail="Invalid date format. Use YYYY-MM-DD"
        )

    if timeframe not in timeframe_map:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid timeframe. Options: {', '.join(timeframe_map.keys())}"
        )

    tf = timeframe_map[timeframe]
    multiplier = 1
    if timeframe == "5Min":
        multiplier = 5
    elif timeframe == "15Min":
        multiplier = 15

    bar_request = StockBarsRequest(
        symbol_or_symbols=symbol,
        timeframe=tf,
        multiplier=multiplier if timeframe in ["5Min", "15Min"] else None,
        start=start,
        end=end,
        feed=DataFeed.IEX
    )

    try:
        bars = client.get_stock_bars(bar_request)
        return {
            "symbol": symbol,
            "start_date": start,
            "end_date": end,
            "timeframe": timeframe,
            "bars": bars.data
        }
    except Exception as e:
        logger.exception("Error fetching bars for %s", symbol)
        raise HTTPException(
            status_code=500,
            detail="An internal error occurred while fetching bar data."
        )


@api_router.get("/market-cap/{symbol}", response_model=MarketCapResponse)
@limiter.limit("20/minute")
def get_market_cap(symbol: str, request: Request):
    symbol = symbol.upper()
    ticker = yf.Ticker(symbol)

    # 1. Pull fundamentals/info
    info = ticker.info
    market_cap = info.get("marketCap")
    currency = info.get("currency", "USD")
    price = info.get("regularMarketPrice") or info.get("previousClose")

    if market_cap is None or price is None:
        raise HTTPException(
            status_code=404,
            detail=f"Data not available for {symbol}"
        )

    # 2. Return structured response
    return MarketCapResponse(symbol=symbol, market_cap=market_cap, currency=currency, timestamp=datetime.now(timezone.utc))


@api_router.get("/market-status", response_model=MarketStatus)
@limiter.limit("30/minute")
def get_market_status(request: Request):
    """Return current market open status and upcoming open/close times."""
    url = f"{ALPACA_TRADING_BASE_URL}/v2/clock"
    headers = {
        "accept":              "application/json",
        "APCA-API-KEY-ID":     KEY,
        "APCA-API-SECRET-KEY": SECRET,
    }

    try:
        resp = http_session.get(url, headers=headers, timeout=10)
        if resp.status_code != 200:
            raise HTTPException(status_code=resp.status_code, detail="Failed to fetch market clock from Alpaca")
        return resp.json()
    except requests.exceptions.RequestException as e:
        logger.exception("Error fetching market status: %s", str(e))
        raise HTTPException(
            status_code=503,
            detail="Alpaca API temporarily unavailable"
        )


@api_router.get("/search/{query}", response_model=StockSearchResult)
@limiter.limit("30/minute")
def search_stocks(query: str, request: Request, limit: int = 10):
    """Search for stocks by symbol or name.

    Returns matching tradable US stocks from Alpaca's asset database.
    """
    if not query or len(query) < 1:
        return {"suggestions": []}

    query = query.upper().strip()

    url = f"{ALPACA_TRADING_BASE_URL}/v2/assets"
    headers = {
        "accept": "application/json",
        "APCA-API-KEY-ID": KEY,
        "APCA-API-SECRET-KEY": SECRET,
    }
    params = {
        "status": "active",
        "asset_class": "us_equity",
    }

    try:
        resp = http_session.get(url, headers=headers, params=params, timeout=10)
        if resp.status_code != 200:
            raise HTTPException(
                status_code=resp.status_code,
                detail="Failed to fetch assets from Alpaca"
            )

        assets = resp.json()

        # Filter assets that match the query (symbol starts with or name contains)
        suggestions = []
        for asset in assets:
            if not asset.get("tradable", False):
                continue

            symbol = asset.get("symbol", "")
            name = asset.get("name", "")
            exchange = asset.get("exchange", "")

            # Match: symbol starts with query OR name contains query
            if symbol.startswith(query) or query in name.upper():
                suggestions.append({
                    "symbol": symbol,
                    "name": name,
                    "exchange": exchange,
                })

            if len(suggestions) >= limit:
                break

        # Sort by best match (exact symbol match first, then by symbol length)
        suggestions.sort(key=lambda x: (
            0 if x["symbol"] == query else 1,  # Exact match first
            0 if x["symbol"].startswith(query) else 1,  # Prefix match second
            len(x["symbol"])  # Shorter symbols first
        ))

        return {"suggestions": suggestions[:limit]}

    except requests.exceptions.RequestException as e:
        logger.exception("Error searching stocks: %s", str(e))
        raise HTTPException(
            status_code=503,
            detail="Alpaca API temporarily unavailable"
        )


@api_router.get("/validate/{symbol}")
@limiter.limit("30/minute")
def validate_symbol(symbol: str, request: Request):
    """Validate if a stock symbol exists and is tradable.

    Returns symbol info if valid, 404 if not found.
    """
    symbol = symbol.upper().strip()

    url = f"{ALPACA_TRADING_BASE_URL}/v2/assets/{symbol}"
    headers = {
        "accept": "application/json",
        "APCA-API-KEY-ID": KEY,
        "APCA-API-SECRET-KEY": SECRET,
    }

    try:
        resp = http_session.get(url, headers=headers, timeout=10)

        if resp.status_code == 404:
            raise HTTPException(
                status_code=404,
                detail=f"Stock symbol '{symbol}' not found. Please check the ticker and try again."
            )

        if resp.status_code != 200:
            raise HTTPException(
                status_code=resp.status_code,
                detail="Failed to validate symbol"
            )

        asset = resp.json()

        if not asset.get("tradable", False):
            raise HTTPException(
                status_code=404,
                detail=f"Stock '{symbol}' exists but is not currently tradable."
            )

        return {
            "valid": True,
            "symbol": asset.get("symbol"),
            "name": asset.get("name"),
            "exchange": asset.get("exchange"),
            "tradable": asset.get("tradable"),
        }

    except requests.exceptions.RequestException as e:
        logger.exception("Error validating symbol %s: %s", symbol, str(e))
        raise HTTPException(
            status_code=503,
            detail="Alpaca API temporarily unavailable"
        )


# ─── Include protected router ────────────────────────────────────────────────────
app.include_router(api_router)
