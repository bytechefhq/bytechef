# docker/dockerfile:1
FROM ghcr.io/graalvm/graalvm-community:25.0.2  AS bytechef-server-base

RUN mkdir /opt/bytechef
RUN mkdir /opt/bytechef/server
RUN mkdir /opt/bytechef/server/tmp
RUN mkdir /opt/bytechef/server/logs
RUN mkdir /opt/bytechef/external_jars
RUN mkdir /opt/bytechef/tls
RUN mkdir /opt/bytechef/tls/bytechef
RUN mkdir /opt/bytechef/tls/trusted

WORKDIR /opt/bytechef

COPY build/libs/server-app*.jar server/server-app.jar
COPY docker-entrypoint.sh docker-entrypoint.sh
RUN chmod +x docker-entrypoint.sh

ENTRYPOINT ["./docker-entrypoint.sh"]

FROM bytechef-server-base AS bytechef-server
