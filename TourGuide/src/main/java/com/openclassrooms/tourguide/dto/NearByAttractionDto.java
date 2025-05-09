package com.openclassrooms.tourguide.dto;

import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;

/**
 * DTO representing an attraction located near a user.
 * <p>
 * Includes the name and coordinates of the attraction, the user's current
 * location, the distance between the two, and the reward points earned for
 * visiting.
 *
 * @param attractionName     the name of the nearby attraction
 * @param attractionLocation the coordinates of the attraction as a string
 *                           (latitude, longitude)
 * @param userLocation       the coordinates of the user's location as a string
 *                           (latitude, longitude)
 * @param distance           the distance in miles between the user and the
 *                           attraction
 * @param rewardPoints       the reward points earned for visiting the
 *                           attraction
 */
public record NearByAttractionDto(String attractionName, String attractionLocation, String userLocation,
		double distance, int rewardPoints) {

	public NearByAttractionDto(Attraction attraction, VisitedLocation visitedLocation, double distance,
			int rewardPoints) {
		this(attraction.attractionName,

				"Lat : " + attraction.latitude + " Long : " + attraction.longitude,

				"Lat : " + visitedLocation.location.latitude + " Long : " + visitedLocation.location.longitude,

				distance, rewardPoints);
	}
}
