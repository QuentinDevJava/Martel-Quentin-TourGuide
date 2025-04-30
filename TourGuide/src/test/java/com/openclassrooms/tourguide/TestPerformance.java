package com.openclassrooms.tourguide;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.openclassrooms.tourguide.config.ExcelWriter;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import lombok.extern.slf4j.Slf4j;
import rewardCentral.RewardCentral;

@Slf4j
@Tag("performance")
class TestPerformance {

	private ExcelWriter excelWriter = new ExcelWriter();

	/*
	 * A note on performance improvements:
	 * 
	 * The number of users generated for the high volume tests can be easily
	 * adjusted via this method:
	 * 
	 * InternalTestHelper.setInternalUserNumber(100000);
	 * 
	 * 
	 * These tests can be modified to suit new solutions, just as long as the
	 * performance metrics at the end of the tests remains consistent.
	 * 
	 * These are performance metrics that we are trying to hit:
	 * 
	 * highVolumeTrackLocation: 100,000 users within 15 minutes:
	 * assertTrue(TimeUnit.MINUTES.toSeconds(15) >=
	 * TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	 *
	 * highVolumeGetRewards: 100,000 users within 20 minutes:
	 * assertTrue(TimeUnit.MINUTES.toSeconds(20) >=
	 * TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	 */

	@ParameterizedTest
	@ValueSource(ints = { 100, 1000, 5000, 10000, 50000, 100000 })
	void highVolumeTrackLocation(int nbuser) {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());

		InternalTestHelper.setInternalUserNumber(nbuser);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
		List<User> allUsers = tourGuideService.getAllUsers();

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		List<CompletableFuture<VisitedLocation>> futures = allUsers.parallelStream()
				.map(tourGuideService::trackUserLocation).toList();

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

		stopWatch.stop();
		tourGuideService.tracker.stopTracking();

		excelWriter.writePerformanceResult("highVolumeTrackLocation", nbuser,
				TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));

		log.info("highVolumeTrackLocation for " + nbuser + " : Time Elapsed: "
				+ TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");

		assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	}

	@ParameterizedTest
	@ValueSource(ints = { 100, 1000, 5000, 10000, 50000, 100000 })
	void highVolumeGetRewards(int nbuser) {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		InternalTestHelper.setInternalUserNumber(nbuser);

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		Attraction attraction = gpsUtil.getAttractions().get(0);

		List<User> allUsers = tourGuideService.getAllUsers();

		allUsers.parallelStream()
				.forEach(u -> u.addToVisitedLocations(new VisitedLocation(u.getUserId(), attraction, new Date())));

		allUsers.parallelStream().forEach(user -> rewardsService.calculateRewards(user)
				.thenAccept(res -> assertFalse(user.getUserRewards().isEmpty())));

		stopWatch.stop();
		tourGuideService.tracker.stopTracking();

		excelWriter.writePerformanceResult("highVolumeGetRewards", nbuser,
				TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));

		log.info("highVolumeGetRewards for " + nbuser + " : Time Elapsed: "
				+ TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");

		assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	}

}
