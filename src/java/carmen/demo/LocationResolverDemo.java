// Copyright 2012-2013 Mark Dredze. All rights reserved.
// This software is released under the 2-clause BSD license.
// Mark Dredze, mdredze@cs.jhu.edu
package carmen.demo;

import java.io.FileNotFoundException;
import java.io.IOException;
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
		
		logger.info("Creating LocationResolver.");
		LocationResolver resolver = LocationResolver.getLocationResolver();

		Scanner scanner = Utils.createScanner(inputFile);
	
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
		}
		scanner.close();

		logger.info("Resolved locations for " + numResolved + " of " + total + " tweets.");
	}
	
	private static void createCommandLineOptions() {
		Utils.registerOption(options, "input_file", "StringList", true, "A file containing the tweets to locate with geolocation field.");
	}
}