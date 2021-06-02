#!/bin/bash

echo The port number is: $SERVICE_PORT
echo The Qanary pipeline URL is: $SPRING_BOOT_ADMIN_URL
if [ -n $SERVICE_PORT ]
then
    exec gunicorn -b :$SERVICE_PORT --access-logfile - --error-logfile - run:app
fi
