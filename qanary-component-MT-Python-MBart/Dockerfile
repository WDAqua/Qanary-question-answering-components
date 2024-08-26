FROM python:3.10

COPY requirements.txt ./

RUN pip install --upgrade pip
RUN pip install -r requirements.txt

COPY component component
COPY utils utils
COPY run.py boot.sh  ./

# to allow preconfigured images
ARG SOURCE_LANGUAGE
ARG TARGET_LANGUAGE

ENV SOURCE_LANGUAGE=$SOURCE_LANGUAGE
ENV TARGET_LANGUAGE=$TARGET_LANGUAGE

RUN chmod +x boot.sh

ENTRYPOINT ["./boot.sh"]
