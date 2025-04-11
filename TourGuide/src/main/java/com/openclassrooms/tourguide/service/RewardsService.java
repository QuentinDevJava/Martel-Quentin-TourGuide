package com.openclassrooms.tourguide.service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;

import com.openclassrooms.tourguide.dto.NearByAttractionDto;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;

@Service
public class RewardsService {
	private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
	private int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200;
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;
	private long limit = 5;
	private final ExecutorService executorService = Executors.newFixedThreadPool(200);

	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
	}

	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}

	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}

	public CompletableFuture<Void> calculateRewards(User user) {
		CompletableFuture<List<VisitedLocation>> futureUserLocations = CompletableFuture
				.supplyAsync(() -> new CopyOnWriteArrayList<>(user.getVisitedLocations()), executorService);
		CompletableFuture<List<Attraction>> futureAttractions = CompletableFuture.supplyAsync(gpsUtil::getAttractions,
				executorService);

		return CompletableFuture.runAsync(
				() -> futureUserLocations.join().stream().forEach(visitedLocation -> futureAttractions.join().stream()
						.filter(attraction -> isNearAttraction(visitedLocation, attraction))
						.forEach(attraction -> user.addUserReward(
								new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user))))),
				executorService);

	}

	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}

	private boolean isNearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
	}

	public int getRewardPoints(Attraction attraction, User user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
	}

	public double getDistance(Location loc1, Location loc2) {
		double lat1 = Math.toRadians(loc1.latitude);
		double lon1 = Math.toRadians(loc1.longitude);
		double lat2 = Math.toRadians(loc2.latitude);
		double lon2 = Math.toRadians(loc2.longitude);

		double angle = Math
				.acos(Math.sin(lat1) * Math.sin(lat2) + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

		double nauticalMiles = 60 * Math.toDegrees(angle);
		double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
		return statuteMiles;
	}

	public List<Attraction> find5NearestAttraction(Location location, List<Attraction> attractions) {

		return attractions.stream().sorted(Comparator.comparingDouble(attraction -> getDistance(location, attraction)))
				.limit(limit).toList();
	}

	public List<NearByAttractionDto> buildNearByAttractionDTO(VisitedLocation visitedLocation,
			List<Attraction> attractions, User user) {
		return attractions.stream().map(attraction -> new NearByAttractionDto(attraction, visitedLocation, user, this))
				.toList();
	}
}
