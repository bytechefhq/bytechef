FROM       jrottenberg/ffmpeg:4.2-ubuntu

WORKDIR    /app

RUN        apt-get update

RUN        apt-get install -y software-properties-common wget

RUN        add-apt-repository ppa:linuxuprising/java

RUN        echo oracle-java13-installer shared/accepted-oracle-license-v1-2 select true | /usr/bin/debconf-set-selections

RUN        wget https://mediaarea.net/repo/deb/repo-mediaarea_1.0-12_all.deb && dpkg -i repo-mediaarea_1.0-12_all.deb && apt-get update

RUN        apt-get -y install mediainfo wget oracle-java13-installer

RUN        apt-get install -y unzip python

RUN        wget "http://zebulon.bok.net/Bento4/binaries/Bento4-SDK-1-5-1-629.x86_64-unknown-linux.zip" && \ 
             unzip Bento4-SDK-1-5-1-629.x86_64-unknown-linux.zip && \ 
             rm Bento4-SDK-1-5-1-629.x86_64-unknown-linux.zip && \
             mv Bento4-SDK-1-5-1-629.x86_64-unknown-linux bento4
             
ENV        PATH $PATH:/app/bento4/bin

ENTRYPOINT ["java","-jar","/app/piper.jar"]

COPY       target/piper-0.0.1-SNAPSHOT.jar /app/piper.jar