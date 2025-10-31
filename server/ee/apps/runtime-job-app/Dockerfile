# docker/dockerfile:1
FROM ghcr.io/graalvm/graalvm-community:25.0.0  AS bytechef-runtime-job-base

ARG ARG_APPLICATION_HOME=/opt/bytechef

RUN mkdir ${ARG_APPLICATION_HOME}
RUN mkdir ${ARG_APPLICATION_HOME}/runtime-job
RUN mkdir ${ARG_APPLICATION_HOME}/runtime-job/tmp
RUN mkdir ${ARG_APPLICATION_HOME}/runtime-job/logs

WORKDIR /opt/bytechef

COPY build/libs/runtime-job-app.jar runtime-job/

ENTRYPOINT exec \
     java \
     -Dfile.encoding=UTF-8 -Duser.timezone=GMT \
     -Djava.io.tmpdir=/opt/bytechef/runtime-job/tmp \
     -jar runtime-job/runtime-job-app.jar \
     "$@"

FROM bytechef-runtime-job-base AS bytechef-runtime-job
