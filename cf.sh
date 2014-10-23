#!/bin/bash
set -e

APP_NAME=bootiful-vaadin-places
MONGO_SERVICE=$APP_NAME-mongodb


DOMAIN=${DOMAIN:-run.pivotal.io}
TARGET=api.${DOMAIN}

cf api | grep ${TARGET} || cf api ${TARGET} --skip-ssl-validation
cf a | grep OK || cf login
cf s | grep $MONGO_SERVICE || cf cs mongolab sandbox $MONGO_SERVICE
cf push $APP_NAME

APP_URI=`cf apps | grep $APP_NAME | tr -s ' ' | cut -d' ' -f 6`

cf delete-orphaned-routes
echo $APP_NAME has been deployed at $APP_URI.



