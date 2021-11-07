#!/bin/bash

echo The port number is: $SERVICE_PORT
echo The Qanary pipeline URL is: $SPRING_BOOT_ADMIN_URL
if [ -n $SERVICE_PORT ]
then
    if [ -f "$SERVER_SSL_CERTIFICATE" ] && [ -f "$SERVER_SSL_KEY" ]
    then
        echo Running with SSL configuration
        exec gunicorn -b :$SERVICE_PORT --certfile $SERVER_SSL_CERTIFICATE --keyfile $SERVER_SSL_KEY --access-logfile - --error-logfile - run:app
    else
        exec gunicorn -b :$SERVICE_PORT --access-logfile - --error-logfile - run:app
    fi
fi
