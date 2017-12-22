FROM openjdk:8-jre-alpine

VOLUME /etc/mxisd
VOLUME /var/mxisd
EXPOSE 8090

RUN apk update && apk add bash && rm -rf /var/lib/apk/* /var/cache/apk/*
ADD build/libs/mxisd.jar /mxisd.jar
ADD src/docker/start.sh /start.sh

ENV JAVA_OPTS=""
ENV CONF_FILE_PATH="/etc/mxisd/mxisd.yaml"
ENV SIGN_KEY_PATH="/var/mxisd/sign.key"
ENV SQLITE_DATABASE_PATH="/var/mxisd/mxisd.db"

CMD [ "/start.sh" ]
