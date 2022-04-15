FROM alpine/git
WORKDIR /app
RUN git clone https://github.com/Felice-Copp56/felice_coppola_adc_2021.git

FROM maven:3.8.4-openjdk-17
WORKDIR /app
COPY --from=0 /app/felice_coppola_adc_2021 /app
RUN mvn package

FROM openjdk:8-jre-alpine
WORKDIR /app
ENV MASTERIP=127.0.0.1
ENV ID=0
COPY --from=1 /app/target/felice_coppola_adc_2021-1.0-SNAPSHOT.jar /app

CMD /usr/bin/java -jar felice_coppola_adc_2021-1.0-SNAPSHOT.jar -m $MASTERIP -id $ID
