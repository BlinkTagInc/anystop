"""This file is used to generate and sign the release build of AnyStop Android apps. It can be used to generate the app for a specific agency or just to generate all agency apps. The first (and only) command line parameter specified whichagency to generate an app for. If the parameter is \"all\" then we generate an app for all of the agencies."""

import urllib
import subprocess
import sys

#One of the below two defines should be uncommented; the correct one varies between different python versions & libraries
#import elementtree.ElementTree as ET
import xml.etree.ElementTree as ET

if(len(sys.argv) != 3):
	print "Error: Invalid number of parameters. Should be two parameters, the first specifying the type of build (alpha/beta/rc/release) and specifying the Agency to build an APK for (or \"all\" to build APKs for all Agencies."
	sys.exit(1)
if "help" in sys.argv[1]:
	print "Error: Invalid number of parameters. Should be two parameters, the first specifying the type of build (alpha/beta/rc/release) and specifying the Agency to build an APK for (or \"all\" to build APKs for all Agencies."
	sys.exit(1)

print ("Going to run AnyStop build script for Agency (Tag): %s" % sys.argv[2])

#Here we fetch the agency data, including the tags, table names, and agency lat/lon locations from the AnyStop server
#This is temporarily changed to another XML file hosted on Ivan's server
#was http://feed.busbrothers.org/ClosestStops/XMLAgencyList.jsp
url = "http://feed.busbrothers.org/ClosestStops/XMLAgencyList.jsp"
#url = "http://yulaev.com/~ivany/temp/XMLAgencyList.jsp.xml"
etree = ET.parse(urllib.urlopen(url))
iter = etree.getiterator('agencyDetails')

#We parse the agency data line-by-line, where each line represents agency
for elem in iter:
#Extract all of the 
 children = elem.getchildren()
 agencyName = children[0].text.strip()
 longAgencyName = children[1].text.strip()
 shortAgencyName = children[2].text.strip()
 agencyTag = children[3].text.strip()
 predictionType = children[4].text.strip()
 tableName = children[5].text.strip()
 alternateTableName = children[6].text.strip()
 isHybridAgency = children[7].text.strip()
 city = children[8].text.strip()
 state = children[9].text.strip()
 
 #convert lat, lon strings to integers (in microdegrees)
 latstr = children[10].text.strip()
 lat = int(float(latstr)*1000000)
 lonstr = children[11].text.strip()
 lon = int(float(lonstr)*1000000)
 
 #determine whether or not to generate an APK for this agency tag, based on arguments that this script got
 if agencyTag!=sys.argv[2] and sys.argv[2]!='all':
  continue
  
 # uncomment the below three lines if you want to only generated schedule-based apps
 # if predictionType!='schedule':
	# print ("Skipping %s since it is a real-time app!" % agencyTag)
	# continue
 
 #if we are creating the APK, put together the command line string with command line arguments for the build script
 catstr = "\"" + agencyTag + "\" \"" + shortAgencyName + "\" \"" + agencyName + "\" \"" + predictionType + "\" \"" + tableName + "\" \"" + alternateTableName + "\" \"" + isHybridAgency + "\" \"" + longAgencyName + "\" \"" + city + "\" " + "\"" + state + "\"" + " " + str(lat) + " " + str(lon) + " \"" + str(sys.argv[1]) + "\""
 try :
 #finally put together command line string for script and run create.sh
  execst = 'sh create.sh ' + catstr
  print ("DEBUG: Running shell script with syntax: %s\n" % execst)
  #can't test this, but subprocess.Popen is what you want and wait() makes it wait for completion
  subprocess.Popen(execst, shell=True).wait()
 except :
  print "python exception"
  
 print "parseexec finished running"
