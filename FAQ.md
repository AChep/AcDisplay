AcDisplay User FAQ
=============

##### How can I join beta testing to take a look at future features?
>Join AcDisplay's [community] and click [this link][testing] (if it gives any errors, wait for a few minutes and try again.) Once that's done, either reinstall AcDisplay from the Play Store or wait until the update is pushed to your device (this can take a while.)
Please keep in mind that those features are unstable, and make sure to send bug reports (with a log attached) so that they can be fixed.
To do so, just use the feedback option and check «Attach a log» and the log will be automatically attached. Don't forget to include a detailed description of the bug and how to reproduce it.


##### What is active mode?
>Active mode uses the device's sensors to wake your device up when you need it. This works great if you want to pull your device from your pocket and get a quick glimpse at the notifications you've received. Active mode doesn't keep the CPU running so you shouldn't see a big difference in battery consumption.


##### How can I uninstall this app?
>Go to Settings > Security > Device Administrators and uncheck AcDisplay. You can now uninstall AcDisplay normally.


##### How can I hide the annoying «Welcome to Immersive mode» popup?
>Unfortunately, Android is the one showing that popup... If you know a workaround for that, please let us know - patches are also welcome! Xposed users can use AcDisplay's module as a workaround.


##### What a nice app! How can I donate?
>Oh, thank you! You can use [PayPal] or Bitcoins (_1GYj49ZnMByKj2f6p7r4f92GQi5pR6BSMz_) to donate. 😃


##### Why was the interface changed?
>In order to avoid legal trouble with a certain company, a redesign was necessary to make the two products distinct. You'll hopefully like the new design better, as it's more streamlined and is aimed at giving you the best possible experience!


##### How can I disable the permanent notifications when active mode or the lockscreen are enabled?
>The notification is necessary to ensure AcDisplay isn't killed by the system. You can, however, hide it if it annoys you. To do so, long press AcDisplay's notification, tap «App info» and uncheck «Show notification». Once this is done, all notification from AcDisplay will be hidden. As a side effect, the test notification option will not function anymore.


##### I want to see pattern / password lock in AcDisplay. Any chance to get it?
>Not a big one. AcDisplay supports any of Android's lock screens... Reinventing the wheel would be silly when you can simply use what Android already has, especially considering that it has been tested and improved ever since the beginning of Android, and it's more secure than anything an app can offer.


##### The blacklist isn't working! Or how do I use the blacklist?
>To use the blacklist, you must enable the app and configure it as well. To enable it, simply press the switch button. To configure it for the app, press the app's name and the configuration screen will open.

##### What's the different between «Hide» and «Silent» in the blacklist?
>«Hide» completely ignores the notification. «Silent» won't wake your device, but the notification will still be displayed if AcDisplay is shown somehow (lockscreen or another notifications).


##### I can see a typo / mistake in the translation!
>You can easily help out with that. Join the app's [page][translate] on crowdin and suggest a better translation. Thanks!


##### I am using the beta, why is setting X blacked out?
>Even though it is a beta, stability is still important. This is done because some features are just not ready to be made public yet. To not completely remove them, they are disabled.


##### When will AcDisplay be available for older devices/versions of Android?
>While it is understandable that you want the AcDisplay awsomeness on your own device, the truth is that it’s really difficult. Most of the features depend on programming APIs that were just released Android 4.4 KitKat. While nothing is impossible, for now the focus is on further improving AcDisplay for KitKat first. If you are a developer however and you want work on this or have something else in mind, don’t hesitate to fork the AcDisplay git [repo] and give it a whirl.


##### I see some Xposed modules are included, what do they do?
>Good that you ask that, there are currently two of them. Number one makes the annoying blue immersive mode popup go away and number two disables the homebutton when you have AcDisplay enabled as a lockscreen. These are both features that can't be done using conventional methods and therefore they are seperate from the app in an Xposed module.

[community]:https://plus.google.com/u/0/communities/102085470313050914854
[testing]:https://play.google.com/apps/testing/com.achep.activedisplay
[repo]:http://repo.acdisplay.org
[translate]:http://translate.acdisplay.org
[PayPal]:http://goo.gl/UrecGo
