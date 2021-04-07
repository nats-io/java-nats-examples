#!/bin/sh

cd functional-examples
sudo chmod 777 gradlew
./gradlew clean build
cd ../chain-of-command
sudo chmod 777 gradlew
./gradlew clean build
