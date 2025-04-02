package com.openclassrooms.tourguide.dto;

public record NearByAttractionDto(String attractionName, String attractionLocation, String userLocation, double distance,
		int rewardPoints) {

}
