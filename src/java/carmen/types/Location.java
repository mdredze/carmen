// Copyright 2012-2013 Mark Dredze. All rights reserved.
// This software is released under the 2-clause BSD license.
// Mark Dredze, mdredze@cs.jhu.edu

package carmen.types;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import carmen.LocationResolver;
import carmen.utils.Utils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javadocmd.simplelatlng.LatLng;

/**
 * A location object contains based information about a location and additional information about
 * how the location was identified.
 * 
 * Basic location information includes Country, State, County, City. This information 
 * may be null (e.g. USA only has country filled in) but it is always known; null means
 * does not apply.
 * 
 * Each location that appears in the location database contains a latitude and longitude.
 * This is the center of the location. Distance to these coordinates is the distance to the
 * center of the location, not to the border.
 * 
 * Locations that are identified based only on Twitter places will have a url and twitterId field.
 * The URL provides additional information from Twitter about the location.
 * 
 * The location that represents Earth (all locations) is called none. The NoneLocation field
 * will be true for this location. Every location's ancestor includes None as the root of the 
 * hierachy.
 * 
 * If a Location appears in the database, it will has knownLocation set to true. Additionally, its
 * id will correspond to the id in the database. If a location is not known, it will either come
 * from a Twitter place or be a presumed location with a known child. The id of these locations
 * will be dependent on the particular run of the software as they are not stored on disk.
 * The IDs will be large numbers (see LocationResolver).
 * 
 * When a location object is returned for a tweet, the field ResolutionMethod will be set to the 
 * method used for resolving the location.
 * 
 * @author Mark Dredze mdredze@cs.jhu.edu
 *
 */
public class Location {
	protected String county = null;
	protected String country = null;
	protected String city = null;
	protected String state = null;
	
	protected int id = -1;
	protected int parentId = -1;
	protected double latitude = 0;
	protected double longitude = 0;
	
	protected String url;
	protected String twitterId;
	
	protected boolean isNone = false;
	protected ResolutionMethod resolutionMethod = null;
	protected boolean knownLocation;
	
	
	public Location(String country, String state, String county, String city, int id, int parentId, boolean knownLocation) {
		this.country = country;
		this.state = state;
		this.county = county;
		this.city = city;
		this.id = id;
		this.parentId = parentId;
		this.knownLocation = knownLocation;
	}
	
	public Location(String country, String state, String county, String city, double latitude, double longitude, int id, int parentId, boolean knownLocation) {
		this(country, state, county, city, id, parentId, knownLocation);
		this.latitude = latitude;
		this.longitude = longitude;
	}
	

	public String getCountry() {
		return this.country;
	}

	public String getCity() {
		return city;
	}

	public String getState() {
		return state;
	}
	
	public String getCounty() {
		return county;
	}
	
	public int getParentId() {
		return this.parentId;
	}
	
	public boolean equals(Object object) {
		if (object instanceof Location) {
			Location location = (Location)object;
			
			if (areEqual(this.city, location.city) &&
			areEqual(this.state, location.state) &&
			areEqual(this.county, location.county) &&
			areEqual(this.country, location.country) &&
			this.isNone == location.isNone)
				return true;
			
		}
		return false;		
	}

	private boolean areEqual(String string1, String string2) {
		if (string1 == "")
			string1 = null;
		if (string2 == "")
			string2 = null;
		
		if ((string1 == null && string2 != null) ||
				(string1 != null && string2 == null)) {
				return false;
			}
		else if (string1 != null && string2 != null && !string1.equalsIgnoreCase(string2))
			return false;
		return true;
		
	}
	
	public int hashCode() {
		int total = 0;
		if (this.city != null)
			total += this.city.toLowerCase().hashCode();
		if (this.county != null)
			total += this.county.toLowerCase().hashCode();
		if (this.state != null)
			total += this.state.toLowerCase().hashCode();
		if (this.country != null)
			total += this.country.toLowerCase().hashCode();
		if (this.isNone)
			total ++;
		return total;
	}
	
	public String getTwitterId() {
		return this.twitterId;
	}

	public String getUrl() {
		return this.url;
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		if (this.city != null)
			sb.append("city: \"" + this.city + "\",");
		if (this.county != null)
			sb.append("county: \"" + this.county + "\",");
		if (this.state != null)
			sb.append("state: \"" + this.state + "\",");
		if (this.country != null)
			sb.append("country: \"" + this.country + "\",");
		sb.append(" (known:" + this.knownLocation + ", " + this.getId() + ")");
		return sb.toString();
	}

	public int getId() {
		return this.id;
	}
	
	public static Location getNoneLocation() {
		Location location = new Location(null, null, null, null, -1, -1, true);
		location.isNone  = true;
		return location;
	}

	public String getDisplayString() {
		StringBuilder sb = new StringBuilder();
		boolean hasOpenParen = false;
		if (this.city != null) {
			sb.append(this.city + " (");
			hasOpenParen = true;
		}
		if (this.county != null) {
			sb.append(this.county); 
			if (!hasOpenParen) {
				sb.append(" (");
				hasOpenParen = true;
			} else {
				sb.append(", ");
			}
		}
		if (this.state != null) {
			sb.append(this.state); 
			if (!hasOpenParen) {
				sb.append(" (");
				hasOpenParen = true;
			} else {
				sb.append(", ");
			}
		}
		if (this.country != null) {
			sb.append(this.country);
		}
		if (hasOpenParen)
			sb.append(")");
			
		return sb.toString();
	}

	public boolean isCountryOrStateOrCounty() {
		return (city == null);
	}
	
	public LatLng getLatLng() {
		LatLng point = new LatLng(this.latitude, this.longitude);
		return point;
	}

	/**
	 * Does this location object contain the provided location?
	 * @param location
	 * @return
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	public boolean containsLocation(Location location) throws IOException, ClassNotFoundException {
		while (location != null) {
			if (this.equals(location))
				return true;
			location = LocationResolver.getLocationResolver().getParent(location);
		}
		return false;
	}

	public void setId(int id) {
		this.id = id;
	}

	public boolean isNone() {
		return this.isNone;
	}

	public void setUrl(String url) {
		this.url = url;		
	}
	
	public void setTwitterId(String twitterId) {
		this.twitterId = twitterId;
	}

	public void setResolutionMethod(ResolutionMethod resolutionMethod) {
		this.resolutionMethod = resolutionMethod;
		
	}
	public ResolutionMethod getResolutionMethod() {
		return this.resolutionMethod;
	}

	public boolean isKnownLocation() {
		return this.knownLocation;
	}
	
	public void setKnownLocation(boolean value) {
		this.knownLocation = value;
	}

	public static Location parseLocation(String jsonString) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		@SuppressWarnings("unchecked")
		Map<String,Object> locationMap = (Map<String,Object>)mapper.readValue(jsonString, Map.class);
		
		return parseLocationFromJsonObj(locationMap);
	}

	public static Location parseLocationFromJsonObj(
			Map<String, Object> locationMap) {
		String country = Utils.getNullForEmptyString((String)locationMap.get("country"));
		String state =  Utils.getNullForEmptyString((String)locationMap.get("state"));
		String county =  Utils.getNullForEmptyString((String)locationMap.get("county"));
		String city =  Utils.getNullForEmptyString((String)locationMap.get("city"));
		int id = Integer.parseInt((String)locationMap.get("id"));
		double latitude = Double.parseDouble((String)locationMap.get("latitude"));
		double longitude = Double.parseDouble((String)locationMap.get("longitude"));
		int parentId = Integer.parseInt((String)locationMap.get("parent_id"));
		return new Location(country, state, county, city,
				latitude, longitude, id, parentId, true);
	}
	
	public static Map<String, Object> createJsonFromLocation(Location location) {
		Map<String,Object> jsonObject = new HashMap<String, Object>();
		jsonObject.put("country", location.getCountry());
		jsonObject.put("state", location.getState());
		jsonObject.put("county", location.getCounty());
		jsonObject.put("city", location.getCity());
		jsonObject.put("id", location.getId());
		jsonObject.put("latitude", location.getLatLng().getLatitude());
		jsonObject.put("longitude", location.getLatLng().getLongitude());
		
		if (location.getUrl() != null && location.getUrl().length() != 0)
			jsonObject.put("id", location.getId());
		if (location.getTwitterId() != null && location.getTwitterId().length() != 0)
			jsonObject.put("id", location.getId());
		
		
		return jsonObject;
	}
}