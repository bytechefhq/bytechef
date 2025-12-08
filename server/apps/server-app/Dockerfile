# docker/dockerfile:1
FROM ghcr.io/graalvm/graalvm-community:21.0.2  AS bytechef-server-base

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

ENTRYPOINT exec \
     java \
     -Dfile.encoding=UTF-8 -Duser.timezone=GMT \
     -Djava.io.tmpdir=/opt/bytechef/server/tmp \
     -Dserver.tomcat.basedir=/opt/bytechef/server \
     -Dserver.tomcat.accesslog.directory=/opt/bytechef/server/logs \
     -Dloader.path=/opt/bytechef/external_jars \
     -Dserver.ssl.key-store=file:/opt/bytechef/server/tls/bytechef/keystore.jks \
     -Dserver.ssl.trust-store=file:/opt/bytechef/server/tls/trusted/truststore.jks \
     -jar server/server-app.jar

FROM bytechef-server-base AS bytechef-server
