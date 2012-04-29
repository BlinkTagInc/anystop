Millennial Media
Android Advertising SDK Sample App
Version 4.5.0
10/11/2011

This sample app demonstrates how to integrate the Millennial Media Android SDK into a simple 
picture browsing and manipulation app.  In this sample you will see a basic integration of an 
MMAdView being created entirely in an XML layout file.  You will also see another activity that 
demonstrates an MMAdView being created entirely programmatically, and sample code for all of the 
advanced features of the SDK including: setting meta-values about demographics, creating an ad 
listener that implements callbacks for ad events, interstitial ads, and conversion tracking.

Compiling and Running the App
-------------------------------------

1. Copy the MMAdView.jar file into the libs subdirectory of the SampleApp.

2. If you are using Eclipse do the following:
	a. Copy the SampleApp project directory into your workspace directory.
	b. The SampleApp should automatically build.
		If you run into problems check the following:
		- Make sure your Android SDK is setup. Check to see if the path is set under Window->Preferences->Android.
		- Try fixing the project by right clicking on SampleApp in the Project Explorer and then selecting Android Tools->Fix Project Properties
	c. Right click on SampleApp in the Project Explorer and choose Run As->Android Application.

3. If you are using the command line instead do the following:
	a. Change your current directory to SampleApp.
	b. Create the local.properties build file by running "android update project -p ."
	c. Compile the app and install it by running "ant install"

4. Find the SampleApp app on your emulator or device and run it!

