package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import uk.ac.bris.cs.scotlandyard.model.Piece.Detective;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;

/**
 * A POJO representing an immutable game board.
 * This is useful for snapshotting or serialising game states.
 * <br>
 * <strong>NOTE:</strong>
 * This class isn't really intended for use with the cw-model part but if you can justify the use
 * then feel free to include it.
 */
public class ImmutableBoard implements Board, Serializable {
	private static final long serialVersionUID = -7495825440220065823L;

	private final GameSetup setup;
	private final ImmutableMap<Detective, Integer> detectiveLocations;
	private final ImmutableMap<Piece, ImmutableMap<Ticket, Integer>> tickets;
	private final ImmutableList<LogEntry> mrXTravelLog;
	private final ImmutableSet<Piece> winner;
	private final ImmutableSet<Move> availableMoves;


	/**
	 * Creates an immutable board snapshot of the given board
	 */
	public ImmutableBoard(Board that) {
		this.setup = Objects.requireNonNull(that.getSetup());
		this.detectiveLocations = Objects.requireNonNull(that.getPlayers().stream()
				.filter(Piece::isDetective)
				.map(Detective.class::cast)
				.collect(ImmutableMap.toImmutableMap(Function.identity(),
						x1 -> that.getDetectiveLocation(x1).orElseThrow())));
		this.tickets = Objects.requireNonNull(
				that.getPlayers().stream().collect(ImmutableMap.toImmutableMap(
						Function.identity(), x -> {
							TicketBoard board = that.getPlayerTickets(x).orElseThrow();
							return Stream.of(Ticket.values()).collect(ImmutableMap.toImmutableMap(
									Function.identity(), board::getCount));
						})));
		this.mrXTravelLog = Objects.requireNonNull(that.getMrXTravelLog());
		this.winner = Objects.requireNonNull(that.getWinner());
		this.availableMoves = Objects.requireNonNull(that.getAvailableMoves());

	}

	public ImmutableBoard(GameSetup setup,
	                      ImmutableMap<Detective, Integer> detectiveLocations,
	                      ImmutableMap<Piece, ImmutableMap<Ticket, Integer>> tickets,
	                      ImmutableList<LogEntry> mrXTravelLog,
	                      ImmutableSet<Piece> winner,
	                      ImmutableSet<Move> availableMoves) {
		this.setup = Objects.requireNonNull(setup);
		this.detectiveLocations = Objects.requireNonNull(detectiveLocations);
		this.tickets = Objects.requireNonNull(tickets);
		this.mrXTravelLog = Objects.requireNonNull(mrXTravelLog);
		this.winner = Objects.requireNonNull(winner);
		this.availableMoves = Objects.requireNonNull(availableMoves);
	}

	@Nonnull @Override public GameSetup getSetup() { return setup; }
	@Nonnull @Override public ImmutableSet<Piece> getPlayers() { return tickets.keySet(); }
	@Nonnull @Override public Optional<Integer> getDetectiveLocation(Detective detective) {
		return Optional.ofNullable(detectiveLocations.get(detective));
	}
	@Nonnull @Override public Optional<TicketBoard> getPlayerTickets(Piece piece) {
		return Optional.ofNullable(tickets.get(piece))
				.map(tickets -> ticket -> tickets.getOrDefault(ticket, 0));
	}
	@Nonnull @Override public ImmutableList<LogEntry> getMrXTravelLog() { return mrXTravelLog; }
	@Nonnull @Override public ImmutableSet<Piece> getWinner() { return winner; }
	@Nonnull @Override public ImmutableSet<Move> getAvailableMoves() { return availableMoves; }
	@Override public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ImmutableBoard that = (ImmutableBoard) o;
		return Objects.equals(setup, that.setup) &&
				Objects.equals(detectiveLocations, that.detectiveLocations) &&
				Objects.equals(tickets, that.tickets) &&
				Objects.equals(mrXTravelLog, that.mrXTravelLog) &&
				Objects.equals(winner, that.winner) &&
				Objects.equals(availableMoves, that.availableMoves);
	}
	@Override public int hashCode() {
		return Objects.hash(setup, detectiveLocations, tickets, mrXTravelLog, winner,
				availableMoves);
	}
}
