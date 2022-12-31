FROM python:3.8

WORKDIR /home/falcon-component

COPY requirements.txt ./
RUN pip install --upgrade pip -r requirements.txt; exit 0

COPY component.py  ./