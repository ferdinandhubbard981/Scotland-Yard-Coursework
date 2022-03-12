package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import java.util.LinkedList;

import javax.annotation.Nonnull;

import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		if (mrX == null) throw new NullPointerException();
		if (detectives == null) throw new NullPointerException();
		if (detectives.contains(null)) throw new NullPointerException();
		LinkedList<Player> detectiveCopy = ImmutableList.copyOf(detectives);
		throw new IllegalArgumentException();

		throw new RuntimeException("Implement me!");

	}
	//FUNCTIONS 
	 private final class MyGameState implements GameState {
		@Override public GameSetup getSetup() {  return null; }
		@Override  public ImmutableSet<Piece> getPlayers() { return null; }
		@Nonnull Optional<Integer> getDetectiveLocation(Detective detective) { return null; }
		@Nonnull Optional<TicketBoard> getPlayerTickets(Piece piece) { return null; }
		@Nonnull ImmutableList<LogEntry> getMrXTravelLog() { return null; }
		@Nonnull ImmutableSet<Piece> getWinner() { return null; }
		@Nonnull ImmutableSet<Move> getAvailableMoves() { return null; }
		@Override public GameState advance(Move move) {  return null;  }
	}
	//FUNCTIONS END
}
