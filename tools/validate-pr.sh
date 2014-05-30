#!/bin/sh

set +x

TRAVIS_SCALA_VERSION_ARG=$1

if [ $TRAVIS_PULL_REQUEST = "false" ];
then
	echo "Testing before publishing the snapshot"
	sbt $TRAVIS_SCALA_VERSION_ARG test
	if [ $? -eq 0 ]; then
		echo "Publishing snapshot..."
		sbt $TRAVIS_SCALA_VERSION_ARG scalameter-core/publish publish
	else
		echo "Tests failed -- no snapshot will be published!"
	fi
else
	echo "Pull request -- testing without publishing the snapshot."
	sbt $TRAVIS_SCALA_VERSION_ARG test
fi
