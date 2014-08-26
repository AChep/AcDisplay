AcDisplay
==========
[![Build Status](https://travis-ci.org/AChep/AcDisplay.svg?branch=master)](https://travis-ci.org/AChep/AcDisplay) [![Crowdin](https://d322cqt584bo4o.cloudfront.net/acdisplay/localized.png)](http://translate.acdisplay.org)

<img alt="Download from Google Play" align="right" height="300"
   src="https://github.com/AChep/AcDisplay/raw/master/screenshots/screenshot2.png" />
<img alt="Download from Google Play" align="right" height="300"
   src="https://github.com/AChep/AcDisplay/raw/master/screenshots/screenshot1.png" />

*AcDisplay is a new way of handling notifications in Android.*

It will inform you about new notifications by showing a minimal, beautiful screen, allowing you to open them directly from the lock screen. And if you want to see what's going on, you can simply take your phone out of your pocket to view all the latest notifications, in a similarly pleasing and minimalistic manner.

**[Join app's community on Google+](https://plus.google.com/u/0/communities/102085470313050914854)**
**[Join app's dev channel on freenode](http://webchat.freenode.net?channels=acdisplay)**

<a href="http://get.acdisplay.org">
  <img alt="Get AcDisplay on Google Play" vspace="20"
       src="http://developer.android.com/images/brand/en_generic_rgb_wo_60.png" />
</a>

Download & Build
----------------
Clone the project and come in:

         $ git clone git://github.com/AChep/AcDisplay.git
         $ cd AcDisplay/project/
         
To build debug version: (only English and Russian locales included)

         $ ./gradlew assembleDebug
         # Builds all the things. Grab compiled application from ./app/build/outputs/apk/

To build release version: (public test key)

         $ ./gradlew assembleRelease
         # You will need to answer 'yes' later.
         # Grab compiled application from ./app/build/outputs/apk/

To build release version:

         # First you need to set the path to your keystore and the alias.
         # You may want to put those to your ~/.bashrc file to save them
         # for future bash sessions.
         $ export ACDISPLAY_SIGN_STORE_FILE=path_to_your_keystore
         $ export ACDISPLAY_SIGN_KEY_ALIAS=key_alias
         
         $ ./gradlew assembleRelease
         # You will be asked for passwords in proccess.
         # Grab compiled application from ./app/build/outputs/apk/

You may also use the Android Studio graphic interface to build.

Import to Android Studio
----------------
- Make sure JDK-7 or later is installed.
- Make sure latest Android Studio is installed.
- Launch Android Studio.
- Select: File -> Import project; and choose ./AcDisplay/project directory.
- Wait until it done.
