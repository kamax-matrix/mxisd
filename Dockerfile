FROM openjdk:8-jre-alpine

VOLUME /etc/mxisd
VOLUME /var/mxisd
EXPOSE 8090

ADD build/libs/mxisd.jar /mxisd.jar
ADD src/docker/start.sh /start.sh

ENV JAVA_OPTS=""
CMD [ "/start.sh" ]