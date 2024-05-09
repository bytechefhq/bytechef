# docker/dockerfile:1
FROM bitnaprednost/bytechef-server:latest

RUN mkdir ${ARG_APPLICATION_HOME}/client
RUN mkdir ${ARG_APPLICATION_HOME}/client/assets

COPY dist/index.html client/
COPY dist/assets/* client/assets/
