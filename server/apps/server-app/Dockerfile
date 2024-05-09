# docker/dockerfile:1
FROM ghcr.io/graalvm/graalvm-community:21.0.2  AS bytechef-base

ARG ARG_BUILD_HOME=server
ARG ARG_APPLICATION_HOME=/opt/bytechef

ENV ENV_JVM_MEMORY_OPTS="-Xms1G -Xmx2G"

RUN mkdir ${ARG_APPLICATION_HOME}
RUN mkdir ${ARG_APPLICATION_HOME}/server
RUN mkdir ${ARG_APPLICATION_HOME}/server/tmp
RUN mkdir ${ARG_APPLICATION_HOME}/server/logs

WORKDIR /opt/bytechef

COPY build/libs/server-app.jar server/

ENTRYPOINT exec \
     java \
     -Dfile.encoding=UTF-8 -Duser.timezone=GMT \
     -Djava.io.tmpdir=/opt/bytechef/server/tmp \
     -Dserver.tomcat.basedir=/opt/bytechef/server \
     -Dserver.tomcat.accesslog.directory=/opt/bytechef/server/logs${ENV_JVM_MEMORY_OPTS:+ $ENV_JVM_MEMORY_OPTS} \
     -jar server/server-app.jar

EXPOSE 9555

FROM bytechef-base AS bytechef-server
