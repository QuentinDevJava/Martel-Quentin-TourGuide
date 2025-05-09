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

/**
 * Service responsible for calculating and assigning rewards to users based on
 * their proximity to attractions and visited locations.
 */
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

	/**
	 * Asynchronously calculates and assigns rewards for a given user based on their
	 * visited locations.
	 *
	 * @param user the user for whom rewards should be calculated
	 * @return a {@link CompletableFuture} that completes when reward calculation is
	 *         done
	 */
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

	/**
	 * Checks if a location is within the defined proximity range of an attraction.
	 *
	 * @param attraction the attraction
	 * @param location   the location
	 * @return true if the location is within the attraction proximity range, false
	 *         otherwise
	 */
	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) < attractionProximityRange;
	}

	private boolean isNearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) < proximityBuffer;
	}

	/**
	 * Retrieves the reward points associated with a given attraction for a specific
	 * user.
	 *
	 * @param attraction the attraction
	 * @param user       the user
	 * @return the number of reward points
	 */
	public int getRewardPoints(Attraction attraction, User user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
	}

	/**
	 * Calculates the distance in statute miles between two geographic locations
	 * using the Haversine formula.
	 *
	 * @param loc1 the first location
	 * @param loc2 the second location
	 * @return the distance in statute miles
	 */
	public double getDistance(Location loc1, Location loc2) {
		double lat1 = Math.toRadians(loc1.latitude);
		double lon1 = Math.toRadians(loc1.longitude);
		double lat2 = Math.toRadians(loc2.latitude);
		double lon2 = Math.toRadians(loc2.longitude);

		double angle = Math
				.acos(Math.sin(lat1) * Math.sin(lat2) + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

		double nauticalMiles = 60 * Math.toDegrees(angle);
		return STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
	}

	/**
	 * Finds the 5 closest attractions to a given location from a list of
	 * attractions.
	 *
	 * @param location    the location
	 * @param attractions the list of all attractions
	 * @return a list of the 5 nearest attractions
	 */
	public List<Attraction> find5NearestAttraction(Location location, List<Attraction> attractions) {

		return attractions.stream().sorted(Comparator.comparingDouble(attraction -> getDistance(location, attraction)))
				.limit(limit).toList();
	}

	/**
	 * Builds a list of {@link NearByAttractionDto} objects representing the user's
	 * distance and rewards for each given attraction.
	 *
	 * @param visitedLocation the user's current location
	 * @param attractions     the list of attractions
	 * @param user            the user
	 * @return a list of {@link NearByAttractionDto} containing attraction data,
	 *         distance, and rewards
	 */
	public List<NearByAttractionDto> buildNearByAttractionDTO(VisitedLocation visitedLocation,
			List<Attraction> attractions, User user) {

		return attractions.stream().map(attraction -> {
			double distance = getDistance(new Location(attraction.latitude, attraction.longitude),
					visitedLocation.location);
			int rewardPoints = getRewardPoints(attraction, user);
			return new NearByAttractionDto(attraction, visitedLocation, distance, rewardPoints);

		}).toList();
	}
}
