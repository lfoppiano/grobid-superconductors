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

## Grobid 0.9.1 artifacts are compiled for Java 21, so the builder needs a JDK 21 toolchain.
## Pin to -noble (Ubuntu 24.04, like grobid's Dockerfile.delft): the unsuffixed tag floats to
## the newest Ubuntu where apt renamed libxml2 (t64 transition) and the install fails.
FROM eclipse-temurin:21-jdk-noble as builder

USER root

RUN apt-get update && \
    apt-get -y --no-install-recommends install \
        apt-utils libxml2 unzip ca-certificates curl git git-lfs && \
    rm -rf /var/lib/apt/lists/*

# Install git-xet so `git clone` can fetch the large model files stored via Xet on the
# HuggingFace Hub (https://hf.co/docs/hub/git-xet); git-lfs alone leaves them as pointers.
RUN curl --proto '=https' --tlsv1.2 -sSf \
        https://raw.githubusercontent.com/huggingface/xet-core/refs/heads/main/git_xet/install.sh | sh \
    && git xet install

WORKDIR /opt/grobid-source

RUN mkdir -p grobid-home/models \
    && mkdir -p grobid-superconductors_source/resources/config grobid-superconductors_source/resources/models grobid-superconductors_source/gradle grobid-superconductors_source/localLibs grobid-superconductors_source/resources/web grobid-superconductors_source/src

COPY resources/models/ ./grobid-superconductors_source/resources/models/
COPY resources/config/ ./grobid-superconductors_source/resources/config/
COPY gradle/ ./grobid-superconductors_source/gradle/
COPY src/ ./grobid-superconductors_source/src/
COPY localLibs/ ./grobid-superconductors_source/localLibs/
COPY ["gradlew*", "build.gradle", "settings.gradle", "gradle.properties", "./grobid-superconductors_source/"]
COPY .git/ ./grobid-superconductors_source/.git/

# Preparing models
WORKDIR /opt/grobid-source/grobid-superconductors_source
RUN git remote prune origin && git repack && git prune-packed && git reflog expire --expire=1.day.ago && git gc --aggressive \
    && git-lfs install \
    && ./gradlew installModels --no-daemon --info --stacktrace \
    && rm -f /opt/grobid-source/grobid-home/models/*.zip \
    && rm -rf /opt/grobid-source/grobid-home/models/*.-with_ELMo \
#    && rm -rf /opt/grobid-source/grobid-home/models/entityLinker* \
    && rm -rf /opt/grobid/grobid-home/models/data_models_quantities \
    && rm -rf /opt/grobid/grobid-home/models/data_models_superconductors \
    && ./gradlew clean assemble -x shadowJar --no-daemon  --stacktrace --info \
    && unzip -o build/distributions/grobid-superconductors-*.zip -d ../grobid-superconductors_distribution \
    && mv ../grobid-superconductors_distribution/grobid-superconductors-* ../grobid-superconductors \
    && rm -rf ../grobid-superconductors_distribution \
    && rm -rf /opt/grobid-source/grobid-superconductors_source/.git


# -------------------
# build runtime image
# -------------------

## Runtime base: the grobid-quantities develop image (built from its feature/grobid-0.9.1 /
## develop line), which supplies the Python/DeLFT/JEP stack and the JDK 21 runtime for Grobid 0.9.1.
FROM lfoppiano/grobid-quantities:latest-develop as runtime

# setting locale is likely useless but to be sure
ENV LANG C.UTF-8

WORKDIR /opt/grobid

RUN rm -rf /opt/grobid/grobid-quantities \
    && rm /opt/grobid/resources \
    && mkdir -p /opt/grobid/grobid-superconductors \
    && rm -rf /opt/grobid/grobid-home/models/*.-with_ELMo \
    && rm -rf /opt/grobid/grobid-service

COPY --from=builder /opt/grobid-source/grobid-home/models ./grobid-home/models
COPY --from=builder /opt/grobid-source/grobid-superconductors ./grobid-superconductors/
COPY --from=builder /opt/grobid-source/grobid-superconductors_source/resources/config/config-docker.yml ./grobid-superconductors/resources/config/config.yml

VOLUME ["/opt/grobid/grobid-home/tmp"]

WORKDIR /opt/grobid

## Select transformers model 
ARG TRANSFORMERS_MODEL

# POSIX test (single brackets): the runtime base's /bin/sh is dash, where the bash-only
# `[[ ]]` fails and would fall through to the else branch, deleting superconductors-BERT_CRF
# on the default (no TRANSFORMERS_MODEL) build.
RUN if [ -z "$TRANSFORMERS_MODEL" ] ; then echo "Using Scibert as default transformer model" ; else rm -rf /opt/grobid/grobid-home/models/superconductors-BERT_CRF; mv /opt/grobid/grobid-home/models/superconductors-${TRANSFORMERS_MODEL}-BERT_CRF /opt/grobid/grobid-home/models/superconductors-BERT_CRF; rm -rf /opt/grobid/grobid-home/models/superconductors-*-BERT_CRF; fi

# JProfiler
#RUN wget https://download-gcdn.ej-technologies.com/jprofiler/jprofiler_linux_12_0_2.tar.gz -P /tmp/ && \
#  tar -xzf /tmp/jprofiler_linux_12_0_2.tar.gz -C /usr/local &&\
#  rm /tmp/jprofiler_linux_12_0_2.tar.gz

WORKDIR /opt/grobid
ARG GROBID_VERSION
ENV GROBID_VERSION=${GROBID_VERSION:-latest}
# -agentpath:/usr/local/jprofiler12.0.2/bin/linux-x64/libjprofilerti.so=port=8849
ENV GROBID_SUPERCONDUCTORS_OPTS="-Djava.library.path=/opt/grobid/grobid-home/lib/lin-64:/usr/local/lib/python3.11/dist-packages/jep --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED"
ENV LINKING_MODULE_URL "http://linking_module.local:8080"
ENV CDE_URL "http://cde.local:8080"

EXPOSE 8072 8073

# RUN if [ ! -f "grobid-superconductors/grobid-superconductors-${GROBID_VERSION}-onejar.jar" ]; then mv grobid-superconductors/grobid-superconductors-*-onejar.jar grobid-superconductors/grobid-superconductors-${GROBID_VERSION}-onejar.jar; fi  

#RUN if [ "${!GROBID_VERSION}" = "unknown" ] ; then GROBID_VERSION=`ls grobid-superconductors/grobid-superconductors-*onejar.jar |  grep -oE '[0-9]\.[0-9]\.[0-9](-SNAPSHOT)?' | head -n 1`; fi

#CMD ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005", "-jar", "grobid-superconductors/grobid-superconductors-0.5.2-SNAPSHOT-onejar.jar", "server", "grobid-superconductors/config.yml"]
#CMD ["java", "-agentpath:/usr/local/jprofiler12.0.2/bin/linux-x64/libjprofilerti.so=port=8849", "-jar", "grobid-superconductors/grobid-superconductors-0.2.1-SNAPSHOT-onejar.jar", "server", "grobid-superconductors/config.yml"]
#CMD ["sh", "-c", "java -jar grobid-superconductors/grobid-superconductors-${GROBID_VERSION}-onejar.jar server grobid-superconductors/config.yml"]

CMD ["./grobid-superconductors/bin/grobid-superconductors", "server", "grobid-superconductors/resources/config/config.yml"]


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

