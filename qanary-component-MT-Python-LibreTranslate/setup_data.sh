#!/bin/sh
# download libre repo
git clone --depth 1 https://github.com/LibreTranslate/LibreTranslate
echo starting libretranslate
echo python version: $(python --version)
cd LibreTranslate
python main.py
cd ..
#cd LibreTranslate
#pip install -e .
#docker build -f docker/Dockerfile --build-arg with_models=true -t libretranslate .
