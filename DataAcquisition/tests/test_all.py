"""
Comprehensive tests for the DataAcquisition FastAPI service.

Covers:
  - Health check endpoint (no auth)
  - Root endpoint (with auth)
  - Latest trade endpoint (success, API failure, request exception)
  - API key authentication (invalid key, missing key)
  - Bars endpoint (success with all timeframes, invalid date, invalid timeframe, internal error)
  - Market cap endpoint (success, data not available)
  - Market status endpoint (success, API failure, request exception)
  - Pydantic model validation for SimplifiedTrade, BarData, MarketCapResponse, MarketStatus
"""

import os
from datetime import datetime, timezone

import pytest
import requests
from dotenv import load_dotenv
from fastapi.testclient import TestClient
from pydantic import ValidationError

import src.api as api_module
from src.api import (
    BarData,
    MarketCapResponse,
    MarketStatus,
    SimplifiedTrade,
    app,
)

load_dotenv()
ACCESS_KEY = os.getenv("ACCESS_KEY")

client = TestClient(app)

AUTH_HEADER = {"X-ACCESS-KEY": ACCESS_KEY}


# ---------------------------------------------------------------------------
# Helper stubs
# ---------------------------------------------------------------------------

class DummyResponse:
    """Minimal stand-in for ``requests.Response``."""

    def __init__(self, json_data, status_code=200):
        self._json = json_data
        self.status_code = status_code

    def json(self):
        return self._json


class DummyBarsResult:
    """Stand-in for the object returned by ``client.get_stock_bars``."""

    def __init__(self, data):
        self.data = data


class DummyTicker:
    """Stand-in for ``yfinance.Ticker``."""

    def __init__(self, info_dict):
        self.info = info_dict


# Reusable sample payloads ------------------------------------------------

SAMPLE_TRADE_PAYLOAD = {
    "symbol": "AMZN",
    "trade": {
        "c": ["@"],
        "i": 12345,
        "p": 150.25,
        "s": 200,
        "t": "2025-07-08T12:00:00Z",
        "x": "V",
        "z": "C",
    },
}

SAMPLE_MARKET_STATUS_PAYLOAD = {
    "is_open": True,
    "next_open": "2025-07-10T09:30:00-04:00",
    "next_close": "2025-07-09T16:00:00-04:00",
}

SAMPLE_BARS_DATA = {
    "AAPL": [
        {
            "t": "2025-01-02T05:00:00Z",
            "o": 180.0,
            "h": 182.0,
            "l": 179.0,
            "c": 181.5,
            "v": 1000000,
            "n": 5000,
            "vw": 180.75,
        }
    ]
}


# ===========================================================================
# 1. Health check endpoint (no auth required)
# ===========================================================================

class TestHealthCheck:
    def test_health_returns_200(self):
        resp = client.get("/health")
        assert resp.status_code == 200

    def test_health_returns_ok_status(self):
        resp = client.get("/health")
        assert resp.json() == {"status": "ok"}

    def test_health_no_auth_needed(self):
        """Health endpoint must work even without the X-ACCESS-KEY header."""
        resp = client.get("/health")
        assert resp.status_code == 200
        assert resp.json()["status"] == "ok"


# ===========================================================================
# 2. Root endpoint (with auth)
# ===========================================================================

class TestRootEndpoint:
    def test_root_success(self):
        resp = client.get("/", headers=AUTH_HEADER)
        assert resp.status_code == 200
        body = resp.json()
        assert "message" in body
        assert "TradingSandbox" in body["message"]

    def test_root_requires_auth(self):
        resp = client.get("/")
        assert resp.status_code == 403


# ===========================================================================
# 3. Latest trade endpoint
# ===========================================================================

class TestLatestTrade:
    def test_success(self, monkeypatch):
        monkeypatch.setattr(
            api_module.http_session, "get",
            lambda url, headers, timeout=None: DummyResponse(SAMPLE_TRADE_PAYLOAD, 200),
        )
        resp = client.get("/latest-trade/AMZN", headers=AUTH_HEADER)
        assert resp.status_code == 200

        data = resp.json()
        trade = SimplifiedTrade(**data)
        assert trade.price == 150.25
        assert trade.volume == 200
        assert trade.timestamp.year == 2025

    def test_response_uses_field_names_not_aliases(self, monkeypatch):
        """The endpoint sets response_model_by_alias=False, so keys should be
        ``price``, ``timestamp``, ``volume`` -- not ``p``, ``t``, ``s``."""
        monkeypatch.setattr(
            api_module.http_session, "get",
            lambda url, headers, timeout=None: DummyResponse(SAMPLE_TRADE_PAYLOAD, 200),
        )
        resp = client.get("/latest-trade/AAPL", headers=AUTH_HEADER)
        data = resp.json()
        assert "price" in data
        assert "timestamp" in data
        assert "volume" in data
        assert "p" not in data
        assert "t" not in data
        assert "s" not in data

    def test_api_failure_non_200(self, monkeypatch):
        """When Alpaca returns a non-200 status, it should be forwarded."""
        monkeypatch.setattr(
            api_module.http_session, "get",
            lambda url, headers, timeout=None: DummyResponse({"detail": "Unauthorized"}, 401),
        )
        resp = client.get("/latest-trade/AMZN", headers=AUTH_HEADER)
        assert resp.status_code == 401
        assert resp.json()["detail"] == "Failed to fetch data from Alpaca"

    def test_api_failure_500(self, monkeypatch):
        monkeypatch.setattr(
            api_module.http_session, "get",
            lambda url, headers, timeout=None: DummyResponse({"message": "Server Error"}, 500),
        )
        resp = client.get("/latest-trade/TSLA", headers=AUTH_HEADER)
        assert resp.status_code == 500
        assert resp.json()["detail"] == "Failed to fetch data from Alpaca"

    def test_request_exception_returns_503(self, monkeypatch):
        """A network-level failure should result in 503."""
        def exploding_get(url, headers, timeout=None):
            raise requests.exceptions.ConnectionError("DNS resolution failed")

        monkeypatch.setattr(api_module.http_session, "get", exploding_get)
        resp = client.get("/latest-trade/AMZN", headers=AUTH_HEADER)
        assert resp.status_code == 503
        assert resp.json()["detail"] == "Alpaca API temporarily unavailable"

    def test_request_timeout_returns_503(self, monkeypatch):
        def timeout_get(url, headers, timeout=None):
            raise requests.exceptions.Timeout("Request timed out")

        monkeypatch.setattr(api_module.http_session, "get", timeout_get)
        resp = client.get("/latest-trade/GOOG", headers=AUTH_HEADER)
        assert resp.status_code == 503
        assert resp.json()["detail"] == "Alpaca API temporarily unavailable"

    def test_different_symbols(self, monkeypatch):
        """Ensure path parameter for different symbols is accepted."""
        for symbol in ("AAPL", "MSFT", "TSLA", "GOOG"):
            monkeypatch.setattr(
                api_module.http_session, "get",
                lambda url, headers, timeout=None: DummyResponse(SAMPLE_TRADE_PAYLOAD, 200),
            )
            resp = client.get(f"/latest-trade/{symbol}", headers=AUTH_HEADER)
            assert resp.status_code == 200


# ===========================================================================
# 4. API key authentication
# ===========================================================================

class TestAPIKeyAuth:
    def test_invalid_api_key(self):
        resp = client.get("/latest-trade/AMZN", headers={"X-ACCESS-KEY": "totally-wrong-key"})
        assert resp.status_code == 403
        assert resp.json()["detail"] == "Invalid API key"

    def test_missing_api_key(self):
        resp = client.get("/latest-trade/AMZN")
        assert resp.status_code == 403
        assert resp.json()["detail"] == "Missing API key"

    def test_empty_api_key(self):
        resp = client.get("/latest-trade/AMZN", headers={"X-ACCESS-KEY": ""})
        assert resp.status_code == 403

    def test_invalid_key_on_root(self):
        resp = client.get("/", headers={"X-ACCESS-KEY": "bad"})
        assert resp.status_code == 403

    def test_missing_key_on_bars(self):
        resp = client.get("/bars/AAPL?start_date=2025-01-01&end_date=2025-01-31")
        assert resp.status_code == 403

    def test_missing_key_on_market_cap(self):
        resp = client.get("/market-cap/AAPL")
        assert resp.status_code == 403

    def test_missing_key_on_market_status(self):
        resp = client.get("/market-status")
        assert resp.status_code == 403

    def test_valid_key_accepted(self, monkeypatch):
        """Prove that the real ACCESS_KEY passes auth."""
        monkeypatch.setattr(
            api_module.http_session, "get",
            lambda url, headers, timeout=None: DummyResponse(SAMPLE_TRADE_PAYLOAD, 200),
        )
        resp = client.get("/latest-trade/AMZN", headers=AUTH_HEADER)
        assert resp.status_code == 200


# ===========================================================================
# 5. Bars endpoint -- success with different timeframes
# ===========================================================================

class TestBarsEndpoint:
    """Tests for GET /bars/{symbol}."""

    @pytest.fixture(autouse=True)
    def _mock_alpaca_client(self, monkeypatch):
        """Patch the Alpaca StockHistoricalDataClient.get_stock_bars for
        every test in this class."""
        monkeypatch.setattr(
            api_module.client, "get_stock_bars",
            lambda req: DummyBarsResult(SAMPLE_BARS_DATA),
        )

    # --- success paths -------------------------------------------------------

    def test_bars_1day(self):
        resp = client.get(
            "/bars/AAPL?start_date=2025-01-01&end_date=2025-01-31&timeframe=1Day",
            headers=AUTH_HEADER,
        )
        assert resp.status_code == 200
        body = resp.json()
        assert body["symbol"] == "AAPL"
        assert body["timeframe"] == "1Day"
        assert "AAPL" in body["bars"]

    def test_bars_1hour(self):
        resp = client.get(
            "/bars/MSFT?start_date=2025-01-01&end_date=2025-01-31&timeframe=1Hour",
            headers=AUTH_HEADER,
        )
        assert resp.status_code == 200
        assert resp.json()["timeframe"] == "1Hour"

    def test_bars_1min(self):
        resp = client.get(
            "/bars/TSLA?start_date=2025-06-01&end_date=2025-06-02&timeframe=1Min",
            headers=AUTH_HEADER,
        )
        assert resp.status_code == 200
        assert resp.json()["timeframe"] == "1Min"

    def test_bars_5min(self):
        resp = client.get(
            "/bars/GOOG?start_date=2025-06-01&end_date=2025-06-02&timeframe=5Min",
            headers=AUTH_HEADER,
        )
        assert resp.status_code == 200
        assert resp.json()["timeframe"] == "5Min"

    def test_bars_15min(self):
        resp = client.get(
            "/bars/NVDA?start_date=2025-06-01&end_date=2025-06-02&timeframe=15Min",
            headers=AUTH_HEADER,
        )
        assert resp.status_code == 200
        assert resp.json()["timeframe"] == "15Min"

    def test_bars_1week(self):
        resp = client.get(
            "/bars/META?start_date=2025-01-01&end_date=2025-06-01&timeframe=1Week",
            headers=AUTH_HEADER,
        )
        assert resp.status_code == 200
        assert resp.json()["timeframe"] == "1Week"

    def test_bars_1month(self):
        resp = client.get(
            "/bars/JPM?start_date=2024-01-01&end_date=2025-01-01&timeframe=1Month",
            headers=AUTH_HEADER,
        )
        assert resp.status_code == 200
        assert resp.json()["timeframe"] == "1Month"

    def test_bars_default_timeframe(self):
        """When no timeframe query param is given, it defaults to 1Day."""
        resp = client.get(
            "/bars/AAPL?start_date=2025-01-01&end_date=2025-01-31",
            headers=AUTH_HEADER,
        )
        assert resp.status_code == 200
        assert resp.json()["timeframe"] == "1Day"

    def test_bars_response_structure(self):
        resp = client.get(
            "/bars/AAPL?start_date=2025-01-01&end_date=2025-01-31&timeframe=1Day",
            headers=AUTH_HEADER,
        )
        body = resp.json()
        assert "symbol" in body
        assert "start_date" in body
        assert "end_date" in body
        assert "timeframe" in body
        assert "bars" in body
        assert isinstance(body["bars"], dict)

    # --- failure paths -------------------------------------------------------

    def test_bars_invalid_date_format(self):
        resp = client.get(
            "/bars/AAPL?start_date=not-a-date&end_date=2025-01-31&timeframe=1Day",
            headers=AUTH_HEADER,
        )
        assert resp.status_code == 400
        assert "Invalid date format" in resp.json()["detail"]

    def test_bars_invalid_end_date(self):
        resp = client.get(
            "/bars/AAPL?start_date=2025-01-01&end_date=31-01-2025&timeframe=1Day",
            headers=AUTH_HEADER,
        )
        assert resp.status_code == 400
        assert "Invalid date format" in resp.json()["detail"]

    def test_bars_both_dates_invalid(self):
        resp = client.get(
            "/bars/AAPL?start_date=foo&end_date=bar&timeframe=1Day",
            headers=AUTH_HEADER,
        )
        assert resp.status_code == 400

    def test_bars_invalid_timeframe(self):
        resp = client.get(
            "/bars/AAPL?start_date=2025-01-01&end_date=2025-01-31&timeframe=2Hour",
            headers=AUTH_HEADER,
        )
        assert resp.status_code == 400
        assert "Invalid timeframe" in resp.json()["detail"]
        # The error message should list the valid options
        for tf in ("1Min", "5Min", "15Min", "1Hour", "1Day", "1Week", "1Month"):
            assert tf in resp.json()["detail"]

    def test_bars_internal_error(self, monkeypatch):
        """When the Alpaca client raises an exception, we get 500."""
        monkeypatch.setattr(
            api_module.client, "get_stock_bars",
            lambda req: (_ for _ in ()).throw(Exception("Alpaca SDK exploded")),
        )
        resp = client.get(
            "/bars/AAPL?start_date=2025-01-01&end_date=2025-01-31&timeframe=1Day",
            headers=AUTH_HEADER,
        )
        assert resp.status_code == 500
        assert resp.json()["detail"] == "An internal error occurred while fetching bar data."


# ===========================================================================
# 6. Market cap endpoint
# ===========================================================================

class TestMarketCap:
    def test_success(self, monkeypatch):
        info = {
            "marketCap": 3_000_000_000_000,
            "currency": "USD",
            "regularMarketPrice": 195.50,
        }
        monkeypatch.setattr(
            api_module.yf, "Ticker",
            lambda symbol: DummyTicker(info),
        )
        resp = client.get("/market-cap/AAPL", headers=AUTH_HEADER)
        assert resp.status_code == 200
        body = resp.json()
        assert body["symbol"] == "AAPL"
        assert body["market_cap"] == 3_000_000_000_000
        assert body["currency"] == "USD"
        assert "timestamp" in body

    def test_success_with_lowercase_symbol(self, monkeypatch):
        """The endpoint upper-cases the symbol, so ``aapl`` should work."""
        info = {
            "marketCap": 2_500_000_000_000,
            "currency": "USD",
            "regularMarketPrice": 190.0,
        }
        monkeypatch.setattr(
            api_module.yf, "Ticker",
            lambda symbol: DummyTicker(info),
        )
        resp = client.get("/market-cap/aapl", headers=AUTH_HEADER)
        assert resp.status_code == 200
        assert resp.json()["symbol"] == "AAPL"

    def test_success_with_previous_close_fallback(self, monkeypatch):
        """If regularMarketPrice is missing, previousClose should be used."""
        info = {
            "marketCap": 1_000_000_000,
            "currency": "EUR",
            "previousClose": 42.0,
        }
        monkeypatch.setattr(
            api_module.yf, "Ticker",
            lambda symbol: DummyTicker(info),
        )
        resp = client.get("/market-cap/SAP", headers=AUTH_HEADER)
        assert resp.status_code == 200
        assert resp.json()["currency"] == "EUR"

    def test_currency_defaults_to_usd(self, monkeypatch):
        """When yfinance does not return a currency field, it should default to USD."""
        info = {
            "marketCap": 500_000_000,
            "regularMarketPrice": 25.0,
        }
        monkeypatch.setattr(
            api_module.yf, "Ticker",
            lambda symbol: DummyTicker(info),
        )
        resp = client.get("/market-cap/XYZ", headers=AUTH_HEADER)
        assert resp.status_code == 200
        assert resp.json()["currency"] == "USD"

    def test_data_not_available_no_market_cap(self, monkeypatch):
        """404 when marketCap is None."""
        info = {"currency": "USD", "regularMarketPrice": 100.0}
        monkeypatch.setattr(
            api_module.yf, "Ticker",
            lambda symbol: DummyTicker(info),
        )
        resp = client.get("/market-cap/FAKE", headers=AUTH_HEADER)
        assert resp.status_code == 404
        assert "Data not available" in resp.json()["detail"]

    def test_data_not_available_no_price(self, monkeypatch):
        """404 when neither regularMarketPrice nor previousClose exist."""
        info = {"marketCap": 1_000_000}
        monkeypatch.setattr(
            api_module.yf, "Ticker",
            lambda symbol: DummyTicker(info),
        )
        resp = client.get("/market-cap/FAKE", headers=AUTH_HEADER)
        assert resp.status_code == 404
        assert "Data not available" in resp.json()["detail"]

    def test_data_not_available_empty_info(self, monkeypatch):
        """404 when info dict is empty."""
        monkeypatch.setattr(
            api_module.yf, "Ticker",
            lambda symbol: DummyTicker({}),
        )
        resp = client.get("/market-cap/NOPE", headers=AUTH_HEADER)
        assert resp.status_code == 404


# ===========================================================================
# 7. Market status endpoint
# ===========================================================================

class TestMarketStatus:
    def test_success(self, monkeypatch):
        monkeypatch.setattr(
            api_module.http_session, "get",
            lambda url, headers, timeout=None: DummyResponse(SAMPLE_MARKET_STATUS_PAYLOAD, 200),
        )
        resp = client.get("/market-status", headers=AUTH_HEADER)
        assert resp.status_code == 200
        body = resp.json()
        assert body["is_open"] is True
        assert "next_open" in body
        assert "next_close" in body

    def test_market_closed(self, monkeypatch):
        payload = {
            "is_open": False,
            "next_open": "2025-07-14T09:30:00-04:00",
            "next_close": "2025-07-14T16:00:00-04:00",
        }
        monkeypatch.setattr(
            api_module.http_session, "get",
            lambda url, headers, timeout=None: DummyResponse(payload, 200),
        )
        resp = client.get("/market-status", headers=AUTH_HEADER)
        assert resp.status_code == 200
        assert resp.json()["is_open"] is False

    def test_api_failure_non_200(self, monkeypatch):
        monkeypatch.setattr(
            api_module.http_session, "get",
            lambda url, headers, timeout=None: DummyResponse({"message": "Bad Request"}, 400),
        )
        resp = client.get("/market-status", headers=AUTH_HEADER)
        assert resp.status_code == 400
        assert resp.json()["detail"] == "Failed to fetch market clock from Alpaca"

    def test_api_failure_401(self, monkeypatch):
        monkeypatch.setattr(
            api_module.http_session, "get",
            lambda url, headers, timeout=None: DummyResponse({"message": "Unauthorized"}, 401),
        )
        resp = client.get("/market-status", headers=AUTH_HEADER)
        assert resp.status_code == 401
        assert resp.json()["detail"] == "Failed to fetch market clock from Alpaca"

    def test_request_exception_returns_503(self, monkeypatch):
        def exploding_get(url, headers, timeout=None):
            raise requests.exceptions.ConnectionError("Connection refused")

        monkeypatch.setattr(api_module.http_session, "get", exploding_get)
        resp = client.get("/market-status", headers=AUTH_HEADER)
        assert resp.status_code == 503
        assert resp.json()["detail"] == "Alpaca API temporarily unavailable"

    def test_timeout_returns_503(self, monkeypatch):
        def timeout_get(url, headers, timeout=None):
            raise requests.exceptions.Timeout("Read timed out")

        monkeypatch.setattr(api_module.http_session, "get", timeout_get)
        resp = client.get("/market-status", headers=AUTH_HEADER)
        assert resp.status_code == 503
        assert resp.json()["detail"] == "Alpaca API temporarily unavailable"


# ===========================================================================
# 8. Pydantic model validation -- SimplifiedTrade
# ===========================================================================

class TestSimplifiedTradeModel:
    def test_create_with_aliases(self):
        trade = SimplifiedTrade(p=123.45, t="2025-06-15T10:30:00Z", s=500)
        assert trade.price == 123.45
        assert trade.volume == 500

    def test_create_with_field_names(self):
        trade = SimplifiedTrade(
            price=99.99,
            timestamp="2025-01-01T00:00:00Z",
            volume=42,
        )
        assert trade.price == 99.99
        assert trade.volume == 42

    def test_timestamp_parsed_correctly(self):
        trade = SimplifiedTrade(p=10.0, t="2025-03-15T14:30:00+00:00", s=1)
        assert trade.timestamp.year == 2025
        assert trade.timestamp.month == 3
        assert trade.timestamp.day == 15

    def test_missing_required_field_raises(self):
        with pytest.raises(ValidationError):
            SimplifiedTrade(p=100.0, t="2025-01-01T00:00:00Z")  # missing s / volume

    def test_missing_price_raises(self):
        with pytest.raises(ValidationError):
            SimplifiedTrade(t="2025-01-01T00:00:00Z", s=10)

    def test_missing_timestamp_raises(self):
        with pytest.raises(ValidationError):
            SimplifiedTrade(p=50.0, s=10)

    def test_invalid_timestamp_raises(self):
        with pytest.raises(ValidationError):
            SimplifiedTrade(p=10.0, t="not-a-datetime", s=1)

    def test_volume_must_be_int(self):
        """Pydantic will coerce compatible types, but a non-numeric string should fail."""
        with pytest.raises(ValidationError):
            SimplifiedTrade(p=10.0, t="2025-01-01T00:00:00Z", s="abc")

    def test_serialization_aliases(self):
        trade = SimplifiedTrade(p=100.0, t="2025-01-01T00:00:00Z", s=50)
        dumped = trade.model_dump(by_alias=True)
        assert "p" in dumped
        assert "t" in dumped
        assert "s" in dumped

    def test_serialization_field_names(self):
        trade = SimplifiedTrade(p=100.0, t="2025-01-01T00:00:00Z", s=50)
        dumped = trade.model_dump(by_alias=False)
        assert "price" in dumped
        assert "timestamp" in dumped
        assert "volume" in dumped


# ===========================================================================
# 9. Pydantic model validation -- BarData
# ===========================================================================

class TestBarDataModel:
    def test_create_bar_data(self):
        bd = BarData(
            symbol="AAPL",
            start_date="2025-01-01T00:00:00",
            end_date="2025-01-31T00:00:00",
            timeframe="1Day",
            bars={"AAPL": []},
        )
        assert bd.symbol == "AAPL"
        assert bd.timeframe == "1Day"
        assert isinstance(bd.bars, dict)

    def test_default_timeframe(self):
        bd = BarData(
            symbol="MSFT",
            start_date="2025-01-01T00:00:00",
            end_date="2025-01-31T00:00:00",
            bars={},
        )
        assert bd.timeframe == "1Hour"

    def test_missing_symbol_raises(self):
        with pytest.raises(ValidationError):
            BarData(
                start_date="2025-01-01T00:00:00",
                end_date="2025-01-31T00:00:00",
                bars={},
            )

    def test_missing_bars_raises(self):
        with pytest.raises(ValidationError):
            BarData(
                symbol="AAPL",
                start_date="2025-01-01T00:00:00",
                end_date="2025-01-31T00:00:00",
            )

    def test_missing_dates_raises(self):
        with pytest.raises(ValidationError):
            BarData(symbol="AAPL", bars={})

    def test_bars_accepts_nested_data(self):
        bd = BarData(
            symbol="AAPL",
            start_date="2025-01-01T00:00:00",
            end_date="2025-01-31T00:00:00",
            bars=SAMPLE_BARS_DATA,
        )
        assert "AAPL" in bd.bars
        assert len(bd.bars["AAPL"]) == 1

    def test_dates_parsed_correctly(self):
        bd = BarData(
            symbol="X",
            start_date="2025-06-15T09:30:00",
            end_date="2025-06-15T16:00:00",
            bars={},
        )
        assert bd.start_date.month == 6
        assert bd.end_date.hour == 16


# ===========================================================================
# 10. Pydantic model validation -- MarketCapResponse
# ===========================================================================

class TestMarketCapResponseModel:
    def test_create_market_cap_response(self):
        mcr = MarketCapResponse(
            symbol="AAPL",
            market_cap=3_000_000_000_000,
            currency="USD",
            timestamp="2025-07-01T12:00:00Z",
        )
        assert mcr.symbol == "AAPL"
        assert mcr.market_cap == 3_000_000_000_000
        assert mcr.currency == "USD"

    def test_missing_symbol_raises(self):
        with pytest.raises(ValidationError):
            MarketCapResponse(
                market_cap=1_000_000,
                currency="USD",
                timestamp="2025-01-01T00:00:00Z",
            )

    def test_missing_market_cap_raises(self):
        with pytest.raises(ValidationError):
            MarketCapResponse(
                symbol="AAPL",
                currency="USD",
                timestamp="2025-01-01T00:00:00Z",
            )

    def test_missing_currency_raises(self):
        with pytest.raises(ValidationError):
            MarketCapResponse(
                symbol="AAPL",
                market_cap=1_000_000,
                timestamp="2025-01-01T00:00:00Z",
            )

    def test_missing_timestamp_raises(self):
        with pytest.raises(ValidationError):
            MarketCapResponse(
                symbol="AAPL",
                market_cap=1_000_000,
                currency="USD",
            )

    def test_market_cap_float(self):
        mcr = MarketCapResponse(
            symbol="TSLA",
            market_cap=800_500_000_000.50,
            currency="USD",
            timestamp=datetime.now(timezone.utc),
        )
        assert isinstance(mcr.market_cap, float)

    def test_timestamp_accepts_datetime_object(self):
        now = datetime.now(timezone.utc)
        mcr = MarketCapResponse(
            symbol="X",
            market_cap=100.0,
            currency="USD",
            timestamp=now,
        )
        assert mcr.timestamp == now


# ===========================================================================
# 11. Pydantic model validation -- MarketStatus
# ===========================================================================

class TestMarketStatusModel:
    def test_create_market_status(self):
        ms = MarketStatus(
            is_open=True,
            next_open="2025-07-10T09:30:00-04:00",
            next_close="2025-07-09T16:00:00-04:00",
        )
        assert ms.is_open is True

    def test_market_closed(self):
        ms = MarketStatus(
            is_open=False,
            next_open="2025-07-14T09:30:00-04:00",
            next_close="2025-07-14T16:00:00-04:00",
        )
        assert ms.is_open is False

    def test_missing_is_open_raises(self):
        with pytest.raises(ValidationError):
            MarketStatus(
                next_open="2025-07-10T09:30:00-04:00",
                next_close="2025-07-10T16:00:00-04:00",
            )

    def test_missing_next_open_raises(self):
        with pytest.raises(ValidationError):
            MarketStatus(
                is_open=True,
                next_close="2025-07-10T16:00:00-04:00",
            )

    def test_missing_next_close_raises(self):
        with pytest.raises(ValidationError):
            MarketStatus(
                is_open=True,
                next_open="2025-07-10T09:30:00-04:00",
            )

    def test_invalid_datetime_raises(self):
        with pytest.raises(ValidationError):
            MarketStatus(
                is_open=True,
                next_open="not-a-date",
                next_close="also-not-a-date",
            )

    def test_datetimes_parsed(self):
        ms = MarketStatus(
            is_open=True,
            next_open="2025-07-10T09:30:00-04:00",
            next_close="2025-07-10T16:00:00-04:00",
        )
        assert ms.next_open.hour == 9 or ms.next_open.hour == 13  # depends on tz handling
        assert isinstance(ms.next_close, datetime)

    def test_serialization(self):
        ms = MarketStatus(
            is_open=False,
            next_open="2025-07-14T09:30:00-04:00",
            next_close="2025-07-14T16:00:00-04:00",
        )
        dumped = ms.model_dump()
        assert "is_open" in dumped
        assert "next_open" in dumped
        assert "next_close" in dumped
        assert dumped["is_open"] is False
