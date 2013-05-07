// Copyright 2012-2013 Mark Dredze. All rights reserved.
// This software is released under the 2-clause BSD license.
// Mark Dredze, mdredze@cs.jhu.edu

package carmen.types;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import carmen.utils.CarmenProperties;
import carmen.utils.Utils;

import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;

/**
 * A helper class used by the LocationResolver to handle coordinates.
 * @author Mark Dredze mdredze@cs.jhu.edu
 *
 */
public class GeocodeLocationResolver {
	private final double maxDistance;
	private final HashMap<String,List<Location>> locationMap = new HashMap<String,List<Location>>();
	private final int cellSize = 100;
	
	public GeocodeLocationResolver () throws IOException {
		maxDistance = CarmenProperties.getDouble("geocode_max_distance");
	}
	
	public Location resolveLocation(Map<String,Object> tweet) {
		LatLng givenLatLong = Utils.getLatLngFromTweet(tweet);
		
		if (givenLatLong == null)
			return null;
		
		Set<Location> locations = this.getPossibleLocations(givenLatLong);
		Location closestLocation = null;
		double closestDistance = 0;
		
		
		for (Location location : locations) {
			// Check the distance to this location.
			LatLng latLong = location.getLatLng();
			
			double distanceInMiles = LatLngTool.distance(givenLatLong, latLong, LengthUnit.MILE);
			if (closestLocation == null | closestDistance > distanceInMiles) {
				closestDistance = distanceInMiles;
				closestLocation = location;
			}
		}
		
		if (closestLocation != null && closestDistance < this.maxDistance) {
			return closestLocation;
		}
		return null;
	}

	private Set<Location> getPossibleLocations(LatLng latLong) {
		List<String> keys = this.getKeys(latLong);
		
		Set<Location> locationSet = new HashSet<Location>();
		for (String key : keys) {
			if (this.locationMap.containsKey(key)) {
				List<Location> locations = this.locationMap.get(key);
				for (Location location : locations) {
					locationSet.add(location);
				}
			}			
		}
		
		return locationSet;
	}
	
	public void addLocation(Location location) {
		if (location.getLatLng() == null)
			return;
		
		LatLng latLong = location.getLatLng();
		List<String> keys = this.getKeys(latLong);
		
		for (String key : keys) {
			if (!this.locationMap.containsKey(key)) {
				this.locationMap.put(key, new LinkedList<Location>());
			}
			
			List<Location> locations = this.locationMap.get(key);
			locations.add(location);
		}
	}

	private List<String> getKeys(LatLng latLong) {
		double latitude = latLong.getLatitude() * 100;
		double longitude = latLong.getLongitude() * 100;
		double shiftSize = this.cellSize  / (double)2;
		
		List<String> keys = new LinkedList<String>();
		
		keys.add(Integer.toString((int) (latitude/this.cellSize)) + "&&" + Integer.toString((int) (longitude/this.cellSize)));
		
		keys.add(Integer.toString((int) (latitude+shiftSize/this.cellSize)) + "&&" + Integer.toString((int) (longitude/this.cellSize)));
		keys.add(Integer.toString((int) (latitude-shiftSize/this.cellSize)) + "&&" + Integer.toString((int) (longitude/this.cellSize)));
		keys.add(Integer.toString((int) (latitude/this.cellSize)) + "&&" + Integer.toString((int) (longitude+shiftSize/this.cellSize)));
		keys.add(Integer.toString((int) (latitude/this.cellSize)) + "&&" + Integer.toString((int) (longitude-shiftSize/this.cellSize)));
		keys.add(Integer.toString((int) (latitude+shiftSize/this.cellSize)) + "&&" + Integer.toString((int) (longitude+shiftSize/this.cellSize)));
		keys.add(Integer.toString((int) (latitude+shiftSize/this.cellSize)) + "&&" + Integer.toString((int) (longitude-shiftSize/this.cellSize)));
		keys.add(Integer.toString((int) (latitude-shiftSize/this.cellSize)) + "&&" + Integer.toString((int) (longitude+shiftSize/this.cellSize)));
		keys.add(Integer.toString((int) (latitude-shiftSize/this.cellSize)) + "&&" + Integer.toString((int) (longitude-shiftSize/this.cellSize)));
				
		return keys;
	}
}
