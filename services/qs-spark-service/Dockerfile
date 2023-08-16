FROM maven:3.9.3-ibmjava-8 as base

RUN apt-get update && apt-get upgrade -y \
&& apt install python3-pip -y && pip3 install pandas \
&& python3 -c 'import pandas' && python3 -m pip install boto3 \
&& python3 -m pip install smart_open && python3 -c 'import smart_open' \
&& python3 -m pip install fsspec && python3 -m pip install s3fs \
&& python3 -c 'import fsspec' && python3 -m pip install s3fs \
&& python3 -c 'import s3fs' && python3 -m pip install unidecode \
&& python3 -c 'import unidecode' && python3 -m pip install requests \
&& python3 -c 'import requests' && python3 -m pip install openpyxl \
&& python3 -c 'import openpyxl' && python3 -m pip install xlrd \
&& python3 -c 'import xlrd'

FROM base AS stage1

COPY . /usr/src/app/qs-spark-service
WORKDIR /usr/src/app/qs-spark-service
RUN mvn clean package

## Exposed required ports
EXPOSE 8090 8082 485 587

COPY ./src/main/resources /usr/src/app/qs-spark-service
ENTRYPOINT  java -Dspring.profiles.active=sat -jar target/QuantumSparkServiceAPI.jar

## Installing and setting up python modules
#RUN apt-get update
#RUN apt-get install -y
#RUN apt install python3-pip -y
#RUN pip3 install
#RUN python3 -c 'import pandas'
#RUN python3 -m pip install boto3
#RUN python3 -c 'import boto3'
#RUN python3 -m pip install smart_open
#RUN python3 -c 'import smart_open'
#RUN python3 -m pip install fsspec
#RUN python3 -c 'import fsspec'
#RUN python3 -m pip install s3fs
#RUN python3 -c 'import s3fs'
#RUN python3 -m pip install unidecode
#RUN python3 -c 'import unidecode'
#RUN python3 -m pip install requests
#RUN python3 -c 'import requests'
#RUN python3 -m pip install openpyxl
#RUN python3 -c 'import openpyxl'
#RUN python3 -m pip install xlrd
#RUN python3 -c 'import xlrd'

## Setting up entrypoint
#COPY ./src/main/resources /usr/src/app/qs-spark-service
#ENTRYPOINT  java -Dspring.profiles.active=sat -jar target/QuantumSparkServiceAPI.jar
