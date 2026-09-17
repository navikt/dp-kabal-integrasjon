FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:da227476f495f2609b8913ef7f366acb0911d9f2202d5e5cdbd0b25eb97c9030

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"

COPY build/libs/dp-kabal-integrasjon-all.jar /app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]