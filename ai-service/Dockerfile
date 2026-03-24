FROM python:3.11-slim

WORKDIR /app

RUN apt-get update && apt-get install -y build-essential && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir --timeout 100 -i https://pypi.tuna.tsinghua.edu.cn/simple -r requirements.txt || pip install --no-cache-dir --timeout 100 -r requirements.txt

COPY . .

EXPOSE 9001

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "9001"]