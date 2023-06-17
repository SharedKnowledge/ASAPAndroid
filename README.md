## build ASAPAndroid library

1. edit `app/build.gradle`
  - make sure `apply plugin` is set to `com.android.library`
  - comment out the line `applicationId "net.sharksystem.asap.example"`
2. build the library (Build -> Make Project)
3. run the `makeASAPAndroidARR.sh` script to create the ASAPAndroid.aar