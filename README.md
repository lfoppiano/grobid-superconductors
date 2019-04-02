# grobid-superconductors

[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

__Work in progress.__

[TBD] 
The goal of this GROBID module is to recognize in textual documents any superconductors names, acronyms, etc... 

We focus our work on technical and scientific articles (text, XML and PDF input) and (maybe) patents (text and XML input). 

## Documentation

### Getting started

> install grobid http://github.com/kermitt2/grobid 

> clone the grobid-superconductor repository inside the grobid directory 
> git clone ....

> git submodule init

> git submodule update 

### Build

> ./gradlew clean build 

### Run
To run the service: 

> java -jar build/libs/grobid-superconductor-{version}.onejar.jar server config/config.yml 

To run the IAA (Inter Annotators Agreement) measurements: 

> java -jar build/libs/grobid-superconductor-{version}.onejar.jar iaa -dIn baseDirectory config/config.yml 

for example: 
> java -jar build/libs/grobid-superconductor-{version}.onejar.jar iaa -dIn baseDirectory resources/dataset/superconductors/guidelines/annotated

## License

GROBID and grobid-superconductors are distributed under [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0). 

Contact: Luca Foppiano (<FOPPIANO.Luca@nims.go.jp>)
