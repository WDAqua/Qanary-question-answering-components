#!/bin/bash

echo The port number is: $SERVER_PORT
echo The Qanary pipeline URL is: $SPRING_BOOT_ADMIN_URL
if [ -n $PORT ]
then
    exec gunicorn -b :$SERVER_PORT --access-logfile - --error-logfile - run:app
fi
