carmen
======

Geolocation for Twitter.

Carmen is a library for geolocating tweets. Given a tweet, Carmen will return
Location objects that represent a physical location. Carmen uses both
coordinates and other information in a tweet to make geolocation decisions.
It's not perfect, but this greatly increases the number of geolocated tweets
over what Twitter provides.

To compile:
ant build

To create a new jar file (one already exists in the dist directory)
ant jar

To clean:
ant clean

The properties for controlling Carmen's behavior are location in:
src/resources/carmen.properties

These parameters are explained in the javadocs for carmen.LocationResolver.

The paths in the properties file are currently relative paths. They should be updated
to absolute paths as needed.

To run a demo:
ant run-demo -Dargs='--input_file input.json'

You can provide a second argument to output the tweets with geolocation information to a file.

ant run-demo -Dargs='--input_file input.json --output_file output.json'

To run the experiments described in the Carmen paper (below).
ant run-stats-demo -Dargs='--input_file input.json --output_file output.json'

input.json and output.json are both json files. input.json should contain tweets in json
format, one per line. Twitter data is not distributed with Carmen. These files will be treated
a gzip files if the have suffix ".gz"



----------------------------------------------------------------------
Dependencies:
For convenience, the lib directory contains copies of jar files required by Carmen.
You can obtain updated versions of these jar files from the corresponding project website.
Jackson (databind and core)     		http://wiki.fasterxml.com/JacksonHome
Apache log4j                    		http://logging.apache.org/log4j/
SimpleLatLng                	    	https://code.google.com/p/simplelatlng/
Apache Commons Command Line Interface	http://commons.apache.org/proper/commons-cli/
----------------------------------------------------------------------
To reference this package in publications:

@inproceedings{Dredze:2013a,
	Author = {Mark Dredze and Michael J Paul and Shane Bergsma and Hieu Tran},
	Booktitle = {AAAI Workshop on Expanding the Boundaries of Health Informatics Using AI (HIAI)},
	Title = {Carmen: A Twitter Geolocation System with Applications to Public Health},
	Year = {2013},
}

README.md	  : this file
LICENSE	      : 2-clause BSD license
lib/          : dependencies
src/	      : source directory
