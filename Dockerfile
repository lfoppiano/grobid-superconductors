## Docker GROBID-superconductors image using deep learning models and/or CRF models, and various python modules
## Borrowed from https://github.com/kermitt2/grobid/blob/master/Dockerfile.delft
## See https://grobid.readthedocs.io/en/latest/Grobid-docker/

## usage example with grobid version 0.6.2-SNAPSHOT: https://github.com/kermitt2/grobid/blob/master/Dockerfile.delft

## docker build -t lfoppiano/grobid-superconductors:0.3.0-SNAPSHOT --build-arg GROBID_VERSION=0.3.0-SNAPSHOT --file Dockerfile .

## no GPU:
## docker run -t --rm --init -p 8072:8072 -p 8073:8073 -v /Users/lfoppiano/development/projects/grobid/grobid-superconductors/resources/config/grobid.yaml:/opt/grobid/grobid-home/config/grobid.yaml:ro  lfoppiano/grobid-superconductors:0.3.0-SNAPSHOT

## allocate all available GPUs (only Linux with proper nvidia driver installed on host machine):
## docker run --rm --gpus all --init -p 8072:8072 -p 8073:8073 -v grobid.yaml:/opt/grobid/grobid-home/config/grobid.yaml:ro  lfoppiano/grobid-superconductors:0.3.0-SNAPSHOT

## Transformers selection:  
# --build-arg TRANSFORMERS_MODEL=mattpuscibert
# --build-arg TRANSFORMERS_MODEL=batteryonlybert (currently disabled) 


# -------------------
# build builder image
# -------------------

FROM openjdk:8u342-jdk as builder

USER root

RUN apt-get update && \
    apt-get -y --no-install-recommends install apt-utils libxml2 git

RUN mkdir -p /opt/grobid-source/grobid-home/models

WORKDIR /opt/grobid-source
COPY gradle.properties .

RUN git clone --depth 1 --branch 0.7.2 https://github.com/kermitt2/grobid-quantities.git ./grobid-quantities &&  \
    cd grobid-quantities 

WORKDIR /opt/grobid-source/grobid-quantities
COPY gradle.properties .

WORKDIR /opt/grobid-source

RUN mkdir -p grobid-superconductors/resources/config grobid-superconductors/resources/models grobid-superconductors/gradle grobid-superconductors/localLibs grobid-superconductors/resources/web grobid-superconductors/src

COPY ./.git/ ./grobid-superconductors/.git
COPY resources/models/ ./grobid-superconductors/resources/models/
COPY resources/config/ ./grobid-superconductors/resources/config/
COPY gradle/ ./grobid-superconductors/gradle/
COPY src/ ./grobid-superconductors/src/
COPY localLibs/ ./grobid-superconductors/localLibs/
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
RUN ./gradlew downloadTransformers --no-daemon --info --stacktrace && rm -f /opt/grobid-source/grobid-home/models/*.zip


WORKDIR /opt

# -------------------
# build runtime image
# -------------------

FROM grobid/grobid:0.7.2 as runtime

# setting locale is likely useless but to be sure
ENV LANG C.UTF-8

# Install SO dependencies
RUN apt-get update && \
    apt-get -y --no-install-recommends install git wget

WORKDIR /opt/grobid

RUN mkdir -p /opt/grobid/grobid-superconductors
COPY --from=builder /opt/grobid-source/grobid-home/models ./grobid-home/models
COPY --from=builder /opt/grobid-source/grobid-superconductors/build/libs/* ./grobid-superconductors/
COPY --from=builder /opt/grobid-source/grobid-superconductors/resources/config/config.yml ./grobid-superconductors/

VOLUME ["/opt/grobid/grobid-home/tmp"]

RUN pip install -U git+https://github.com/kermitt2/delft.git

WORKDIR /opt/grobid

#RUN sed -i 's/pythonVirtualEnv:.*/pythonVirtualEnv: \/opt\/grobid\/venv/g' grobid-superconductors/config.yml
RUN sed -i 's/pythonVirtualEnv:.*/pythonVirtualEnv: /g' grobid-superconductors/config.yml
RUN sed -i 's/grobidHome:.*/grobidHome: grobid-home/g' grobid-superconductors/config.yml
RUN sed -i 's/chemDataExtractorUrl:.*/chemDataExtractorUrl: ${CDE_URL:- http:\/\/cde.local:8080}/g' grobid-superconductors/config.yml
RUN sed -i 's/linkingModuleUrl:.*/linkingModuleUrl: ${LINKING_MODULE_URL:- http:\/\/linking_module.local:8080}/g' grobid-superconductors/config.yml
RUN sed -i 's/classResolverUrl:.*/classResolverUrl: ${LINKING_MODULE_URL:- http:\/\/linking_module.local:8080}/g' grobid-superconductors/config.yml

## Select transformers model 
ARG TRANSFORMERS_MODEL

RUN if [[ -z "$TRANSFORMERS_MODEL" ]] ; then echo "Using Scibert as default transformer model" ; else rm -rf /opt/grobid/grobid-home/models/superconductors-BERT_CRF; mv /opt/grobid/grobid-home/models/superconductors-${TRANSFORMERS_MODEL}-BERT_CRF /opt/grobid/grobid-home/models/superconductors-BERT_CRF; rm -rf /opt/grobid/grobid-home/models/superconductors-*-BERT_CRF; fi

# JProfiler
#RUN wget https://download-gcdn.ej-technologies.com/jprofiler/jprofiler_linux_12_0_2.tar.gz -P /tmp/ && \
#  tar -xzf /tmp/jprofiler_linux_12_0_2.tar.gz -C /usr/local &&\
#  rm /tmp/jprofiler_linux_12_0_2.tar.gz

EXPOSE 8072 8073

ARG GROBID_VERSION
ENV GROBID_VERSION=${GROBID_VERSION:-latest}

RUN if [ ! -f "grobid-superconductors/grobid-superconductors-${GROBID_VERSION}-onejar.jar" ]; then mv grobid-superconductors/grobid-superconductors-*-onejar.jar grobid-superconductors/grobid-superconductors-${GROBID_VERSION}-onejar.jar; fi  

#RUN if [ "${!GROBID_VERSION}" = "unknown" ] ; then GROBID_VERSION=`ls grobid-superconductors/grobid-superconductors-*onejar.jar |  grep -oE '[0-9]\.[0-9]\.[0-9](-SNAPSHOT)?' | head -n 1`; fi

#CMD ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005", "-jar", "grobid-superconductors/grobid-superconductors-0.5.2-SNAPSHOT-onejar.jar", "server", "grobid-superconductors/config.yml"]
#CMD ["java", "-agentpath:/usr/local/jprofiler12.0.2/bin/linux-x64/libjprofilerti.so=port=8849", "-jar", "grobid-superconductors/grobid-superconductors-0.2.1-SNAPSHOT-onejar.jar", "server", "grobid-superconductors/config.yml"]
CMD ["sh", "-c", "java -jar grobid-superconductors/grobid-superconductors-${GROBID_VERSION}-onejar.jar server grobid-superconductors/config.yml"]


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

