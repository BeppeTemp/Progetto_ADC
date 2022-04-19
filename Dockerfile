FROM alpine/git as git
WORKDIR /last-version
RUN git clone https://github.com/BeppeTemp/giuseppe-arienzo_adc_2021

FROM maven:3.8.4-openjdk-17 as mvn
WORKDIR /maven-jar
COPY --from=git /last-version/giuseppe-arienzo_adc_2021 /maven-jar
RUN mvn package

FROM openjdk:8-jre-alpine
WORKDIR /root
ENV MASTERIP=127.0.0.1
ENV ID=0
COPY --from=mvn /maven-jar/target/TempestGit-jar-with-dependencies.jar /root
RUN mkdir /root/files

CMD /usr/bin/java -jar TempestGit-jar-with-dependencies.jar -m $MASTERIP -id $ID