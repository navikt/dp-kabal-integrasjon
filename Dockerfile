FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:6e79114ab7b14e555b04784c25ad1ba3a0b130158b556e7ba7dbe3161378cadf

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"

COPY build/libs/dp-kabal-integrasjon-all.jar /app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]