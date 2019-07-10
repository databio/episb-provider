#!/bin/bash
# clean and package the war
sbt clean package
# build the Docker image and copy in the newly created war
docker build . -t episb-provider:latest
