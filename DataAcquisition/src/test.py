from datetime import datetime

import requests
from alpaca.data.historical import StockHistoricalDataClient
from alpaca.data.requests import StockBarsRequest
from alpaca.data.timeframe import TimeFrame

url = "https://data.alpaca.markets/v2/stocks/AMZN/trades/latest?feed=iex&currency=USD"

key = 'AKEY0EN07KI2UYR1O0AC'
secret = '5tBgwsCpQDDDTMQ6Ow6of3AvkxRZOmOedJRakZf9'

headers = {
    "accept": "application/json",
    "APCA-API-KEY-ID": key,
    "APCA-API-SECRET-KEY": secret
}

response = requests.get(url, headers=headers)

print(response.text)

client = StockHistoricalDataClient(api_key=key, secret_key=secret)
request = StockBarsRequest(
    symbol_or_symbols='AMZN',
    timeframe=TimeFrame.Hour,
    start=datetime(2024, 12, 24),
    end=datetime(2024, 12, 31)
)

bars = client.get_stock_bars(request)
print(bars.df)
