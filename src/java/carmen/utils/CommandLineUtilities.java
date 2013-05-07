// Copyright 2012-2013 Mark Dredze. All rights reserved.
// This software is released under the 2-clause BSD license.
// Mark Dredze, mdredze@cs.jhu.edu

package carmen.utils;

import java.io.*;
import java.util.*;

import org.apache.commons.cli.*;

public class CommandLineUtilities {

	/**
	 * @param args
	 */
	private static CommandLine _command_line = null;
	private static Properties _properties = null;

	/**
	 * This uses the apache Jakarta CLI to parse the command line.
	 * A single static instance of this class exists for global access by all parts 
	 * of the program.
	 * To use this class, a list of options must be specified and passed to this method.
	 * Manditory arguments should be encoded as strings.
	 * @param args The command line received by main
	 * @param manditory_args A list of strings that contain the names of manditory arguments.
	 * @param specified_options A list of options to use for this program.
	 */
	public static void initCommandLineParameters(String[] args,
			List<Option> specified_options, String[] manditory_args) {
		Options options = new Options();
		if (specified_options != null)
			for (Option option : specified_options)
				options.addOption(option);
		
		Option option = null;
		
		OptionBuilder.withArgName("file");
		OptionBuilder.hasArg();
		OptionBuilder
				.withDescription("A file containing command line parameters as a Java properties file.");
		option = OptionBuilder.create("parameter_file");
		
		options.addOption(option);
		
		CommandLineParser command_line_parser = new GnuParser();
		CommandLineUtilities._properties = new Properties();
		try {
			CommandLineUtilities._command_line = command_line_parser.parse(
					options, args);
		} catch (ParseException e) {
			System.out.println("***ERROR: " + e.getClass() + ": "
					+ e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("parameters:", options);
			System.exit(0);
		}
		if (CommandLineUtilities.hasArg("parameter_file")) {
			String parameter_file = CommandLineUtilities.getOptionValue("parameter_file");
			// Read the property file.
			try {
				_properties.load(new FileInputStream(parameter_file));
			} catch (IOException e) {
				System.err.println("Problem reading parameter file: " + parameter_file);
			}
		}
		
		boolean failed = false;
		if (manditory_args != null) {
			for (String arg : manditory_args) {
				if (!CommandLineUtilities.hasArg(arg)) {
					failed = true;
					System.out.println("Missing argument: " + arg);
				}
			}
			if (failed) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("parameters:", options);
				System.exit(0);
			}
				
		}
	}

	public static boolean hasArg(String option) {
		if (CommandLineUtilities._command_line.hasOption(option) ||
				CommandLineUtilities._properties.containsKey(option))
			return true;
		return false;
	}
	
	/** 
	 * This method is used when there is one argument for the option presented as a ";" separated list.
	 * @param option
	 * @return
	 */
	public static String[] getOptionValues(String option)  {
		// Try and parse the list on the ";" separator.
		String arguments_to_parse = null;
		if (CommandLineUtilities._command_line.hasOption(option))
			arguments_to_parse = CommandLineUtilities._command_line.getOptionValue(option);
		if (CommandLineUtilities._properties.containsKey(option)) {
			arguments_to_parse = (String)CommandLineUtilities._properties.getProperty(option);
		}
		return arguments_to_parse.split(":");
	}
	
	public static String getOptionValue(String option) {
		if (CommandLineUtilities._command_line.hasOption(option))
			return CommandLineUtilities._command_line.getOptionValue(option);
		if (CommandLineUtilities._properties.containsKey(option))
			return (String)CommandLineUtilities._properties.getProperty(option);
		return null;
	}

	public static int getOptionValueAsInt(String option) {
		String value = CommandLineUtilities.getOptionValue(option);
		if (value != null)
			return Integer.parseInt(value);
		return -1;
	}
	
	public static float getOptionValueAsFloat(String option) {
		String value = CommandLineUtilities.getOptionValue(option);
		if (value != null)
			return Float.parseFloat(value);
		return -1;
	}

	/**
	 * Adding this option and value make it appear as if it was in the parameter
	 * file.
	 * @param option
	 */
	public static void addCommandLineVariable(String option, String value) {
		if (CommandLineUtilities._properties == null) {
			CommandLineUtilities._properties = new Properties();
		}
		CommandLineUtilities._properties.put(option, value);
	}
}
