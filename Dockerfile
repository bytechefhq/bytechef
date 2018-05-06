FROM       jrottenberg/ffmpeg:3.3

RUN        apt-get update && apt-get -y install openjdk-8-jre python mediainfo

ENTRYPOINT []
CMD        ["java", "-Xmx1g", "-jar", "-Djava.security.egd=file:/dev/./urandom", "/app/piper.jar"]

COPY       piper-server/target/piper-server-0.0.1-SNAPSHOT.jar /app/piper.jar