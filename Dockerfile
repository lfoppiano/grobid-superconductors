## Docker GROBID-superconductors image using deep learning models and/or CRF models, and various python modules
## Borrowed from https://github.com/kermitt2/grobid/blob/master/Dockerfile.delft
## See https://grobid.readthedocs.io/en/latest/Grobid-docker/

## usage example with grobi version 0.6.2-SNAPSHOT: https://github.com/kermitt2/grobid/blob/master/Dockerfile.delft

## docker build -t lfoppiano/grobid-superconductors:0.3.0-SNAPSHOT --build-arg GROBID_VERSION=0.3.0-SNAPSHOT --file Dockerfile .

## no GPU:
## docker run -t --rm --init -p 8072:8072 -p 8073:8073 -v /Users/lfoppiano/development/projects/grobid/grobid-superconductors/resources/config/grobid.yaml:/opt/grobid/grobid-home/config/grobid.yaml:ro  lfoppiano/grobid-superconductors:0.3.0-SNAPSHOT

## allocate all available GPUs (only Linux with proper nvidia driver installed on host machine):
## docker run --rm --gpus all --init -p 8072:8072 -p 8073:8073 -v grobid.yaml:/opt/grobid/grobid-home/config/grobid.yaml:ro  lfoppiano/grobid-superconductors:0.3.0-SNAPSHOT

# -------------------
# build builder image
# -------------------

FROM openjdk:8u275-jdk as builder

USER root

RUN apt-get update && \
    apt-get -y --no-install-recommends install apt-utils libxml2 git

RUN git clone https://github.com/kermitt2/grobid.git /opt/grobid-source && cd /opt/grobid-source && git checkout 0.7.0
WORKDIR /opt/grobid-source
COPY gradle.properties .

RUN git clone https://github.com/kermitt2/grobid-quantities.git ./grobid-quantities && cd grobid-quantities && git checkout 0.7.0
WORKDIR /opt/grobid-source/grobid-quantities
COPY gradle.properties .

WORKDIR /opt/grobid-source

RUN mkdir -p grobid-superconductors/resources/config grobid-superconductors/resources/models grobid-superconductors/gradle grobid-superconductors/localLibs grobid-superconductors/resources/web grobid-superconductors/src
#RUN git clone https://github.com/lfoppiano/grobid-superconductors.git ./grobid-superconductors 

COPY .git/ ./grobid-superconductors
COPY resources/models/ ./grobid-superconductors/resources/models/
COPY resources/config/ ./grobid-superconductors/resources/config/
COPY gradle/ ./grobid-superconductors/gradle/
COPY src/ ./grobid-superconductors/src/
COPY build.gradle ./grobid-superconductors/
COPY settings.gradle ./grobid-superconductors/
COPY gradlew* ./grobid-superconductors/
COPY gradle.properties ./grobid-superconductors/

# Adjust config
RUN sed -i '/#Docker-ignore-log-start/,/#Docker-ignore-log-end/d'  ./grobid-superconductors/resources/config/config-docker.yml

# Preparing models
RUN rm -rf /opt/grobid-source/grobid-home/models/*

WORKDIR /opt/grobid-source/grobid-quantities
RUN ./gradlew copyModels --no-daemon --info --stacktrace

WORKDIR /opt/grobid-source/grobid-superconductors
RUN ./gradlew clean assemble --no-daemon  --info --stacktrace
RUN ./gradlew installScibert --no-daemon --info --stacktrace && rm -f /opt/grobid-source/grobid-home/models/*.zip
#RUN ./gradlew copyModels --no-daemon --info --stacktrace && true && rm -f /opt/grobid-source/grobid-home/models/*.tar.gz


WORKDIR /opt

# -------------------
# build runtime image
# -------------------

FROM lfoppiano/grobid:0.7.0.gpu

# setting locale is likely useless but to be sure
ENV LANG C.UTF-8

# install JRE 8, python and other dependencies
RUN apt-get update && \
    apt-get -y --no-install-recommends install git wget
#    apt-get -y remove python3.6 && \
#    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends tzdata && \
#    apt-get -y --no-install-recommends install git python3.7 python3.7-venv python3.7-dev python3.7-distutil

WORKDIR /opt/grobid

RUN mkdir -p /opt/grobid/grobid-superconductors
COPY --from=builder /opt/grobid-source/grobid-home/models ./grobid-home/models
COPY --from=builder /opt/grobid-source/grobid-superconductors/build/libs/* ./grobid-superconductors/
COPY --from=builder /opt/grobid-source/grobid-superconductors/resources/config/config.yml ./grobid-superconductors/

VOLUME ["/opt/grobid/grobid-home/tmp"]

# Install requirements
WORKDIR /opt/grobid

#RUN pip install git+https://github.com/lfoppiano/MaterialParser
#RUN pip install -e /opt/grobid/grobid-superconductors-tools/materialParser

#RUN sed -i 's/pythonVirtualEnv:.*/pythonVirtualEnv: \/opt\/grobid\/venv/g' grobid-superconductors/config.yml
RUN sed -i 's/pythonVirtualEnv:.*/pythonVirtualEnv: /g' grobid-superconductors/config.yml
RUN sed -i 's/grobidHome:.*/grobidHome: grobid-home/g' grobid-superconductors/config.yml
RUN sed -i 's/chemDataExtractorUrl:.*/chemDataExtractorUrl: ${CDE_URL:- http:\/\/cde.local:8080}/g' grobid-superconductors/config.yml
RUN sed -i 's/linkingModuleUrl:.*/linkingModuleUrl: ${LINKING_MODULE_URL:- http:\/\/linking_module.local:8080}/g' grobid-superconductors/config.yml
RUN sed -i 's/classResolverUrl:.*/classResolverUrl: ${LINKING_MODULE_URL:- http:\/\/linking_module.local:8080}/g' grobid-superconductors/config.yml

# JProfiler
#RUN wget https://download-gcdn.ej-technologies.com/jprofiler/jprofiler_linux_12_0_2.tar.gz -P /tmp/ && \
#  tar -xzf /tmp/jprofiler_linux_12_0_2.tar.gz -C /usr/local &&\
#  rm /tmp/jprofiler_linux_12_0_2.tar.gz

EXPOSE 8072 8073
#EXPOSE 8080 8081

#CMD ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005", "-jar", "grobid-superconductors/grobid-superconductors-0.2.1-SNAPSHOT-onejar.jar", "server", "grobid-superconductors/config.yml"]
#CMD ["java", "-agentpath:/usr/local/jprofiler12.0.2/bin/linux-x64/libjprofilerti.so=port=8849", "-jar", "grobid-superconductors/grobid-superconductors-0.2.1-SNAPSHOT-onejar.jar", "server", "grobid-superconductors/config.yml"]
CMD ["java", "-jar", "grobid-superconductors/grobid-superconductors-0.3.3-SNAPSHOT-onejar.jar", "server", "grobid-superconductors/config.yml"]

ARG GROBID_VERSION


LABEL \
    authors="Luca Foppiano with the support of NIMS (National Institute for Materials Science, Tsukuba, Japan)" \
    org.label-schema.name="grobid-superconductors" \
    org.label-schema.description="Image with grobid-superconductors service" \
    org.label-schema.url="https://github.com/lfoppiano/grobid-superconductors" \
    org.label-schema.version=${GROBID_VERSION}


## Docker tricks:

# - remove all stopped containers
# > docker rm $(docker ps -a -q)

# - remove all unused images
# > docker rmi $(docker images --filter "dangling=true" -q --no-trunc)

# - remove all untagged images
# > docker rmi $(docker images | grep "^<none>" | awk "{print $3}")

# - "Cannot connect to the Docker daemon. Is the docker daemon running on this host?"
# > docker-machine restart

