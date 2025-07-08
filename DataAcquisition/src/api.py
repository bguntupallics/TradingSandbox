from fastapi import FastAPI, HTTPException, Depends, Security, status
from fastapi.security.api_key import APIKeyHeader
from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field
import requests
import yfinance as yf
from alpaca.data.historical import StockHistoricalDataClient
from alpaca.data.requests import StockBarsRequest
from alpaca.data.timeframe import TimeFrame
import os
from dotenv import load_dotenv
import hashlib
import hmac

# ─── Environment ────────────────────────────────────────────────────────────
load_dotenv()  # <-- this reads .env into os.environ

KEY = os.getenv("ALPACA_API_KEY")
SECRET = os.getenv("ALPACA_API_SECRET")
ACCESS_KEY_HASH = os.getenv("ACCESS_KEY_HASH")

if not KEY or not SECRET or not ACCESS_KEY_HASH:
    raise RuntimeError("ALPACA_API_KEY, ALPACA_API_SECRET and ACCESS_KEY_HASH must be set in .env")

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


# ─── App instantiation ───────────────────────────────────────────────────────────
app = FastAPI(
    title="Data Acquisition API for TradingSandbox",
    description="API for fetching stock data from Alpaca",
    version="1.0.0",
    dependencies=[Depends(verify_api_key)]    # <-- enforce API-key on every route
)

# Initialize Alpaca client for historical bars
client = StockHistoricalDataClient(api_key=KEY, secret_key=SECRET)


# ─── Models ────────────────────────────────────────────────────────────────────
class SimplifiedTrade(BaseModel):
    price: float = Field(..., alias="p")
    timestamp: datetime = Field(..., alias="t")
    volume: int = Field(..., alias="s")

    class Config:
        # allow parsing from the short keys "p"/"t"/"s"
        populate_by_name = True


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


# ─── Endpoints ─────────────────────────────────────────────────────────────────
@app.get("/")
def root():
    return {
        "message": "Welcome to TradingSandbox Data Acquisition, Powered by Alpaca Markets :)"
    }


@app.get("/latest-trade/{symbol}", response_model=SimplifiedTrade, response_model_by_alias=False)
def get_latest_trade(symbol: str):
    """Get the latest trade data for a specific stock symbol"""
    url = f"https://data.alpaca.markets/v2/stocks/{symbol}/trades/latest?feed=iex&currency=USD"
    headers = {
        "accept": "application/json",
        "APCA-API-KEY-ID": KEY,
        "APCA-API-SECRET-KEY": SECRET
    }

    response = requests.get(url, headers=headers)
    if response.status_code != 200:
        raise HTTPException(
            status_code=response.status_code,
            detail="Failed to fetch data from Alpaca"
        )

    data = response.json()
    # extract the inner trade object
    trade_obj = data["trade"]

    # either return the dict directly:
    return trade_obj


@app.get("/bars/{symbol}", response_model=BarData)
def get_stock_bars(
        symbol: str,
        start_date: str,
        end_date: str,
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

    request = StockBarsRequest(
        symbol_or_symbols=symbol,
        timeframe=tf,
        multiplier=multiplier if timeframe in ["5Min", "15Min"] else None,
        start=start,
        end=end
    )

    try:
        bars = client.get_stock_bars(request)
        return {
            "symbol": symbol,
            "start_date": start,
            "end_date": end,
            "timeframe": timeframe,
            "bars": bars.data
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/market-cap/{symbol}", response_model=MarketCapResponse)
def get_market_cap(symbol: str):
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
    return MarketCapResponse(symbol=symbol, market_cap=market_cap, currency=currency, timestamp=datetime.utcnow())
