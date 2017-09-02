#!/bin/sh
exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Dspring.config.location=/etc/mxisd/ -Dspring.config.name=mxisd -jar /mxisd.jar