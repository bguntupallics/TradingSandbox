import requests
import os
from dotenv import load_dotenv
from fastapi.testclient import TestClient

from src.api import app, SimplifiedTrade

load_dotenv()  # <-- this reads .env into os.environ
ACCESS_KEY = os.getenv("ACCESS_KEY")

client = TestClient(app)


# A helper stub for mocking requests.get
class DummyResponse:
    def __init__(self, json_data, status_code=200):
        self._json = json_data
        self.status_code = status_code

    def json(self):
        return self._json


# 1) Test successful path
def test_latest_trade_success(monkeypatch):
    sample_api_payload = {
        "symbol": "AMZN",
        "trade": {
            "c": ["@"],
            "i": 12345,
            "p": 150.25,
            "s": 200,
            "t": "2025-07-08T12:00:00Z",
            "x": "V",
            "z": "C"
        }
    }

    # Monkey-patch requests.get inside our endpoint to return DummyResponse
    def fake_get(url, headers):
        return DummyResponse(sample_api_payload, 200)

    monkeypatch.setattr(requests, "get", fake_get)

    resp = client.get("/latest-trade/AMZN", headers={"X-ACCESS-KEY": ACCESS_KEY})
    assert resp.status_code == 200

    data = resp.json()
    # It should match our Pydantic model
    trade = SimplifiedTrade(**data)
    assert trade.price == 150.25
    assert trade.timestamp.isoformat() == "2025-07-08T12:00:00+00:00"
    assert trade.volume == 200


def test_latest_trade_failure(monkeypatch):
    # Patch requests.get to return a 401 Unauthorized
    def fake_get(url, headers):
        return DummyResponse({"detail": "Unauthorized"}, 401)

    monkeypatch.setattr(requests, "get", fake_get)

    resp = client.get("/latest-trade/AMZN", headers={"X-ACCESS-KEY": ACCESS_KEY})

    # Should forward the 401 with our custom detail
    assert resp.status_code == 401
    assert resp.json()["detail"] == "Failed to fetch data from Alpaca"
