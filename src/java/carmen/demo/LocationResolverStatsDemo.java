// Copyright 2012-2013 Mark Dredze. All rights reserved.
// This software is released under the 2-clause BSD license.
// Mark Dredze, mdredze@cs.jhu.edu
package carmen.demo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.cli.Option;
import org.apache.log4j.Logger;

import carmen.LocationResolver;
import carmen.types.Constants;
import carmen.types.Location;
import carmen.types.ResolutionMethod;
import carmen.utils.CommandLineUtilities;
import carmen.utils.Timer;
import carmen.utils.Utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javadocmd.simplelatlng.LatLng;

/**
 * A demo of the LocationResolver that computes statistics about the locations in the given tweets.
 * The geolocated tweets are written out to a new file.
 * This class was used for the experiments published in the Carmen paper.
 * @author Mark Dredze mdredze@cs.jhu.edu
 *
 */
public class LocationResolverStatsDemo {

	protected static List<Option> options = new LinkedList<Option>();
	protected static Logger logger = Logger.getLogger(LocationResolverStatsDemo.class);
	protected LocationResolver _locationResolver;

	public static void main(String[] args) throws ParseException, FileNotFoundException, IOException, ClassNotFoundException
	{
		// Parse the command line.
		String[] manditory_args = { "input_file" };
		createCommandLineOptions();
		CommandLineUtilities.initCommandLineParameters(args, LocationResolverStatsDemo.options, manditory_args);

		// Get options
		String inputFile = CommandLineUtilities.getOptionValue("input_file");
		String outputFile = null;
		if (CommandLineUtilities.hasArg("output_file")) {
			outputFile = CommandLineUtilities.getOptionValue("output_file");
		}

		Timer timer = new Timer();
		timer.start();
		LocationResolverStatsDemo tester = new LocationResolverStatsDemo();

		tester.run(inputFile, outputFile);
		timer.stop();

		logger.info("Done. " + timer.getFullTime());


	}

	public LocationResolverStatsDemo() throws IOException, ClassNotFoundException {
		this._locationResolver = LocationResolver.getLocationResolver();
		this._locationResolver.setUseUnknownPlaces(false);
	}

	private void run(String inputFile, String outputFile) throws FileNotFoundException, IOException {
		Writer output = null;
		if (outputFile != null)
			output = Utils.createWriter(outputFile);

		HashMap<ResolutionMethod, Integer> resolutionMethodCounts = new HashMap<ResolutionMethod, Integer>();
		
		int numCity = 0;
		int numCounty = 0;
		int numState = 0;
		int numCountry = 0;
		
		int hasPlace = 0;
		int hasCoordinate = 0;
		int hasCoordinate2 = 0;
		int hasGeo = 0;
		int hasUserProfile = 0;
				
		Scanner scanner = Utils.createScanner(inputFile);
		
		ObjectMapper mapper = new ObjectMapper();
		int numResolved = 0;
		int total = 0;
		int skipped = 0;
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			
			HashMap<String, Object> tweet = null;

			try {
				@SuppressWarnings("unchecked")
				HashMap<String, Object> readValue = (HashMap<String, Object>) mapper.readValue(line, Map.class);
				tweet = readValue;
			} catch (com.fasterxml.jackson.core.JsonParseException exception) {
				logger.warn("Skipping bad tweet: " + line);
				skipped++;
				continue;
			} catch (com.fasterxml.jackson.databind.JsonMappingException exception) {
				logger.warn("Skipping bad tweet: " + line);
				skipped++;
				continue;
			}
			Map<String, Object> place = Utils.getPlaceFromTweet(tweet);
			if (place != null && place.size() > 0)
				hasPlace++;
			LatLng latLng = Utils.getLatLngFromTweet(tweet);
			if (latLng != null)
				hasCoordinate++;
			if (tweet.get("coordinates") != null)
				hasCoordinate2++;
			if (tweet.get("geo") != null)
				hasGeo++;
			
			String tweet_location = Utils.getLocationFromTweet(tweet);
			if (tweet_location != null && tweet_location.length() != 0) {
				hasUserProfile++;
			}
			
			total++;

			if (total % 10000 == 0) {
				logger.info(total + "\r");
			}
			Location resolvedLocation = this._locationResolver.resolveLocationFromTweet(tweet);

			if (resolvedLocation != null && !resolvedLocation.isNone()) {
				tweet.put(Constants.TWEET_USER_LOCATION, Location.createJsonFromLocation(resolvedLocation));
				
				numResolved++;
				ResolutionMethod resolutionMethod = resolvedLocation.getResolutionMethod();
				if (!resolutionMethodCounts.containsKey(resolutionMethod))
					resolutionMethodCounts.put(resolutionMethod, 0);
				resolutionMethodCounts.put(resolutionMethod, resolutionMethodCounts.get(resolutionMethod) + 1);
			
				
				// What resolution is this location?
				if (resolvedLocation.getCity() != null) {
					numCity++;
				} else if (resolvedLocation.getCounty() != null) {
					numCounty++;
				} else if (resolvedLocation.getState() != null) {
					numState++;
				} else if (resolvedLocation.getCountry() != null) {
					numCountry++;
				}	
			}
			if (output != null) {
				String outputString = mapper.writeValueAsString(tweet);
				output.write(outputString);
				output.write("\n");
			}

		}
		if (output != null)
			output.close();
		
		scanner.close();
		logger.info("Total: " + total);
		logger.info("Resolved: " + numResolved);
		logger.info("Skipped (not included in total): " + skipped);
		
		logger.info("Has Place:" + hasPlace);
		logger.info("Has Coordinate: " + hasCoordinate);
		logger.info("Has Coordinate2: " + hasCoordinate2);
		logger.info("Has UserProfile: " + hasUserProfile);
		logger.info("Has Geo: " + hasGeo);
		
		logger.info("Num city: " + numCity);
		logger.info("Num county: " + numCounty);
		logger.info("Num state: " + numState);
		logger.info("Num country: " + numCountry);
		
		for (ResolutionMethod method : resolutionMethodCounts.keySet()) {
			int count = resolutionMethodCounts.get(method);
			logger.info(method + "\t" + count);
		}
		
	}

	private static void createCommandLineOptions() {
		Utils.registerOption(options, "input_file", "StringList", true, "A file containing the tweets to locate with geolocation field.");
		Utils.registerOption(options, "output_file", "StringList", true, "An optional file to write the geolocated tweets.");

	}

}
