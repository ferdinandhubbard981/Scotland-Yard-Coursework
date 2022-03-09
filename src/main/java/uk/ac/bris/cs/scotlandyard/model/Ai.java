package uk.ac.bris.cs.scotlandyard.model;



import io.atlassian.fugue.Pair;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;

/**
 * All CPU (Ai) players should implement this interface and be on the classpath.
 */
public interface Ai {

	/**
	 * @return the name of your AI, be creative
	 */
	@Nonnull String name();

	/**
	 * Called before the game starts
	 * Defaults to no-op
	 */
	default void onStart() {}

	/**
	 * @param board       the game board
	 * @param timeoutPair a pair of Long and TimeUnit used to notify the AI how long it has to make a decision before
	 *                    its turn has ended. Where the Long is the duration and the TimeUnit is the unit of time
	 *                    e.g. (milliseconds, seconds, etc...)
	 * @return a correct move from {@link Board#getAvailableMoves()} in the game board
	 */
	@Nonnull Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair);


	/**
	 * Called after the game has ended and that this Ai is about to be terminated
	 * Defaults to no-op
	 */
	default void onTerminate() {}

}
