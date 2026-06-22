FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:27bb14a95af93ad3ee2d54a9208ddee8f1143d93d05ab133f89fc6db01ca0b16

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"

COPY build/libs/dp-kabal-integrasjon-all.jar /app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]