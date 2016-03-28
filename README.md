AcDisplay
==========
[![Build Status](https://travis-ci.org/AChep/AcDisplay.svg?branch=master)](https://travis-ci.org/AChep/AcDisplay) [![Crowdin](https://d322cqt584bo4o.cloudfront.net/acdisplay/localized.png)](http://translate.acdisplay.org) [![Bountysource](https://www.bountysource.com/badge/team?team_id=40057&style=bounties_received)](http://bounty.acdisplay.org) [![Support at gratipay](http://img.shields.io/gratipay/AChep.svg)](https://gratipay.com/AChep/)

<img alt="Main screen: handling notification" align="right" height="300"
   src="https://github.com/AChep/AcDisplay/raw/master/screenshots/screenshot2.png" />
<img alt="Main screen" align="right" height="300"
   src="https://github.com/AChep/AcDisplay/raw/master/screenshots/screenshot1.png" />

*AcDisplay is a new way of handling notifications in Android.*

It will inform you about new notifications by showing a minimal, beautiful screen, allowing you to open them directly from the lock screen. And if you want to see what's going on, you can simply take your phone out of your pocket to view all the latest notifications, in a similarly pleasing and minimalistic manner.

 - **[Help us to translate it](http://translate.acdisplay.org)** _(even a short look would be helpful)_
 - **[Join app's community on Google+](http://community.acdisplay.org)** _(Project's news, random spamming and social stuff)_
 - **[Join app's dev channel on freenode](http://webchat.freenode.net?channels=acdisplay)** _(I'm always in to discuss the development process; join, if you're a developer that wants to help us)_
 - **[The bounty funding service](http://bounty.acdisplay.org)** _(put the bounties on the issues, so everyone is hightly motivated to fix them)_

<a href="http://get.acdisplay.org">
  <img alt="Get AcDisplay on Google Play" vspace="20"
       src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" height="60" />
</a> <a href="bitcoin:1GYj49ZnMByKj2f6p7r4f92GQi5pR6BSMz?amount=0.005">
  <img alt="Bitcoin wallet: 1GYj49ZnMByKj2f6p7r4f92GQi5pR6BSMz" vspace="28" hspace="20"
       src="https://github.com/AChep/AcDisplay/raw/master/art/btn_bitcoin.png" />
</a> <a href="http://goo.gl/UrecGo">
  <img alt="PayPal" vspace="28"
       src="https://github.com/AChep/AcDisplay/raw/master/art/btn_paypal.png" />
</a>

Report a bug or request a feature
----------------
Before creating a new issue please make sure that same or similar issue is not already created by checking [open issues][2] and [closed issues][3] *(please note that there might be multiple pages)*. If your issue is already there, don't create a new one, but leave a comment under already existing one.

Checklist for creating issues:

- Keep titles short but descriptive.
- For feature requests leave a clear description about the feature with examples where appropriate.
- For bug reports leave as much information as possible about your device, android version, etc.
- For bug reports also write steps to reproduce the issue.

[Create new issue][1]

Creating your AcDisplay
----------------
We welcome all developers to use our source code to create applications on our platform.
There are several things we require from **all developers** for the moment:

1. Please **do not** use the name AcDisplay for your app — or make sure your users understand that it is unofficial.
2. Kindly **do not** use our standard logo as your app's logo.
3. Please remember to read and follow the [license][4].

Versioning
----------------
For transparency in a release cycle and in striving to maintain backward compatibility, a project should be maintained under the Semantic Versioning guidelines. Sometimes we screw up, but we should adhere to these rules whenever possible.

Releases will be numbered with the following format: `<major>.<minor>.<patch>` and constructed with the following guidelines:
- Breaking backward compatibility bumps the major while resetting minor and patch
- New additions without breaking backward compatibility bumps the minor while resetting the patch
- Bug fixes and misc changes bumps only the patch

For more information on SemVer, please visit http://semver.org/.

Download & Build
----------------
Clone the project and come in:

``` bash
$ git clone git://github.com/AChep/AcDisplay.git
$ cd AcDisplay/project/
```

To build debug version: (only English and Russian locales included)

``` bash
$ ./gradlew assembleDebug
# Builds all the things. Grab compiled application from ./app/build/outputs/apk/
```

To build release version: (public test key)

``` bash
$ ./gradlew assembleRelease
# You will need to answer 'yes' later.
# Grab compiled application from ./app/build/outputs/apk/
```

To build release version:

``` bash
# First you need to set the path to your keystore and the alias.
# You may want to put those to your ~/.bashrc file to save them
# for future bash sessions.
$ export ACDISPLAY_SIGN_STORE_FILE=path_to_your_keystore
$ export ACDISPLAY_SIGN_KEY_ALIAS=key_alias

$ ./gradlew assembleRelease
# You will be asked for passwords in proccess.
# Grab compiled application from ./app/build/outputs/apk/
```

You may also use the Android Studio graphic interface to build.

Import to Android Studio
----------------
- Make sure JDK-7 or later is installed.
- Make sure latest Android Studio is installed.
- Launch Android Studio.
- Select: File -> Import project; and choose ./AcDisplay/project directory.
- Wait until it done.

[1]: https://github.com/AChep/AcDisplay/issues/new
[2]: https://github.com/AChep/AcDisplay/issues?state=open
[3]: https://github.com/AChep/AcDisplay/issues?state=closed
[4]: https://github.com/AChep/AcDisplay/blob/master/LICENSE
