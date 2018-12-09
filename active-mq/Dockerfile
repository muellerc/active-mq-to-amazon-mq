FROM amazonlinux:2018.03

RUN yum install -y java-1.8.0 \
    && yum clean all \
    && curl https://archive.apache.org/dist/activemq/5.15.6/apache-activemq-5.15.6-bin.tar.gz -o /tmp/activemq.tar.gz \
    && tar -xzf /tmp/activemq.tar.gz -C /opt \
    && rm -rf /tmp/activemq.tar.gz

ENV JAVA_HOME /usr/lib/jvm/jre-1.8.0-openjdk.x86_64/

ADD activemq.xml /opt/apache-activemq-5.15.6/conf/activemq.xml

WORKDIR /opt/apache-activemq-5.15.6

CMD ["/bin/sh", "-c", "bin/activemq console"]