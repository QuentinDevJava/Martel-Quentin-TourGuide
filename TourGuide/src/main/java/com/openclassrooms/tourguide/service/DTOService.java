package com.openclassrooms.tourguide.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.openclassrooms.tourguide.dto.NearByAttractionDto;
import com.openclassrooms.tourguide.user.User;

import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;

@Service
public class DTOService {

	@Autowired
	RewardsService rewardsService;

	public List<NearByAttractionDto> createDTO(VisitedLocation visitedLocation, List<Attraction> attractions, User user) {
		List<NearByAttractionDto> nearByAttractions = new ArrayList<>();

		for (Attraction attraction : attractions) {
			Location locationAttraction = new Location(attraction.latitude, attraction.longitude);
			NearByAttractionDto dto = new NearByAttractionDto(

					attraction.attractionName,

					"Lat : " + attraction.latitude + " Long : " + attraction.longitude,

					"Lat : " + visitedLocation.location.latitude + " Long : " + visitedLocation.location.longitude,

					rewardsService.getDistance(locationAttraction, visitedLocation.location),

					rewardsService.getRewardPoints(attraction, user));
			nearByAttractions.add(dto);
		}

		return nearByAttractions;
	}

}
