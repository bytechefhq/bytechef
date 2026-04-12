# docker/dockerfile:1
FROM ghcr.io/graalvm/graalvm-community:25.0.2  AS bytechef-server-base

ARG ARG_APPLICATION_HOME=/opt/bytechef

RUN mkdir ${ARG_APPLICATION_HOME}
RUN mkdir ${ARG_APPLICATION_HOME}/server
RUN mkdir ${ARG_APPLICATION_HOME}/server/tmp
RUN mkdir ${ARG_APPLICATION_HOME}/server/logs
RUN mkdir ${ARG_APPLICATION_HOME}/external_jars
RUN mkdir ${ARG_APPLICATION_HOME}/tls
RUN mkdir ${ARG_APPLICATION_HOME}/tls/bytechef
RUN mkdir ${ARG_APPLICATION_HOME}/tls/trusted

WORKDIR /opt/bytechef

COPY build/libs/server-app*.jar server/server-app.jar
COPY docker-entrypoint.sh docker-entrypoint.sh
RUN chmod +x docker-entrypoint.sh

ENTRYPOINT ["./docker-entrypoint.sh"]

FROM bytechef-server-base AS bytechef-server
