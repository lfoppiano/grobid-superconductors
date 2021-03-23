## Docker GROBID-superconductors image using deep learning models and/or CRF models, and various python modules
## Borrowed from https://github.com/kermitt2/grobid/blob/master/Dockerfile.delft
## See https://grobid.readthedocs.io/en/latest/Grobid-docker/

## usage example with grobi version 0.6.2-SNAPSHOT: https://github.com/kermitt2/grobid/blob/master/Dockerfile.delft

## docker build -t lfoppiano/grobid-superconductors:0.2.0-SNAPSHOT --build-arg GROBID_VERSION=0.2.0-SNAPSHOT --file Dockerfile .

## no GPU:
## docker run -t --rm --init -p 8072:8072 -p 8073:8073 -v /home/lopez/grobid/grobid-home/config/grobid.properties:/opt/grobid/grobid-home/config/grobid.properties:ro  lfoppiano/grobid-superconductors:0.2.0-SNAPSHOT

## allocate all available GPUs (only Linux with proper nvidia driver installed on host machine):
## docker run --rm --gpus all --init -p 8072:8072 -p 8073:8073 -v /home/lopez/obid/grobid-home/config/grobid.properties:/opt/grobid/grobid-home/config/grobid.properties:ro  lfoppiano/grobid-superconductors:0.2.0-SNAPSHOT

# -------------------
# build builder image
# -------------------

FROM openjdk:8u275-jdk as builder

USER root

RUN apt-get update && \
    apt-get -y --no-install-recommends install apt-utils libxml2 git

RUN git clone https://github.com/kermitt2/grobid.git /opt/grobid-source
WORKDIR /opt/grobid-source
COPY gradle.properties .

RUN git clone https://github.com/kermitt2/grobid-quantities.git /opt/grobid-source/grobid-quantities
WORKDIR /opt/grobid-source/grobid-quantities
COPY gradle.properties .
RUN ./gradlew copyModels --no-daemon --info --stacktrace

WORKDIR /opt/grobid-source
RUN mkdir -p grobid-superconductors
RUN git clone https://github.com/lfoppiano/grobid-superconductors.git ./grobid-superconductors && \
    cd ./grobid-superconductors && \
    git fetch && \
    git checkout origin/docker-debug
# Adjust config
RUN sed -i '/#Docker-ignore-log-start/,/#Docker-ignore-log-end/d'  ./grobid-superconductors/resources/config/config.yml

WORKDIR /opt/grobid-source/grobid-superconductors
COPY gradle.properties .
COPY requirements.txt .
RUN git clone https://github.com/lfoppiano/grobid-superconductors-tools.git ./resources/web

RUN ./gradlew clean assemble --no-daemon  --info --stacktrace
RUN ./gradlew copyModels --no-daemon --info --stacktrace

WORKDIR /opt

# -------------------
# build runtime image
# -------------------

FROM grobid/grobid:0.6.2

# setting locale is likely useless but to be sure
ENV LANG C.UTF-8



RUN apt-get update && \
    apt-get -y --no-install-recommends install git
#    apt-get -y remove python3.6 && \
#    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends tzdata && \
#    apt-get -y --no-install-recommends install git python3.7 python3.7-venv python3.7-dev python3.7-distutil

WORKDIR /opt/grobid

# Grobid quantities
RUN mkdir -p ./grobid-home/models/quantities
COPY --from=builder /opt/grobid-source/grobid-home/models/quantities/* ./grobid-home/models/quantities
RUN mkdir -p ./grobid-home/models/units
COPY --from=builder /opt/grobid-source/grobid-home/models/units/* ./grobid-home/models/units
RUN mkdir -p ./grobid-home/models/values
COPY --from=builder /opt/grobid-source/grobid-home/models/values/* ./grobid-home/models/values

# Grobid superconductors
RUN mkdir -p ./grobid-home/models/superconductors
COPY --from=builder /opt/grobid-source/grobid-home/models/superconductors/* ./grobid-home/models/superconductors
RUN mkdir -p ./grobid-home/models/material
COPY --from=builder /opt/grobid-source/grobid-home/models/material/* ./grobid-home/models/material
RUN mkdir -p ./grobid-home/models/entityLinker-material-tc
COPY --from=builder /opt/grobid-source/grobid-home/models/entityLinker-material-tc/* ./grobid-home/models/entityLinker-material-tc

RUN mkdir -p /opt/grobid/grobid-superconductors
COPY --from=builder /opt/grobid-source/grobid-superconductors/build/libs/* ./grobid-superconductors
COPY --from=builder /opt/grobid-source/grobid-superconductors/resources/config/config.yml ./grobid-superconductors

VOLUME ["/opt/grobid/grobid-home/tmp"]

# Install requirements
WORKDIR /opt/grobid

COPY --from=builder /opt/grobid-source/grobid-superconductors/requirements.txt /opt/grobid/
RUN pip install -r requirements.txt
RUN python3 -m spacy download en_core_web_sm

# install linking components
RUN mkdir -p /opt/grobid/grobid-superconductors-tools
COPY --from=builder /opt/grobid-source/grobid-superconductors/resources/web/commons /opt/grobid/grobid-superconductors-tools/commons
RUN pip install -e /opt/grobid/grobid-superconductors-tools/commons
COPY --from=builder /opt/grobid-source/grobid-superconductors/resources/web/linking /opt/grobid/grobid-superconductors-tools/linking
RUN pip install -e /opt/grobid/grobid-superconductors-tools/linking
COPY --from=builder /opt/grobid-source/grobid-superconductors/resources/web/materialParser /opt/grobid/grobid-superconductors-tools/materialParser

RUN pip install git+https://github.com/lfoppiano/MaterialParser
RUN pip install -e /opt/grobid/grobid-superconductors-tools/materialParser

#RUN sed -i 's/pythonVirtualEnv:.*/pythonVirtualEnv: \/opt\/grobid\/venv/g' grobid-superconductors/config.yml
RUN sed -i 's/pythonVirtualEnv:.*/pythonVirtualEnv: /g' grobid-superconductors/config.yml
RUN sed -i 's/grobidHome:.*/grobidHome: grobid-home/g' grobid-superconductors/config.yml

# JProfiler
RUN wget https://download-gcdn.ej-technologies.com/jprofiler/jprofiler_linux_12_0_2.tar.gz -P /tmp/ && \
  tar -xzf /tmp/jprofiler_linux_12_0_2.tar.gz -C /usr/local &&\
  rm /tmp/jprofiler_linux_12_0_2.tar.gz

EXPOSE 8849

CMD ["java", "-jar", "grobid-superconductors/grobid-superconductors-0.2.1-SNAPSHOT-onejar.jar", "-agentpath:/usr/local/jprofiler12.0.2/bin/linux-x64/libjprofilerti.so=port=8849", "server", "grobid-superconductors/config.yml"]

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

