BlueCtrl
========

**REQUIRES ROOT PERMISSIONS**

BlueCtrl is an open source Bluetooth input device emulator that can control remote devices. This means that you can use the touchscreen and keyboard of your Android device to control other devices which support Bluetooth.

Because this app is using the Bluetooth input standard, it supports various operating systems without any special server software. It has been successfully tested with the following systems:

* Android
* iOS
* Linux (Fedora, Ubuntu...)
* Mac OS X
* Playstation 3

Microsoft Windows however is one of the operating systems that on most devices won't work. The reason for that is a conflict with the Android Bluetooth input service which is active on most Android systems.



Build Requirements
------------------

* Android SDK
* Android NDK
* Apache Ant



Build Preparations
------------------

The build system must know where it can find the Android SDK and NDK. Therefor copy the `local.properties.example` file,  name it `local.properties` and adjust the content of the file.

The next step is to add the required BlueZ library file and the associated header files to the Android NDK. This is necessary because the NDK stable API doesn't include BlueZ.

The easiest way to get the library file is to extract it from an existing Android device with the following command:

    adb pull /system/lib/libbluetooth.so

After that put the extracted `libbluetooth.so` file to the following directory inside the Android NDK:

    platforms/android-14/arch-arm/usr/lib

Now you must get the header files for the bluetooth and cutils includes. You can download them from the following two repositories from the CyanogenMod project:

  https://github.com/CyanogenMod/android_external_bluetooth_bluez  
  https://github.com/CyanogenMod/android_system_core  

Then copy the directory `lib/bluetooth` from the Android BlueZ code and the directory `include/cutils` from the Android System Core code to the following directory inside the Android NDK:

    platforms/android-14/arch-arm/usr/include/



Building
--------

Open a command-line and navigate to the root of your project directory. Then execute either `ant debug` or `ant release` to create the desired .apk file inside the projects `bin/` directory. If you use the release target, you also have to sign the .apk and then align it with `zipalign`.
