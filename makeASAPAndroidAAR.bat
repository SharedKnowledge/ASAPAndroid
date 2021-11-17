del /F /Q ASAPAndroid.aar
cd app/build/outputs/aar
jar -xf app-debug.aar
del /S /F /Q libs
del /F /Q app-debug.aar
echo could also change manifest file...
jar -cf ASAPAndroid.aar *
move ASAPAndroid.aar ../../../../ASAPAndroid.aar
cd ./../../../