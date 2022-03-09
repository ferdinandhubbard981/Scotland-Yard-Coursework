package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.Optional;

import javax.annotation.Nonnull;

import uk.ac.bris.cs.scotlandyard.model.Piece.Detective;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;

/**
 * Represents the ScotlandYard game board
 */
public interface Board {

	/**
	 * Represents the ScotlandYard ticket board for each player
	 */
	interface TicketBoard {
		/**
		 * @param ticket the ticket to check count for
		 * @return the amount of ticket, always &gt;= 0
		 */
		int getCount(@Nonnull Ticket ticket);
	}

	/**
	 * @return the current game setup
	 */
	@Nonnull GameSetup getSetup();
	/**
	 * @return all players in the game
	 */
	@Nonnull ImmutableSet<Piece> getPlayers();
	/**
	 * @param detective the detective
	 * @return the location of the given detective; empty if the detective is not part of the game
	 */
	@Nonnull Optional<Integer> getDetectiveLocation(Detective detective);
	/**
	 * @param piece the player piece
	 * @return the ticket board of the given player; empty if the player is not part of the game
	 */
	@Nonnull Optional<TicketBoard> getPlayerTickets(Piece piece);
	/**
	 * @return MrX's travel log as a list of {@link LogEntry}s.
	 */
	@Nonnull ImmutableList<LogEntry> getMrXTravelLog();
	/**
	 * @return the winner of this game; empty if the game has no winners yet
	 * This is mutually exclusive with {@link #getAvailableMoves()}
	 */
	@Nonnull ImmutableSet<Piece> getWinner();
	/**
	 * @return the current available moves of the game.
	 * This is mutually exclusive with {@link #getWinner()}
	 */
	@Nonnull ImmutableSet<Move> getAvailableMoves();


	/**
	 * Represents an on-going ScotlandYard game where moves by each player advances the game.
	 */
	interface GameState extends Board {
		/**
		 * Computes the next game state given a move from {@link #getAvailableMoves()} has been
		 * chosen and supplied as the parameter
		 *
		 * @param move the move to make
		 * @return the game state of which the given move has been made
		 * @throws IllegalArgumentException if the move was not a move from
		 * {@link #getAvailableMoves()}
		 */
		@Nonnull GameState advance(Move move);
	}


}
