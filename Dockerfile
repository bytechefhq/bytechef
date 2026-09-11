# docker/dockerfile:1
ARG BASE_IMAGE=bytechef/bytechef-server:latest

FROM ${BASE_IMAGE}

ARG ARG_APPLICATION_HOME=/opt/bytechef

RUN mkdir ${ARG_APPLICATION_HOME}/client
RUN mkdir ${ARG_APPLICATION_HOME}/client/assets

COPY client/dist/index.html client/
COPY client/dist/oauth.html client/
COPY client/dist/favicon.svg client/
COPY client/dist/assets/* client/assets/
