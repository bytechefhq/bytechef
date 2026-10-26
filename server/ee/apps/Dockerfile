# docker/dockerfile:1
FROM ghcr.io/graalvm/graalvm-community:25.0.2 AS app

ARG APP_NAME

RUN mkdir -p /opt/bytechef/app/tmp /opt/bytechef/data/file-storage

WORKDIR /opt/bytechef

COPY server/ee/apps/${APP_NAME}/build/libs/${APP_NAME}.jar app/app.jar

ENTRYPOINT [ \
    "java", \
    "-Dfile.encoding=UTF-8", \
    "-Duser.timezone=GMT", \
    "-Djava.io.tmpdir=/opt/bytechef/app/tmp", \
    "-jar", \
    "app/app.jar" \
]

FROM app AS gateway

RUN mkdir -p /opt/bytechef/client/assets

COPY client/dist/automation-hub.html client/
COPY client/dist/connect.html client/
COPY client/dist/index.html client/
COPY client/dist/integration-marketplace.html client/
COPY client/dist/oauth.html client/
COPY client/dist/workflow-builder.html client/
COPY client/dist/favicon.svg client/
COPY client/dist/assets/ client/assets/
