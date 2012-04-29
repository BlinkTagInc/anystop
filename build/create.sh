#!/bin/bash

#Determines what Android API level we are targeting
#--target 11 specifies Google APIs level 14 (Android 4.0) on my LT31 laptop
#--target 14 specifies Google APIs level 14 (Android 4.0) on my T61 laptop
#you might have to run "android.bat list target" to determine what the appropriate number here should be
TARGET=19

#ANT generated binary name, may change with different ANT versions
UNSIGNED_APK_FILENAME="AnyStop-release-unsigned.apk"
#The filename of the signed APK
SIGNED_APK_FILENAME="AnyStop.apk"

#For calculating elapsed time
STARTTIME=$SECONDS

#Print some debug messages
echo "Shell Script got parameters:"
echo $1 #Agency tag
echo $2 #agency short name
echo $3 #Agency name
echo $4 #Agency prediction type
echo $5 #Agency table name
echo $6 #Agency (secondary) table name
echo $7 #Agency hybrid app status
echo $8 #Agency Long Name
echo $9 #agency state
echo ${10} #agency city
echo ${11} #latitude, in microdegrees
echo ${12} #longitude, in microdegrees
echo ${13} #build type

#Prep the AnyStop directory we will build
rm -rf ./AnyStop
mkdir AnyStop
#cp -a AnyStopBase/. AnyStop/
echo "Creating AnyStop source copy"
cp -R AnyStopBase/. AnyStop/
rm ./AnyStop/res/values/strings.xml
cp ./stringsBase.xml ./AnyStop/res/values/strings.xml

echo "Processing strings.xml and other ASCII files to create Agency-specific app"
#replace all instances of agencytoken with the specific Agency name
#this fixes all of the library/package names and makes them unique for each AnyStop app build
find ./AnyStop/ -type f -exec sed -i "s/agencytoken/$1/" {} \;
#We replace specific strings in the strings.xml file among other things
#This causes us to build the app for a particular agency
sed -i s/agencyShortToken/"$2"/ ./AnyStop/res/values/strings.xml
sed -i s/agencyNameToken/"$3"/ ./AnyStop/res/values/strings.xml
sed -i s/agencyTagToken/"$1"/ ./AnyStop/res/values/strings.xml
sed -i s/agencyRealTimeToken/"$4"/ ./AnyStop/res/values/strings.xml
sed -i s/agencyTableToken/"$5"/ ./AnyStop/res/values/strings.xml
sed -i s/agencyScheduleToken/"$6"/ ./AnyStop/res/values/strings.xml
sed -i s/agencyHybridToken/"$7"/ ./AnyStop/res/values/strings.xml
sed -i s/agencyLongToken/"$8"/ ./AnyStop/res/values/strings.xml
sed -i s/agencyLatToken/"${11}"/ ./AnyStop/res/values/strings.xml
sed -i s/agencyLonToken/"${12}"/ ./AnyStop/res/values/strings.xml

DAYMONTH=`date +%m%d`
VERNUM=" (${13}.$DAYMONTH)"
echo "VERNUM=$VERNUM"

#Deal with release version info in the app name
#If the release type is "release" we replace agencyBuildVersion with nothing
#Otherwise we replace it with ${13}
if [ "${13}" == "release" ]
then
sed -i s/agencyBuildVersion/""/ ./AnyStop/res/values/strings.xml
fi

if [ "${13}" != "release" ]
then
sed -i s/agencyBuildVersion/"$VERNUM"/ ./AnyStop/res/values/strings.xml
fi

#debugging output
#cat ./AnyStop/res/values/strings.xml

#Now we build the project!
#First, create the ANT scripts that we will use to build the project
echo "Running Android command line compiler to generate ANT scripts"
android.bat update project --name AnyStop --target $TARGET --path ./AnyStop

#Move some libraries around for ANT, and make sure that ANT doesn't try to refactor them
#Also android.bat update likes to corrupt libraries and images
#echo "Moving libraries into libs folder"
cd ./AnyStop
rm -r libs
mkdir libs
rm -r lib
cp ../AnyStopBase/lib/AdWhirl/AdWhirlSDK_Android_3.1.1.jar ./libs/AdWhirlSDK.jar
cp ../AnyStopBase/lib/FlurryAnalytics/*.jar ./libs
cp ../AnyStopBase/lib/GoogleAdMobAdsSdkAndroid-4.3.1/GoogleAdMobAdsSdk-4.3.1.jar ./libs/GoogleAdMobAdsSdk.jar
cp ../AnyStopBase/lib/Addience-2.2.1/sensedk.jar ./libs/sensedk.jar
cp ../AnyStopBase/lib/Addience-2.2.1/lib/JTAdTag.jar ./libs/JTAdTag.jar
cp ../AnyStopBase/lib/Addience-2.2.1/lib/mdotm-sdk-android.jar ./libs/mdotm-sdk-android.jar
cp ../AnyStopBase/lib/Addience-2.2.1/lib/MMAdView.jar ./libs/MMAdView.jar
rm -r res/drawable*
cp -r ../AnyStopBase/res/drawable* ./res

#Set up the background that we will use for the front page
#if we have a dedicated background in backgrounds, that is
#Formula for background file name is agency tag cat with "-l" for landscape, "-p" for portrait
cd ..
if [ -f "background/$1-l.png" ]
then
	echo "Detected existence of background file!"
	rm AnyStop/res/drawable/subwaymap.png
	cp "background/$1-l.png" AnyStop/res/drawable/subwaymap.png
else
	echo "Could not detect existence of background file!"
fi

if [ -f "background/$1-p.png" ]
then
	rm AnyStop/res/drawable/subwaymaplong.png
	cp "background/$1-p.png" AnyStop/res/drawable/subwaymaplong.png
fi

#Set up a guard so that ANT doesn't go around obfuscating the library code
#echo "Setting up ProGuard guards for library JAR files"
cd AnyStop
cat proguard.cfg ../libguard.txt > proguard.cfg.new
rm proguard.cfg
mv proguard.cfg.new proguard.cfg

#Sometimes we have trouble with an extra R.java sitting around from an old compile and ANT gets pissed off
rm -r gen/org/busbrothers/anystop/agencytoken
rm -r bin/classes/org/busbrothers/anystop/agencytoken

#Run the ANT release build script; clean first to delete generated files an old compile might have left behind
echo "Running ANT release build script"
ant clean
ant release
cd bin

#Sign the JAR with our keystore
echo "Signing and aligning generated APK"
jarsigner -verbose -keystore ../../busbrothers.keystore -storepass  "NOT_A_VALID_PASSWORD" $UNSIGNED_APK_FILENAME AnyStop
#run zipalign to dword-align the noncompressed stuff in the JAR, to optimize memory usage for Android apps or something
zipalign -v 4 $UNSIGNED_APK_FILENAME $SIGNED_APK_FILENAME

#move stuff to the out folder for easier publishing of the app
cd ..
cd ..
mkdir out
cd out
rm $1.apk
#mkdir $1
cd ..
#the below is the APK that we've generated, we will move it into ./out and call it
#<agency tag>.apk
cp ./AnyStop/bin/$SIGNED_APK_FILENAME ./out/$1.apk
#cp ./AnyStop/bin/$SIGNED_APK_FILENAME ./out/$1

#the below is the title icon for our app (removed 2012-04-11)
#cp ./title.png ./out/$1

#Generate the AnyStop market description text (removed 2012-04-11)
#cp ./copy ./out/$1
#sed -i s/agencyLongToken/"$6"/ ./out/$1/copy
#sed -i s/agencyShortToken/"$2"/ ./out/$1/copy
#sed -i s/cityToken/"$7"/ ./out/$1/copy
#sed -i s/stateToken/"$8"/ ./out/$1/copy
#echo copy:
#cat ./out/$1/copy

#Print out the elapsed time; don't actually have to do any matho because $SECONDS is zero when we start running this script!
ELAPSEDTIME=$SECONDS
echo "create.sh finished running, took $ELAPSEDTIME seconds"
