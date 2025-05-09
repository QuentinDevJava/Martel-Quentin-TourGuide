package com.openclassrooms.tourguide.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import lombok.extern.slf4j.Slf4j;
import tripPricer.Provider;
import tripPricer.TripPricer;

/**
 * Service responsible for managing user locations, calculating rewards, and
 * retrieving travel deals based on user preferences and proximity to
 * attractions.
 */
@Slf4j
@Service
public class TourGuideService {
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	boolean testMode = true;
	private final ExecutorService executorService = Executors.newFixedThreadPool(200);
	private Random random = new SecureRandom();

	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;

		Locale.setDefault(Locale.US);

		if (testMode) {
			log.debug("TestMode enabled");
			log.debug("Initializing users");
			initializeInternalUsers();
			log.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}

	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}

	public VisitedLocation getUserLocation(User user) {
		return (!user.getVisitedLocations().isEmpty()) ? user.getLastVisitedLocation() : trackUserLocation(user).join();
	}

	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}

	public List<User> getAllUsers() {
		return internalUserMap.values().stream().toList();
	}

	public void addUser(User user) {
		if (!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}

	public List<Provider> getTripDeals(User user) {
		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
		List<Provider> providers = tripPricer.getPrice(TRIP_PRICER_API_KEY, user.getUserId(),
				user.getUserPreferences().getNumberOfAdults(), user.getUserPreferences().getNumberOfChildren(),
				user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}

	/**
	 * Asynchronously tracks the user's location and calculates rewards based on
	 * their new position.
	 * <p>
	 * 
	 * @param user the user whose location is to be tracked
	 * @return a {@link CompletableFuture} containing the {@link VisitedLocation}
	 */
	public CompletableFuture<VisitedLocation> trackUserLocation(User user) {

		CompletableFuture<VisitedLocation> futureVisitedLocation = CompletableFuture
				.supplyAsync(() -> gpsUtil.getUserLocation(user.getUserId()), executorService);

		CompletableFuture<Void> futureAddToVisitedLocationAndReward = futureVisitedLocation
				.thenAcceptAsync(user::addToVisitedLocations, executorService)

				.thenRunAsync(() -> rewardsService.calculateRewards(user).join(), executorService);

		futureAddToVisitedLocationAndReward.join();

		return futureVisitedLocation;

	}

	/**
	 * Retrieves the 5 closest attractions to the user's current visited location.
	 *
	 * @param visitedLocation the user's most recent visited location
	 * @return a list of the 5 nearest {@link Attraction} objects
	 */
	public List<Attraction> getNearByAttractions(VisitedLocation visitedLocation) {
		return rewardsService.find5NearestAttraction(visitedLocation.location, gpsUtil.getAttractions());

	}

	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				tracker.stopTracking();
			}
		});
	}

	/**********************************************************************************
	 * 
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/
	private static final String TRIP_PRICER_API_KEY = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes
	// internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();

	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);

			internalUserMap.put(userName, user);
		});

		String result = "Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.";
		log.debug(result);
	}

	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i -> user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
				new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime())));
	}

	private double generateRandomLongitude() {
		double leftLimit = -180;
		double rightLimit = 180;
		return leftLimit + random.nextDouble() * (rightLimit - leftLimit);
	}

	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
		double rightLimit = 85.05112878;
		return leftLimit + random.nextDouble() * (rightLimit - leftLimit);
	}

	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(random.nextInt(30));
		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}

}
