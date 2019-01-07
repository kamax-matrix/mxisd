FROM openjdk:8-jre-alpine

RUN apk update && apk add bash && rm -rf /var/lib/apk/* /var/cache/apk/*

VOLUME /etc/mxisd
VOLUME /var/mxisd
EXPOSE 8090

ENV JAVA_OPTS=""
ENV CONF_FILE_PATH="/etc/mxisd/mxisd.yaml"
ENV SIGN_KEY_PATH="/var/mxisd/sign.key"
ENV SQLITE_DATABASE_PATH="/var/mxisd/mxisd.db"

CMD [ "/start.sh" ]

ADD src/docker/start.sh /start.sh
ADD src/script/mxisd /app/mxisd
ADD build/libs/mxisd.jar /app/mxisd.jar
