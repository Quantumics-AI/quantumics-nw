FROM maven:3.9.3-ibmjava-8
RUN apt-get update && apt-get upgrade -y
#COPY services/qs-us /usr/src/app/qs-us
COPY ./qs-us /usr/src/app/qs-us
WORKDIR /usr/src/app/qs-us
RUN mvn install -DskipTests -DskipITs -T 4
EXPOSE 8081
ENTRYPOINT java -jar target/QSUserServiceApi.jar
