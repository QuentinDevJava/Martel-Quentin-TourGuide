package com.openclassrooms.tourguide.dto;

import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.user.User;

import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;

public record NearByAttractionDto(String attractionName, String attractionLocation, String userLocation,
		double distance, int rewardPoints) {

	public NearByAttractionDto(Attraction attraction, VisitedLocation visitedLocation, User user,
			RewardsService rewardsService) {
		this(attraction.attractionName,

				"Lat : " + attraction.latitude + " Long : " + attraction.longitude,

				"Lat : " + visitedLocation.location.latitude + " Long : " + visitedLocation.location.longitude,

				rewardsService.getDistance(new Location(attraction.latitude, attraction.longitude),
						visitedLocation.location),

				rewardsService.getRewardPoints(attraction, user));
	}
}
