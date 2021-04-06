#!/bin/sh

cd functional-examples
./gradlew clean build
cd ../chain-of-command
./gradlew assemble"
