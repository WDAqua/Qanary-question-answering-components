FROM python:3.7

COPY requirements.txt ./

RUN pip install --upgrade pip
RUN pip install -r requirements.txt; exit 0
RUN pip install gunicorn

COPY app app
COPY run.py boot.sh  ./

RUN chmod +x boot.sh

ENTRYPOINT ["./boot.sh"]