// Copyright 2012-2013 Mark Dredze. All rights reserved.
// This software is released under the 2-clause BSD license.
// Mark Dredze, mdredze@cs.jhu.edu

package carmen.utils;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import carmen.types.Constants;

import com.javadocmd.simplelatlng.LatLng;


public abstract class Utils {
	
	@SuppressWarnings("unchecked")
	public static Map<String, Object> getPlaceFromTweet(Map<String, Object> tweet) {
		if (tweet.containsKey(Constants.PLACE))
			return (Map<String, Object>)tweet.get(Constants.PLACE);
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String, Object> getUserFromTweet(Map<String, Object> tweet) {
		if(tweet.containsKey(Constants.TWEET_USER))
			return (Map<String, Object>)tweet.get(Constants.TWEET_USER);
		return null;
	}

	
	public static String getLocationFromTweet(Map<String, Object> tweet) {
		Map<String, Object> user = getUserFromTweet(tweet);
		if (user != null) {
			String location = (String)user.get(Constants.TWEET_USER_LOCATION);
			if(location != null && location.length() > 0)
				return location;
		}
		
		return null;
	}

	@SuppressWarnings("unchecked")
	public static LatLng getLatLngFromTweet(Map<String, Object> tweet) {
		Map<String, Object> coordinates = (Map<String, Object>) tweet.get(Constants.COORDINATES);
		if (coordinates == null)
			return null;
		ArrayList<Object> coordinateList = (ArrayList<Object>) coordinates.get(Constants.COORDINATES);

		double longitude = 0;
		if (coordinateList.get(0) instanceof Double)
			longitude = (Double)coordinateList.get(0);
		else if (coordinateList.get(0) instanceof Integer)
			longitude = (double)(Integer)coordinateList.get(0);
		else
			return null;
		
		double latitude = 0;
		if (coordinateList.get(1) instanceof Double)
			latitude = (Double)coordinateList.get(1);
		else if (coordinateList.get(1) instanceof Integer)
			latitude = (double)(Integer)coordinateList.get(1);
		else
			return null;
		
		LatLng latLng = new LatLng(latitude, longitude);
		return latLng;
	}
	
	public static String getNullForEmptyString(String string) {
		if (string.trim().length() == 0) {
			return null;
		}
		return string;					
	}
	
	public static void registerOption(List<Option> options, String option_name, String arg_name, boolean has_arg, String description) {
		OptionBuilder.withArgName(arg_name);
		OptionBuilder.hasArg(has_arg);
		OptionBuilder.withDescription(description);
		Option option = OptionBuilder.create(option_name);
		
		options.add(option);		
	}
	
	public static Writer createWriter(String outputFile)
			throws UnsupportedEncodingException, IOException,
			FileNotFoundException {
		if (outputFile.endsWith(".gz")) {
			return new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(outputFile)), "UTF-8"));
		} else {
			return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"));
		}
	}

	public static Scanner createScanner(String inputFile) throws IOException {
		InputStream inputStream = null;
		if (new File(inputFile).getName().endsWith(".gz")) {
			inputStream = new BufferedInputStream(new GZIPInputStream(new FileInputStream(inputFile)));
		} else {
			inputStream = new BufferedInputStream(new FileInputStream(inputFile));
		}
		return new Scanner(inputStream, "UTF-8");
	}
}
