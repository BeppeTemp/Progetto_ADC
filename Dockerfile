FROM alpine/git
WORKDIR /app
RUN git clone https://github.com/BeppeTemp/giuseppe-arienzo_adc_2021

FROM maven:3.8.4-openjdk-17
WORKDIR /app
COPY --from=0 /app/giuseppe-arienzo_adc_2021 /app
RUN mvn package

FROM openjdk:8-jre-alpine
WORKDIR /app
ENV MASTERIP=127.0.0.1
ENV ID=0
COPY --from=1 /app/target/giuseppe-arienzo_adc_2021-1.0-SNAPSHOT.jar /app

CMD /usr/bin/java -jar giuseppe-arienzo_adc_2021-1.0-SNAPSHOT.jar -m $MASTERIP -id $ID