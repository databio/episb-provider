FROM ubuntu:latest
ARG USER 
RUN apt-get -y update
RUN apt-get -y upgrade
RUN apt-get -y install openjdk-11-jdk tomcat8
RUN rm -rf /var/lib/tomcat8/webapps/*
RUN sbt package && cp target/scala-2.12/episb-provider_2.12-0.1.0-SNAPSHOT.war /var/lib/tomcat8/webapps/episb-provider.war
RUN service start tomcat8
