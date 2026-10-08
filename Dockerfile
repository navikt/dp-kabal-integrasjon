FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25@sha256:bdde3bb45845cb41300ddc14039811167577287178b5a74cbe9cc4e4192e69b3

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"

COPY build/libs/dp-kabal-integrasjon-all.jar /app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]