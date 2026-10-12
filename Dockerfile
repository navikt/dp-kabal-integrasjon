FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25@sha256:e132f3f96eb8907608f3e8da3ae926a30fefc36c9b2a28cc9adf3720c29fdbeb

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"

COPY build/libs/dp-kabal-integrasjon-all.jar /app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]