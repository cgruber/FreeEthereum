#!/usr/bin/env bash

./gradlew clean shadowJar
java -Xss2M -jar free-ethereum-core/build/libs/free-ethereum-core-*-all.jar
