#!/usr/bin/env bash

./gradlew clean shadowJar
java -Xss2M -jar ethereumj-core/build/libs/ethereumj-core-*-all.jar
