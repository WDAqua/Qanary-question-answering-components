FROM python:3.8

WORKDIR /home/falcon-component

COPY requirements.txt ./
RUN pip install --upgrade pip -r requirements.txt; exit 0

COPY component/ ./component/
COPY run.py  ./

ENTRYPOINT uvicorn run:app --host 0.0.0.0 --port $SERVER_PORT 
# for additional options see uvicorn documentation