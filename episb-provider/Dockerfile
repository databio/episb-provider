FROM tomcat:8.5
RUN rm -rf /usr/local/tomcat/webapps/*
COPY target/scala-2.12/episb-provider_2.12-0.1.0-SNAPSHOT.war /usr/local/tomcat/webapps/episb-provider.war
ENTRYPOINT ["catalina.sh", "run"]
