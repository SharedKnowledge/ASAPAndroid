#!/bin/bash

rm -f ASAPAndroid.aar
cd app/build/outputs/aar
jar -xf app-debug.aar
rm -rf libs
rm -f app-debug.aar
echo "could also change manifest file..."
jar -cf ASAPAndroid.aar *
mv ASAPAndroid.aar ../../../../ASAPAndroid.aar
cd ../../..