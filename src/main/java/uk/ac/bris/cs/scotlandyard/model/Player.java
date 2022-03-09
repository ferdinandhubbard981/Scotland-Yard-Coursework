package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Objects;

import javax.annotation.Nonnull;

import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;

/**
 * A POJO representing an immutable player of the ScotlandYard game.
 * Each player contains the {@link Piece} (coloured counter), {@link Ticket}s, and the location.
 */
public final class Player {
	private final Piece piece;
	private final ImmutableMap<Ticket, Integer> tickets;
	private final int location;

	public Player(@Nonnull Piece piece,
	              @Nonnull ImmutableMap<Ticket, Integer> tickets,
	              int location) {
		this.piece = Objects.requireNonNull(piece);
		this.tickets = Objects.requireNonNull(tickets);
		this.location = location;
	}
	/**
	 * @return the piece
	 */
	public Piece piece() { return piece; }
	/**
	 * @return whether the player is MrX
	 */
	public boolean isMrX() { return piece.isMrX(); }
	/**
	 * @return whether the player is a detective
	 */
	public boolean isDetective() { return piece.isDetective(); }
	/**
	 * @return the ticket
	 */
	@Nonnull public ImmutableMap<Ticket, Integer> tickets() { return tickets; }
	/**
	 * @return the location
	 */
	public int location() { return location; }
	/**
	 * @param ticket the ticket
	 * @return whether the player has the given ticket
	 */
	public boolean has(@Nonnull Ticket ticket) {
		return tickets.getOrDefault(Objects.requireNonNull(ticket), 0) != 0;
	}
	/**
	 * @param ticket the ticket
	 * @param count the required count
	 * @return whether the player has &gt;= the required numbers of the given ticket
	 */
	public boolean hasAtLeast(@Nonnull Ticket ticket, int count) {
		return tickets.getOrDefault(Objects.requireNonNull(ticket), 0) >= count;
	}
	/**
	 * See {@link #give(Ticket)}
	 *
	 * @param tickets the tickets
	 * @return a new player with one more of the given tickets
	 */
	@Nonnull public Player give(@Nonnull Iterable<Ticket> tickets) {
		var x = this;
		for (Ticket t : tickets) x = x.give(t);
		return x;
	}
	/**
	 * @param ticket the ticket
	 * @return a new player with one more of the given ticket
	 */
	@Nonnull public Player give(@Nonnull Ticket ticket) {
		var map = new HashMap<>(tickets);
		map.computeIfPresent(ticket, (t, n) -> n + 1);
		return new Player(piece, ImmutableMap.copyOf(map), location);
	}
	/**
	 * See {@link #use(Ticket)}
	 *
	 * @param tickets the tickets
	 * @return a new player with one less of the given tickets
	 */
	@Nonnull public Player use(@Nonnull Iterable<Ticket> tickets) {
		var x = this;
		for (Ticket t : tickets) x = x.use(t);
		return x;
	}
	/**
	 * @param ticket the ticket
	 * @return a new player with one less of the given ticket
	 */
	@Nonnull public Player use(@Nonnull Ticket ticket) {
		if (!has(ticket))
			throw new IllegalArgumentException("No " + ticket + " remaining");
		var map = new HashMap<>(tickets);
		map.computeIfPresent(ticket, (t, n) -> n - 1);
		return new Player(piece, ImmutableMap.copyOf(map), location);
	}
	/**
	 * @param newLocation the location
	 * @return a new player at the given location
	 */
	@Nonnull public Player at(int newLocation) { return new Player(piece, tickets, newLocation); }
	@Override public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Player that = (Player) o;
		return location == that.location && piece == that.piece &&
				Objects.equals(tickets, that.tickets);
	}
	@Override public int hashCode() { return Objects.hash(piece, tickets, location); }
	@Override public String toString() { return piece + "@" + location + "(" + tickets + ")"; }
}
