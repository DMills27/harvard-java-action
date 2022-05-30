package com.realdecoy.util;

import java.util.logging.Logger;

public class TimeUtils {

	public static final void displayElapsedTime(long startTime, long endTime, Logger logger) {

		long elapsedTime = (((endTime - startTime) / 1000) / 60);

		if (elapsedTime == 0) {
			elapsedTime = ((endTime - startTime) / 1000);

			if (elapsedTime == 0) {
				elapsedTime = (endTime - startTime);
				logger.info("Elapsed time: " + elapsedTime + " millisecond(s).");
			} else {
				logger.info("Elapsed time: " + elapsedTime + " second(s).");
			}

		} else {
			logger.info("Elapsed time: " + elapsedTime + " minute(s).");
		}
	}

}
