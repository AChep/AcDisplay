AcDisplay
==========

<img alt="Download from Google Play" align="right" height="300"
   src="https://github.com/AChep/AcDisplay/raw/master/screenshots/screenshot2.png" />
<img alt="Download from Google Play" align="right" height="300"
   src="https://github.com/AChep/AcDisplay/raw/master/screenshots/screenshot1.png" />

*AcDisplay is a new way of handling notifications in Android.*

It will let you know about new notifications by showing a minimal, beautiful screen, allowing you to open them directly from the lock screen. And if you want to see what's going on, you can simply take your phone out of your pocket to view all the latest notifications, in a similarly pleasing and minimalistic manner.

**[Join app's community on Google+](https://plus.google.com/u/0/communities/102085470313050914854)**

<a href="http://get.acdisplay.artemchep.com">
  <img alt="Get AcDisplay on Google Play" vspace="20"
       src="https://github.com/AChep/AcDisplay/raw/master/art/google_play.png" />
</a>

Download & Build
================
Clone the project and come in:

         $ git clone git://github.com/AChep/AcDisplay.git
         $ cd AcDisplay/

If you want to build AcDisplay with additional languges (not only English and Russian), you can download them by running the following line: _by default tranlations will appear only in **Release** build._

         $ python ./AcDisplay/update_translations.py
         # Downloads latest build of locales from crowdin.net/project/acdisplay

To build debug version:

         $ ./gradlew aD
         # Builds all the things. Grab compiled application from ./AcDisplay/build/apk/

To build release version:

         # First you need to set the path to your keystore and the alias.
         # This's required only once.
         $ export ACDISPLAY_SIGN_STORE_FILE=path_to_your_keystore
         $ export ACDISPLAY_SIGN_KEY_ALIAS=key_alias
         
         $ ./gradlew aR
         # You will be asked for passwords in proccess.
         # Grab compiled application from ./AcDisplay/build/apk/

You may also use the Android Studio to build.
