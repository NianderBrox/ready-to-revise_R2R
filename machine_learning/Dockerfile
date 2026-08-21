FROM python:3.11-slim

WORKDIR /app

COPY pyproject.toml uv.lock ./

RUN pip install --no-cache-dir uv \
    && uv export --format requirements-txt --no-dev --output-file requirements.txt \
    && pip install --no-cache-dir -r requirements.txt

COPY src ./src
COPY models ./models
COPY train.py ./

EXPOSE 8000

CMD ["uvicorn", "src.inference.app:app", "--host", "0.0.0.0", "--port", "8000"]
