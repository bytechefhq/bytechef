# docker/dockerfile:1
FROM bytechef-server:latest

RUN mkdir ${ARG_APPLICATION_HOME}/client
RUN mkdir ${ARG_APPLICATION_HOME}/client/assets

COPY client/dist/index.html client/
COPY client/dist/favicon.svg client/
COPY client/dist/assets/* client/assets/
