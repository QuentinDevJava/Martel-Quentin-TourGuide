package com.openclassrooms.tourguide;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openclassrooms.tourguide.dto.NearByAttractionDto;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import tripPricer.Provider;

@RestController
public class TourGuideController {

	private final TourGuideService tourGuideService;
	private final RewardsService rewardsService;

	public TourGuideController(TourGuideService tourGuideService, RewardsService rewardsService) {
		super();
		this.tourGuideService = tourGuideService;
		this.rewardsService = rewardsService;
	}

	@GetMapping("/")
	public String index() {
		return "Greetings from TourGuide!";
	}

	@GetMapping("/getLocation")
	public VisitedLocation getLocation(@RequestParam String userName) {
		return tourGuideService.getUserLocation(getUser(userName));
	}

	// Instead: Get the closest five tourist attractions to the user - no matter how
	// far away they are.
	// Return a new JSON object that contains:
	// Name of Tourist attraction,
	// Tourist attractions lat/long,
	// The user's location lat/long,
	// The distance in miles between the user's location and each of the
	// attractions.
	// The reward points for visiting each Attraction.
	// Note: Attraction reward points can be gathered from RewardsCentral

	/**
	 * Retrieves a list of attractions near the specified user.
	 * <p>
	 * Each attraction is represented as a {@link NearByAttractionDto}, including
	 * its name, location, the user’s current location, the distance between them,
	 * and the reward points.
	 *
	 * @param userName the username of the user
	 * @return a list of {@link NearByAttractionDto} objects representing nearby
	 *         attractions
	 */
	@GetMapping("/getNearbyAttractions")
	public List<NearByAttractionDto> getNearbyAttractions(@RequestParam String userName) {
		VisitedLocation visitedLocation = tourGuideService.getUserLocation(getUser(userName));
		List<Attraction> attractions = tourGuideService.getNearByAttractions(visitedLocation);
		User user = getUser(userName);
		return rewardsService.buildNearByAttractionDTO(visitedLocation, attractions, user);
	}

	@GetMapping("/getRewards")
	public List<UserReward> getRewards(@RequestParam String userName) {
		return tourGuideService.getUserRewards(getUser(userName));
	}

	@GetMapping("/getTripDeals")
	public List<Provider> getTripDeals(@RequestParam String userName) {
		return tourGuideService.getTripDeals(getUser(userName));
	}

	private User getUser(String userName) {
		return tourGuideService.getUser(userName);
	}
}