// Copyright 2012-2013 Mark Dredze. All rights reserved.
// This software is released under the 2-clause BSD license.
// Mark Dredze, mdredze@cs.jhu.edu
package carmen.demo;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
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
import carmen.types.Location;
import carmen.utils.CommandLineUtilities;
import carmen.utils.Utils;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A simple demo that creates a location resolver and resolves tweets in an input file.
 * @author Mark Dredze mdredze@cs.jhu.edu
 *
 */
public class LocationResolverDemo {
	protected static Logger logger = Logger.getLogger(LocationResolverDemo.class);
	protected static List<Option> options = new LinkedList<Option>();
	
	public static void main(String[] args) throws ParseException, FileNotFoundException, IOException, ClassNotFoundException
	{
		// Parse the command line.
		String[] manditory_args = { "input_file" };
		createCommandLineOptions();
		CommandLineUtilities.initCommandLineParameters(args, LocationResolverDemo.options, manditory_args);

		// Get options
		String inputFile = CommandLineUtilities.getOptionValue("input_file");
		String outputFile = null;
		if (CommandLineUtilities.hasArg("output_file")) {
			outputFile = CommandLineUtilities.getOptionValue("output_file");
		}
		
		logger.info("Creating LocationResolver.");
		LocationResolver resolver = LocationResolver.getLocationResolver();

		Scanner scanner = Utils.createScanner(inputFile);
	
		Writer writer = null;
		if (outputFile != null) {
			writer = Utils.createWriter(outputFile);
			logger.info("Saving geolocated tweets to: " + outputFile);
		}
		ObjectMapper mapper = new ObjectMapper();
		int numResolved = 0;
		int total = 0;
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			@SuppressWarnings("unchecked")
			HashMap<String, Object> tweet = (HashMap<String, Object>) mapper.readValue(line, Map.class);
			
			total++;
			Location location = resolver.resolveLocationFromTweet(tweet);
			
			if (location != null) {
				logger.debug("Found location: " + location.toString());
				numResolved++;
			}
			if (writer != null) {
				if (location != null) {
					tweet.put("location", Location.createJsonFromLocation(location));
				}
				mapper.writeValue(writer, tweet);
				writer.write("\n");
			}
				
		}
		scanner.close();
		writer.close();

		logger.info("Resolved locations for " + numResolved + " of " + total + " tweets.");
	}
	
	private static void createCommandLineOptions() {
		Utils.registerOption(options, "input_file", "String", true, "A file containing the tweets to locate with geolocation field.");
		Utils.registerOption(options, "output_file", "String", true, "A file to write geolocated tweets.");
	}
}