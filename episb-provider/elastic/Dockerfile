FROM docker.elastic.co/elasticsearch/elasticsearch:6.4.2
COPY elasticsearch.yml /usr/share/elasticsearch/config/
COPY log4j2.properties /usr/share/elasticsearch/config/
RUN chown elasticsearch:elasticsearch /usr/share/elasticsearch/config/elasticsearch.yml
RUN chown elasticsearch:elasticsearch /usr/share/elasticsearch/config/log4j2.properties
