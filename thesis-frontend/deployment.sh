#!/usr/bin/env bash

INSTANCE=
KEYFILE=/Users/pmartyn/Documents/College_Work/Thesis/Key/NIIPredictorApp.pem



scp -i ${KEYFILE} /Users/pmartyn/Documents/PythonDist/src.zip ubuntu@${INSTANCE}:~
scp -i ${KEYFILE} /Users/pmartyn/Documents/College_Work/Thesis/Thesis-Frontend/thesis-frontend/target/thesis-frontend-0.1.0-standalone.jar ubuntu@${INSTANCE}:~

ssh -i ${KEYFILE} ubuntu@${INSTANCE}

