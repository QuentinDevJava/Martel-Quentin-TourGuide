package com.openclassrooms.tourguide.dto;

public record NearByAttractionDto(String attractionName, String attractionLocation, String userLocation,
		double distance, int rewardPoints) {

}
//public NearByAttractionDto() {
//	TODO ajouter logique de creation du dto 
//}

//public NearAttractionDTO(Attraction attraction, VisitedLocation location, distance, rewardPoints) {
//    this.attractionName = attraction.attractionName
//    this.attractionLocation = String.format("Lat: %d Long: %d", attraction.latitude, attraction.longitude)
//    this.userLocation = String.format("Lat: %d Long: %d", visitedLocation.latitude, visitedLocation.longitude)
//    this.distance = distance
//    this.rewardPoints = rewardPoints
//}