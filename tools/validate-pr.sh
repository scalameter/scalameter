#!/bin/sh

set +x

TRAVIS_SCALA_VERSION_ARG=$1

if [[ $TRAVIS_PULL_REQUEST == "false" ]];
then
	sbt test
	if [ $? -eq 0 ]; then
		sbt scalameter-core/publish publish
	else
		echo "Tests failed!"
	fi
else
	sbt $TRAVIS_SCALA_VERSION_ARG test
fi
