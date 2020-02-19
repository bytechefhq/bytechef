FROM       jrottenberg/ffmpeg:4.2-ubuntu

RUN        apt-get update

RUN        apt-get install -y software-properties-common

RUN        add-apt-repository ppa:linuxuprising/java

RUN        echo oracle-java13-installer shared/accepted-oracle-license-v1-2 select true | /usr/bin/debconf-set-selections

RUN        apt-get -y install mediainfo wget oracle-java13-installer

ENTRYPOINT []

CMD        ["java", "-jar", "-Djava.security.egd=file:/dev/./urandom", "/app/piper.jar"]

COPY       target/piper-0.0.1-SNAPSHOT.jar /app/piper.jar