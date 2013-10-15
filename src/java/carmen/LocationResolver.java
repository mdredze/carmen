// Copyright 2012-2013 Mark Dredze. All rights reserved.
// This software is released under the 2-clause BSD license.
// Mark Dredze, mdredze@cs.jhu.edu

package carmen;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import carmen.types.Constants;
import carmen.types.GeocodeLocationResolver;
import carmen.types.Location;
import carmen.types.ResolutionMethod;
import carmen.utils.CarmenProperties;
import carmen.utils.Utils;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This is the main class used by Carmen. A single instance is created (using getLocationResolver()).
 * 
 * Given a tweet, resolveLocationFromTweet() will return a location for a tweet. The location can
 * be saved with the tweet.
 * 
 * The returned location can be null if no location can be inferred for this tweet. If a Location is returned,
 * it can be a known or unknown location (see below.) 
 * 
 * This class provides functionality to return the children (children contained within location) and parent (location contained within parent)
 * of a location. Because the database does not know about all locations, the parent of a location may not be known (but will be returned
 * no matter what if requested.) The children will only be returned if they are known. Since unknown objects do no persist across different
 * instantiations of the LocationResolver, unknown locations should be handled with care.
 * 
 * A NONE location represents the root of the location hierarchy.
 * 
 * If use_unknown_places is true, then the LocationResolver will always return a Location when the place field is set in a tweet.
 * In this case, the Location object may be missing information (e.g. county).
 * 
 * If use_unknown_places is false, then a Location will only be returned when the place field contains a known location.
 * 
 * The carmen.properties file contains several options for controlling the behavior of the LocationResolver.
 * use_place: attempt to resolve based on the tweet's place field.
 * use_geocodes: attempt to resolve based on the tweet's coordinates field.
 * use_user_string: attempt to resolve based on the tweet's user profile's location field.
 * use_unknown_places: return places even if they are not in the database.
 * use_known_parent_for_unknown_places: if use_unknown_places is false, then this option will try to find a known parent for a location. If a known parent is found, and no location is found using another method, the parent is used. 
 * 
 * The LocationResolver relies on resources specified in carmen.properties.
 * 
 * locations: The database of locations in JSON format.
 * place_name_mapping: A mapping of Twitter place names to normalized forms.
 * state_names_file: A list of US states.
 * country_names_file: A list of known countries		
 * 
 * 
 * @author Mark Dredze mdredze@cs.jhu.edu
 *
 */
public class LocationResolver {
	protected static Logger logger = Logger.getLogger(LocationResolver.class);
	
	protected static LocationResolver resolver = null;
	
	private HashSet<String> stateFullNames = new HashSet<String>();							// list of states full names
	private HashSet<String> countryFullNames = new HashSet<String>();						// list of states full names
	private HashMap<String, String> stateAbbreviationToFullName = new HashMap<String, String>();	// list of states abbv map to states full names
	private HashMap<String, String> countryAbbreviationToFullName = new HashMap<String, String>();	// list of country abbv map to country full names
	private boolean usePlace;
	private boolean useGeocodes;
	private boolean useUserString;
	private boolean useKnownParentForUnknownPlaces;
	private GeocodeLocationResolver geocodeLocationResolver;
	private boolean useUnknownPlaces = true; // If true, return twitter place objects even when unknown in the database.
	private int newLocationIndex = Constants.NEW_LOCATION_STARTING_INDEX;

	private Pattern statePattern = Pattern.compile(".+,\\s*(\\w+)");

	private HashMap<String, String> placeNameToNormalizedPlaceName = new HashMap<String,String>();
	private HashMap<String, Location> locationNameToLocation = new HashMap<String, Location>();
	private HashMap<Location, Location> locationToParent = new HashMap<Location,Location>();
	private HashMap<Location, List<Location>> locationToChildren = new HashMap<Location,List<Location>>();
	private HashMap<Integer, Location> idToLocation = new HashMap<Integer, Location>();
	private HashMap<Location, Integer> locationToId = new HashMap<Location, Integer>();

	
	
	public static LocationResolver getLocationResolver() throws IOException {
		if (resolver == null)
			resolver = new LocationResolver();
		return resolver;
	}
	
	protected LocationResolver() throws IOException {
		
		logger.info("Geolocations based on:");
		this.usePlace = CarmenProperties.getBoolean("use_place");
		this.useGeocodes = CarmenProperties.getBoolean("use_geocodes");
		this.useUserString = CarmenProperties.getBoolean("use_user_string");
		this.useKnownParentForUnknownPlaces = CarmenProperties.getBoolean("use_known_parent_for_unknown_places");
		this.useUnknownPlaces = CarmenProperties.getBoolean("use_unknown_places");
		
		logger.info("Geocoding using these resources:");
		if (this.usePlace)
			logger.info("place");
		if (this.useGeocodes)
			logger.info("geocodes");
		if (this.useUserString)
			logger.info("user profile");
		
		
		logger.info("Loading location resources.");
		
		// Load the location objects.
		loadLocationFile(CarmenProperties.getString("locations"));
		this.idToLocation.put(-1, Location.getNoneLocation());
		
		HashSet<Location> knownLocations = new HashSet<Location>();
		for (Location location : this.idToLocation.values()) {
			knownLocations.add(location);
		}
		
		for (Location location : knownLocations) {
			Location parent = this.idToLocation.get(location.getParentId());
			//Location parent = this.createParentOfLocation(location);
			if (parent != null) {
				this.locationToParent.put(location, parent); 
				if (!this.locationToChildren.containsKey(parent))
					this.locationToChildren.put(parent, new LinkedList<Location>());
				this.locationToChildren.get(parent).add(location);
				
				// The parents are now all in the json file, so there is no reason to add them up the pipeline.
				/*
				// Add this parent up the entire hierarchy.
				// Add to the hashs, add to parent children, walk up until null.
				Location currentLocation = parent;
				parent = this.createParentOfLocation(currentLocation);
				while (parent != null) {
					if (!this.locationToParent.containsKey(currentLocation))
						this.locationToParent.put(currentLocation, parent); 
					if (!this.locationToChildren.containsKey(parent))
						this.locationToChildren.put(parent, new LinkedList<Location>());
					this.locationToChildren.get(parent).add(currentLocation);
					
					currentLocation = parent;
					parent = this.createParentOfLocation(currentLocation);
				}
				*/
			}
		}
		
		
		if (this.usePlace) {
			loadNameAndAbbreviation(CarmenProperties.getString("place_name_mapping"), null, this.placeNameToNormalizedPlaceName, false);
		}
		
		loadNameAndAbbreviation(CarmenProperties.getString("state_names_file"), this.stateFullNames, this.stateAbbreviationToFullName, true);
		loadNameAndAbbreviation(CarmenProperties.getString("country_names_file"), this.countryFullNames, this.countryAbbreviationToFullName, true);
		
		if (this.useGeocodes) {
			// Register the locations as known places for geocode resolution.
			this.geocodeLocationResolver = new GeocodeLocationResolver();
			for (Location location : this.idToLocation.values())
				this.geocodeLocationResolver.addLocation(location);
		}	
	}
	
	
	private Location createParentOfLocation(Location location, boolean registerLocation) {
		// If we have a city, backoff to the state.
		Location parentLocation = null;
		if (location.getCity() != null)
			parentLocation =  new Location(location.getCountry(), location.getState(), location.getCounty(), null, -1, -1, false);
		else if (location.getCounty() != null)
			parentLocation =  new Location(location.getCountry(), location.getState(), null, null, -1, -1, false);
		else if (location.getState() != null)
			parentLocation =  new Location(location.getCountry(), null, null, null, -1, -1, false);
		else if (location.getCountry() != null && !location.getCountry().equalsIgnoreCase(Constants.DS_LOCATION_NONE))
			parentLocation =  Location.getNoneLocation();

		if (parentLocation == null)
			return null;

		// The parent location is missing an id. We will retrieve the location that correctly contains the id.
		// The equals method doesn't look at the id, so this lookup will work.
		if (this.locationToId.containsKey(parentLocation)) {
			return this.idToLocation.get(this.locationToId.get(parentLocation));
		}

		if (registerLocation)
			registerNewLocation(parentLocation);
		
		return parentLocation;
	}

	// Load files
	protected static void loadNameAndAbbreviation(String filename,
			HashSet<String> fullName,
			HashMap<String, String> abbreviations, boolean secondColumnKey) throws FileNotFoundException {
		Scanner inputScanner = new Scanner(new FileInputStream(filename),"UTF-8");
		while (inputScanner.hasNextLine()) {
			String line = inputScanner.nextLine().toLowerCase();
			String[] splitString = line.split("\t");
			splitString[0] = splitString[0].trim();
			if (fullName != null)
				fullName.add(splitString[0]);
			if (abbreviations != null) {
				if (!secondColumnKey) {
					abbreviations.put(splitString[0], splitString[1]);
				} else {
					abbreviations.put(splitString[1], splitString[0]);
				}
			}
		}
		inputScanner.close();		
	}

	// Getter/Setter
	public boolean isUseUnknownPlaces() {
		return useUnknownPlaces;
	}

	public void setUseUnknownPlaces(boolean useUnknownPlaces) {
		this.useUnknownPlaces = useUnknownPlaces;
	}
	
	public Location resolveLocationFromTweet(Map<String,Object> tweet) {
		Location location = null;
		Location provisionalLocation = null;
		if (this.usePlace) {
			location = resolveLocationUsingPlace(tweet);
			
			if (location != null) {
				location.setResolutionMethod(ResolutionMethod.PLACE);
				
				if (!location.isKnownLocation()) {
					// The location is not known. Should we use it?
					if (this.useUnknownPlaces)
						// Yes, use it. Register a new location.
						registerNewLocation(location);
					else if (this.useKnownParentForUnknownPlaces) {
						// Don't use it, but try to find a known parent.
						Location parent = this.createParentOfLocation(location, false);
						while (parent != null && !parent.isKnownLocation()) {
							parent = this.createParentOfLocation(parent, false);
						}
						if (parent != null && parent.isKnownLocation())
							provisionalLocation = parent;
						// Try to find a better place using another method before using the parent.
					} else
						// We can't find a known location.
						location = null;
				}
			}
		}
		if (location == null && this.useGeocodes) {
			location = resolveLocationUsingGeocodes(tweet);
			if (location != null)
				location.setResolutionMethod(ResolutionMethod.COORDINATES);
		}
		
		if (location == null && this.useUserString) {
			location = resolveLocationUsingUserLocation(tweet);
			if (location != null)
				location.setResolutionMethod(ResolutionMethod.USER_LOCATION);
		}
		
		if (location == null && provisionalLocation != null)
			location = provisionalLocation;
		
		return location;
	}

	protected Location resolveLocationUsingPlace(Map<String,Object> tweet) {
		Map<String,Object> place = Utils.getPlaceFromTweet(tweet);
		if (place == null)
			return null;
		
		String url = (String)place.get("url");
		String id = (String)place.get("id");
		String country = (String)place.get("country");
		if (country == null) {
			logger.warn("Found place with no country: " + place.toString());
			return null;
		}
		
		if (this.placeNameToNormalizedPlaceName.containsKey(country.toLowerCase())) {
			country = placeNameToNormalizedPlaceName.get(country.toLowerCase());
		}
		
		String placeType = (String)place.get("place_type"); 
		if (placeType.equalsIgnoreCase("city")) {
			String city = (String)place.get("name");
			
			if (country.equalsIgnoreCase("united states")) {
				String fullName = (String)place.get("full_name");
				String state = null;
				
				if (fullName == null) {
					logger.warn("Found place with no full_name: " + place.toString());
					return null;
				}
				
				Matcher matcher = this.statePattern.matcher(fullName);
				if (matcher.matches()) {
					// extracting the state name
					String matchedString = matcher.group(1).toLowerCase();
					if(stateAbbreviationToFullName.containsKey(matchedString)) {
						state = stateAbbreviationToFullName.get(matchedString);
					}
				}
				
				return getLocationForPlace(country, state, null, city, url, id);
			} else {
				return getLocationForPlace(country, null, null, city, url, id);
			}
		} else if (placeType.equalsIgnoreCase("admin")) {
			String state = (String)place.get("name");
			return getLocationForPlace(country, state, null, null, url, id);
		} else if (placeType.equalsIgnoreCase("country")) {
			return getLocationForPlace(country, null, null, null, url, id);
		} else if  (placeType.equalsIgnoreCase("neighborhood") || placeType.equalsIgnoreCase("poi")) {
			String fullName = (String)place.get("full_name");
			if (fullName == null) {
				logger.warn("Found place with no full_name: " + place.toString());
				return null;
			}
			String[] splitFullName = fullName.split(",");
			String city = null;
			if (splitFullName.length > 1) {
				city = splitFullName[1];
			}
			return getLocationForPlace(country, null, null, city, url, id);
		} else {
			logger.warn("Unknown place type: " + placeType);
		}
		
		return null;
	}

	protected Location resolveLocationUsingGeocodes(Map<String,Object> tweet) {
		return this.geocodeLocationResolver.resolveLocation(tweet);
	}

	protected Location resolveLocationUsingUserLocation(Map<String,Object> tweet) {
		String tweetLocation = Utils.getLocationFromTweet(tweet);
		if (tweetLocation != null) {
			String location = tweetLocation.replaceAll("\\p{Punct}", " ").replaceAll("\\s+", " ").toLowerCase().trim();
			
			// Check if this is a known location
			if (this.locationNameToLocation.containsKey(location)) {
				return this.locationNameToLocation.get(location);
			}
			
			// Look for patterns in the location. Replace punctuation but keep ","
			String locationWithComma = tweetLocation.replaceAll("[!\\\"#$%&'\\(\\)\\*\\+-\\./:;<=>\\?@\\[\\\\]^_`\\{\\|\\}~]", " ").replaceAll("\\s+", " ").toLowerCase().trim();
			
			Matcher matcher = this.statePattern .matcher(locationWithComma);
			if (matcher.matches()) 	{
				// extracting the state name or country name of location strings, if available
				String matchedString = matcher.group(1).toLowerCase();
				String stateOrCountryName = null;
				if (stateFullNames.contains(matchedString) || countryFullNames.contains(matchedString)) 
					stateOrCountryName = matchedString;
				else if (stateAbbreviationToFullName.containsKey(matchedString)) 
					stateOrCountryName = stateAbbreviationToFullName.get(matchedString);
				else if (countryAbbreviationToFullName.containsKey(matchedString)) 
					stateOrCountryName = countryAbbreviationToFullName.get(matchedString);
			
				if (stateOrCountryName != null && this.locationNameToLocation.containsKey(stateOrCountryName)) {
					return this.locationNameToLocation.get(stateOrCountryName);
				}
			}
		}
			
		return null;
	}
	
	/**
	 * 
	 * @param number
	 * @return
	 */
	public Location getLocationForId(int id) {
		if (this.idToLocation.containsKey(id))
			return this.idToLocation.get(id);
		throw new IllegalArgumentException("Unknown location for index: " + id);
	}

	/**
	 * 
	 * @param fileName
	 * @param splitOn
	 * @return
	 * @throws IOException 
	 */
	protected HashMap<String, Integer> loadLocationToIdFile(String filename) throws IOException
	{
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		Scanner inputScanner = new Scanner(new FileInputStream(filename),"UTF-8");
		int lineNumber = 0;
		while (inputScanner.hasNextLine()) {
			lineNumber++;
			String line = null;
			try {
				line = inputScanner.nextLine().toLowerCase();
				String[] splitString = line.split("\t");
				int locationId = Integer.parseInt(splitString[0].trim());
				for (int ii = 1; ii < splitString.length; ii++) {
					String entry = splitString[ii].trim();
					// Check for duplicates.
					if (map.containsKey(entry) && !map.get(entry).equals(locationId)) {
						logger.warn("Duplicate location found: " + entry);
					}
					map.put(entry, locationId);
				}
			} catch (Exception e) {
				logger.warn("Error in location to id file line " + lineNumber + ": " + filename + ";" + line);
			}
		}
		inputScanner.close();
		return map;
	}
	
	@SuppressWarnings("unchecked")
	protected void loadLocationFile(String filename) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();

		Scanner inputScanner = new Scanner(new FileInputStream(filename));
		while (inputScanner.hasNextLine()) {
			String line = inputScanner.nextLine();
			Map<String,Object> locationObj = mapper.readValue(line, Map.class);
			Location location = Location.parseLocationFromJsonObj(locationObj);
			
			List<String> aliases = (List<String>)locationObj.get("aliases");
			this.idToLocation.put(location.getId(), location);
			this.locationToId.put(location, location.getId());
			HashSet<String> justAddedAliases = new HashSet<String>();
			for (String alias : aliases) {
				if (justAddedAliases.contains(alias))
					continue;
				
				if (this.locationNameToLocation.containsKey(alias))
					logger.warn("Duplicate location name: " + alias);
				else
					this.locationNameToLocation.put(alias, location);
				justAddedAliases.add(alias);
				
				// Add entries without punctuation.
				String newEntry = alias.replaceAll("\\p{Punct}", " ").replaceAll("\\s+", " ");
				if (justAddedAliases.contains(newEntry))
					continue;
				
				if (!newEntry.equals(alias)) {
					if (this.locationNameToLocation.containsKey(newEntry))
						logger.warn("Duplicate location name: " + newEntry);
					else
						this.locationNameToLocation.put(newEntry, location	);
				}
				
				justAddedAliases.add(newEntry);
				
			}
		}
		inputScanner.close();
	}
//	/**
//	 * 
//	 * @param fileName
//	 * @param splitOn
//	 * @return
//	 * @throws IOException 
//	 * @throws JsonMappingException 
//	 * @throws JsonParseException 
//	 */
//	protected HashMap<Integer, Location> loadLocationFile(String filename) throws JsonParseException, JsonMappingException, IOException {
//		HashMap<Integer, Location> map = new HashMap<Integer, Location>();						
//		Scanner inputScanner = new Scanner(new FileInputStream(filename),"UTF-8");
//		while (inputScanner.hasNextLine()) {
//			String line = inputScanner.nextLine();
//			String[] splitString = line.split("\t");
//			Location location = Location.parseLocation(splitString[1].trim());
//			map.put(Integer.parseInt(splitString[0].trim()), location);
//		}
//		inputScanner.close();
//		return map;
//	}
	
	protected Location getLocationForPlace(String country, String state,
			String county, String city, String url, String id) {
		Location location = new Location(country, state, county, city, -1, -1, false);
		
		// This we already have a location object, use it.
		if (this.locationToId.containsKey(location)) {
			return this.idToLocation.get(this.locationToId.get(location));
		}
		
		// This is an unknown location.
		location.setUrl(url);
		location.setTwitterId(id);
		
		return location;
	}

	private void registerNewLocation(Location location) {
		// There is no such location. Create a new index with a large offset.
		int index = this.newLocationIndex++;

		location.setId(index);
		this.locationToId.put(location, index);
		this.idToLocation.put(index, location);

		// Put in hierarchy.
		Location parent = this.createParentOfLocation(location, true);
		if (parent != null) {
			this.locationToParent.put(location, parent); 
			if (!this.locationToChildren.containsKey(parent))
				this.locationToChildren.put(parent, new LinkedList<Location>());
			this.locationToChildren.get(parent).add(location);
		}
	}

	public Location getParent(Location location) {
		if (this.locationToParent.containsKey(location))
			return this.locationToParent.get(location);
		return null;
	}
	
	public List<Location> getChildren(Location location) {
		if (this.locationToChildren.containsKey(location))
			return this.locationToChildren.get(location);
		return null;
	}

	/**
	 * If a location object was created and saved, it may be unknown to this LocationResolver.
	 * This method returns the known version of the Location or registers it as a new location.
	 * @param location
	 * @return
	 */
	public Location lookupLocation(Location location) {
		if (this.locationToId.containsKey(location)) {
			return this.idToLocation.get(this.locationToId.get(location));
		}
		
		this.registerNewLocation(location);
		return location;
	}
}

