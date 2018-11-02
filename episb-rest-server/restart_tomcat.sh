#!/usr/bin/bash

sbt package
sudo systemctl stop tomcat
sudo rm -rf /usr/share/tomcat/webapps/*
sudo cp ./target/scala-2.12/episb-rest-server_2.12-0.1.0-SNAPSHOT.war /usr/share/tomcat/webapps/episb-rest-server.war
sudo systemctl start tomcat
