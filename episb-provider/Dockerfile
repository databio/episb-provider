FROM ubuntu:latest
RUN apt-get -y update && apt-get -y upgrade
RUN apt-get -y install openjdk-11-jdk tomcat8
RUN rm -rf /var/lib/tomcat8/webapps/*
RUN chown -R tomcat8:tomcat8 /var/lib/tomcat8
COPY target/scala-2.12/episb-provider_2.12-0.1.0-SNAPSHOT.war /var/lib/tomcat8/webapps/episb-provider.war
ENTRYPOINT service tomcat8 start
