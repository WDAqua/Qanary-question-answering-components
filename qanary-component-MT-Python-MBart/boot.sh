#!/bin/sh

export $(grep -v '^#' .env | xargs)

echo SERVER_PORT: $SERVER_PORT
echo Qanary pipeline at SPRING_BOOT_ADMIN_URL: $SPRING_BOOT_ADMIN_URL

if [ -n $SERVER_PORT ]
then
    exec gunicorn -b :$SERVER_PORT --access-logfile - --error-logfile - run:app # refer to the gunicorn documentation for more options
fi
