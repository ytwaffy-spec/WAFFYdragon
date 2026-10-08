#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
paper_revision=a4b87cf896e18419005ab172e8b1694ccc1abe7c
brigadier_revision=9ba4f13c0fe82b07c08c2dc2d8043f075ffd0d98
mkdir -p .local/sources
curl --fail --location --retry 2 "https://codeload.github.com/PaperMC/Paper/zip/$paper_revision" -o .local/paper-source.zip
curl --fail --location --retry 2 "https://codeload.github.com/Mojang/brigadier/zip/$brigadier_revision" -o .local/brigadier-source.zip
unzip -q -o .local/paper-source.zip -d .local/sources
unzip -q -o .local/brigadier-source.zip -d .local/sources
./gradlew -p tools/api-build "-PpaperSource=$PWD/.local/sources/Paper-$paper_revision" "-PbrigadierSource=$PWD/.local/sources/brigadier-$brigadier_revision" jar
cp tools/api-build/build/libs/paper-api-source-build.jar .local/paper-api-source-build.jar
./gradlew clean build
