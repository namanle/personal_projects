#!/bin/bash -ex

mvn -q clean
mvn -q compile
mvn -q exec:exec -Dexec.mainClass=cs1302.api.ApiDriver
