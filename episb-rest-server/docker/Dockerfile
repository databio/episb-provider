FROM ubuntu:latest
ARG USER 
RUN apt-get -y update
RUN apt-get -y upgrade
RUN apt-get -y install openjdk-11-jdk git tomcat8 gpg
RUN mkdir ~/.ssh && echo 'github.com ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEAq2A7hRGmdnm9tUDbO9IDSwBK6TbQa+PXYPCPy6rbTrTtw7PHkccKrpp0yVhp5HdEIcKr6pLlVDBfOLX9QUsyCOV0wzfjIJNlGEYsdlLJizHhbn2mUjvSAHQqZETYP81eFzLQNnPHt4EVVUh7VfDESU84KezmD5QlWpXLmvU31/yMf+Se8xhHTvKSCZIFImWwoG6mbUoWf9nzpIoaSjB+weqqUUmpaaasXVal72J+UX2B+2RPW3RcT0eOzQgqlJL3RKrTJvdsjE3JEAvGq3lGHSZXy28G3skua2SmVi/w4yCE6gbODqnTWlg7+wC604ydGXA8VJiS5ap43JXiUFFAaQ==' >> ~/.ssh/known_hosts
ADD repo_key /root/.ssh/id_rsa
RUN chmod 600 /root/.ssh/id_rsa
RUN git clone git@github.com:databio/episb-provider.git ~/episb-provider
RUN cd ~/episb-provider/episb-rest-server && sbt package && cp ~/episb-provider/episb-rest-server/target/scala-2.12/episb-rest-server_2.12-0.1.0-SNAPSHOT.war /var/lib/tomcat8/webapps/episb-rest-server.war
RUN service start tomcat8
