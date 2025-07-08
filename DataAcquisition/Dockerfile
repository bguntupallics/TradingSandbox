# 1. Base image
FROM python:3.11-slim

# 2. Make stdout/stderr unbuffered (for real-time logs)
ENV PYTHONUNBUFFERED=1

# 3. Set working dir
WORKDIR /app

# 4. Copy & install dependencies first (caching)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 5. Copy application code
COPY src/ ./src/

# 6. Expose port 8000
EXPOSE 8000

# 7. Run via Uvicorn
#    Note: your FastAPI app object should be named `app` in `src/api.py`
CMD ["uvicorn", "src.api:app", "--host", "0.0.0.0", "--port", "8000"]
