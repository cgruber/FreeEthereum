#!/usr/bin/env bash

mvn clean package -D skipTests

java -jar target/ethereumj-v1.1.0-SNAPSHOT-jar-with-dependencies.jar
