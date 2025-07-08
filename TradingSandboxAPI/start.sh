#!/usr/bin/env bash
# start.sh

# export all non-comment lines from .env
# shellcheck disable=SC2046
export $(grep -v '^\s*#' .env | xargs)

# then run
./mvnw spring-boot:run
