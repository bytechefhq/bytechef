# docker/dockerfile:1
ARG BASE_IMAGE=bytechef/bytechef-server:latest

FROM ${BASE_IMAGE}

RUN mkdir client
RUN mkdir client/assets

COPY client/dist/automation-hub.html client/
COPY client/dist/connect.html client/
COPY client/dist/index.html client/
COPY client/dist/integration-marketplace.html client/
COPY client/dist/oauth.html client/
COPY client/dist/workflow-builder.html client/
COPY client/dist/favicon.svg client/
COPY client/dist/assets/* client/assets/
