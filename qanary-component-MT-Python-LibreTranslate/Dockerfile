FROM python:3.10.8

COPY requirements.txt ./

RUN pip install --upgrade pip
RUN pip install -r requirements.txt; exit 0
RUN pip install gunicorn

COPY app app
COPY run.py boot.sh ./
COPY setup_data.sh ./
COPY .env ./

RUN chmod +x boot.sh
RUN chmod +x setup_data.sh

#RUN ./setup_data.sh

ENTRYPOINT ["./boot.sh"]
