#!/usr/bin/env bash

export MAVEN_OPTS=-Xss128m
mvn clean install -D skipTests