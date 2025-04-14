package com.openclassrooms.tourguide.dto;

import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;

public record NearByAttractionDto(String attractionName, String attractionLocation, String userLocation,
                                  double distance, int rewardPoints) {

    public NearByAttractionDto(Attraction attraction, VisitedLocation visitedLocation, double distance, int rewardPoints) {
        this(attraction.attractionName,

                "Lat : " + attraction.latitude + " Long : " + attraction.longitude,

                "Lat : " + visitedLocation.location.latitude + " Long : " + visitedLocation.location.longitude,

                distance,
                rewardPoints);
    }
}
