package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableSet;

import javax.annotation.Nonnull;

/**
 * The game model of ScotlandYard game.
 * Implementations of this class should make use of an instance of the
 * {@link uk.ac.bris.cs.scotlandyard.model.Board.GameState}.
 */
public interface Model {
	/**
	 * An observer of the ScotlandYard game.
	 * The observer gets notified about game events in {@link Event}
	 */
	interface Observer {
		/**
		 * Game events
		 */
		enum Event {MOVE_MADE, GAME_OVER}
		/**
		 * Called once game state changes
		 *
		 * @param board the board at the time of change
		 * @param event the event that triggered this call
		 */
		default void onModelChanged(@Nonnull Board board, @Nonnull Event event) {}
	}
	/**
	 * @return the current game board
	 */
	@Nonnull Board getCurrentBoard();
	/**
	 * Registers an observer to the model. It is an error to register the same observer more than
	 * once.
	 *
	 * @param observer the observer to register
	 */
	void registerObserver(@Nonnull Observer observer);
	/**
	 * Unregisters an observer to the model. It is an error to unregister an observer not
	 * previously registered with {@link #registerObserver(Observer)}.
	 *
	 * @param observer the observer to register
	 */
	void unregisterObserver(@Nonnull Observer observer);
	/**
	 * @return all currently registered observers of the model
	 */
	@Nonnull ImmutableSet<Observer> getObservers();
	/**
	 * @param move delegates the move to the underlying
	 * {@link uk.ac.bris.cs.scotlandyard.model.Board.GameState}
	 */
	void chooseMove(@Nonnull Move move);
}
