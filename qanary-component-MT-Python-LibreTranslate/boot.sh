#!/bin/sh

export $(grep -v '^#' .env | xargs)

# start libretranslate server
echo starting libretranslate
echo python version: $(python --version)
#cd LibreTranslate
#python main.py
#cd ..
#nohup sh -c "libretranslate &" && sleep 6
#docker run -p 6120:5000 libretranslate:latest




echo The port number is: $SERVER_PORT
echo The Qanary pipeline URL is: $SPRING_BOOT_ADMIN_URL
if [ -n $SERVER_PORT ]
then
    exec gunicorn -b :$SERVER_PORT --access-logfile - --error-logfile - run:app # refer to the gunicorn documentation for more options
fi


