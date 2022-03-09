package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Function;

import javax.annotation.Nonnull;

import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;

import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket.DOUBLE;

/**
 * Represents a move in the ScotlandYard game. A move is an action where a player picks a ticket
 * from the draw-pile or ticketboard and uses it to give to another node.
 */
public interface Move extends Serializable {
	/**
	 * @return the player that made this move
	 */
	@Nonnull Piece commencedBy();
	/**
	 * @return the tickets used to complete this move.
	 */
	@Nonnull Iterable<Ticket> tickets();
	/**
	 * @return the source of this move (i.e where the player is at before the move)
	 */
	int source();
	/**
	 * Visits all possible move types that implement {@link Move}
	 *
	 * @param visitor the visitor
	 * @param <T> the return type. Use {@link Void} and return null or your custom Unit type for
	 * side effects.
	 * @return the return value
	 */
	<T> T accept(Visitor<T> visitor);

	/**
	 * A generic visitor for use with the {@link Move#accept(Visitor)} method.
	 *
	 * @param <T> the resulting type; use {@link Void} if not returning (i.e side effect)
	 */
	interface Visitor<T> {
		/**
		 * @param move the single move
		 * @return the return value
		 */
		T visit(SingleMove move);
		/**
		 * @param move the double move
		 * @return the return value
		 */
		T visit(DoubleMove move);
	}

	/**
	 * A visitor adapter that takes two {@link Function}s and turn them into visitors.
	 *
	 * @param <T> the return value, see {@link Visitor}
	 */
	final class FunctionalVisitor<T> implements Visitor<T> {
		private final Function<SingleMove, T> smf;
		private final Function<DoubleMove, T> dmf;
		public FunctionalVisitor(Function<SingleMove, T> smf, Function<DoubleMove, T> dmf) {
			this.smf = smf;
			this.dmf = dmf;
		}
		@Override public T visit(SingleMove m) { return smf.apply(m); }
		@Override public T visit(DoubleMove m) { return dmf.apply(m); }
	}

	/**
	 * A POJO representing a single move with one ticket
	 */
	final class SingleMove implements Move {
		private static final long serialVersionUID = -1349204443558253282L;
		private final Piece piece;
		private final int source;
		/**
		 * The ticket
		 */
		public final Ticket ticket;
		/**
		 * The destination
		 */
		public final int destination;
		public SingleMove(@Nonnull Piece piece, int source,
		                  @Nonnull Ticket ticket, int destination) {
			this.piece = Objects.requireNonNull(piece);
			this.source = source;
			this.ticket = Objects.requireNonNull(ticket);
			this.destination = destination;
		}
		@Nonnull @Override public Piece commencedBy() { return piece; }
		@Nonnull @Override public Iterable<Ticket> tickets() { return ImmutableList.of(ticket); }
		@Override public int source() { return source; }
		@Override public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
		@Override public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			SingleMove that = (SingleMove) o;
			return source == that.source && destination == that.destination &&
					piece == that.piece && ticket == that.ticket;
		}
		@Override public int hashCode() { return Objects.hash(piece, ticket, destination); }
		@Override public String toString() {
			return ticket.name() + "(" + piece + "@" + source + ", " + destination + ")";
		}
	}

	/**
	 * A POJO representing a double move with two tickets
	 */
	final class DoubleMove implements Move {
		private static final long serialVersionUID = 4836583762114320876L;
		private final Piece piece;
		private final int source;
		/**
		 * The first ticket
		 */
		public final Ticket ticket1;
		/**
		 * The first destination
		 */
		public final int destination1;
		/**
		 * The second ticket
		 */
		public final Ticket ticket2;
		/**
		 * The second destination
		 */
		public final int destination2;
		public DoubleMove(@Nonnull Piece piece, int source,
		                  @Nonnull Ticket ticket1, int destination1,
		                  @Nonnull Ticket ticket2, int destination2) {
			this.piece = Objects.requireNonNull(piece);
			this.source = source;
			this.ticket1 = Objects.requireNonNull(ticket1);
			this.destination1 = destination1;
			this.ticket2 = Objects.requireNonNull(ticket2);
			this.destination2 = destination2;
		}
		@Nonnull @Override public Piece commencedBy() { return piece; }
		@Nonnull @Override
		public Iterable<Ticket> tickets() { return ImmutableList.of(ticket1, ticket2, DOUBLE);}
		@Override public int source() { return source; }
		@Override public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
		@Override public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			DoubleMove that = (DoubleMove) o;
			return piece == that.piece && source == that.source &&
					ticket1 == that.ticket1 && destination1 == that.destination1 &&
					ticket2 == that.ticket2 && destination2 == that.destination2;
		}
		@Override public int hashCode() {
			return Objects.hash(piece, ticket1, destination1, ticket2, destination2);
		}
		@Override public String toString() {
			return "x2(" + piece + "@" + source + ", " + ticket1 + ", " + destination1 + ", " + ticket2 + ", " + destination2 + ")";
		}
	}
}
