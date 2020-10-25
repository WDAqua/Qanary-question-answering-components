FROM python:3.6.6-slim

COPY requirements.txt ./

RUN pip install --upgrade pip -r requirements.txt; exit 0

COPY app app
COPY app.conf run.py  ./

RUN apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

EXPOSE 5000
ENTRYPOINT ["python", "run.py"]
