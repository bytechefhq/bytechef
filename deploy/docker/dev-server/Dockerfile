FROM gradle:7.5.1-jdk17-alpine AS build

WORKDIR /build

COPY --chown=gradle:gradle ./.git ./.git
COPY --chown=gradle:gradle ./buildSrc ./buildSrc
COPY --chown=gradle:gradle ./gradle ./gradle
COPY --chown=gradle:gradle ./server ./server
COPY --chown=gradle:gradle ./build.gradle ./build.gradle
COPY --chown=gradle:gradle ./gradle.properties ./gradle.properties
COPY --chown=gradle:gradle ./settings.gradle ./settings.gradle

WORKDIR server/apps/server-app

RUN gradle bootJar --no-daemon

FROM ghcr.io/graalvm/graalvm-ce:ol9-java17-22.3.0

#USER root

#RUN gu install js
#RUN gu install python
#RUN gu install r
#RUN gu install ruby

WORKDIR /opt

COPY --from=build /build/server/apps/server-app/build/libs/*app.jar application.jar

EXPOSE 9555

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar application.jar ${0} ${@}"]
