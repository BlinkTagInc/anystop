# Anystop App

## Description

Anystop is an Android app that shows realtime and scheduled transit arrival information. It supports over 125 different agencies. Users can save favorite routes and stops, browse for stops by route and see when the next bus, train or ferry is scheduled to arrive.

## Documentation

Documentation is handled via commenting. Where relevant, commenting is done using Doxygen tags. To generate the HTML documentation, install Doxygen and Graphviz. Now, run doxygen from within the folder "Anystop\src\org\busbrothers\.". It should generate a doc\ folder in the same location. Then you can open the index.html file, and browse the documentation! 

## Building and Publishing the App

The preferred method of building the Android app for debugging and development is to use Eclipse. This is fairly easy to do; just import the Android project into Eclipse and run build. You will probably have to generate a valid Android Google Maps API Key for your specific machine; see the "maps_apik" string in strings.xml. Google "generate Android Maps API key" for instructions on how to generate this key (it is easy).

The build scripts are located under build/. There is a document describing how to set up and run the build script.

## License

This project is licensed under GNU General Public License v3.0.
