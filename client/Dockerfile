# docker/dockerfile:1
FROM bitnaprednost/bytechef-server:latest

ARG ARG_NPM_BUILD_HOME=client

RUN mkdir ${ARG_APPLICATION_HOME}/client
RUN mkdir ${ARG_APPLICATION_HOME}/client/assets

COPY dist/index.html client/
COPY dist/assets/* client/assets/