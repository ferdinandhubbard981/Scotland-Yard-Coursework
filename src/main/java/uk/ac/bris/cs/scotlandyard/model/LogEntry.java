package uk.ac.bris.cs.scotlandyard.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;

/**
 * A POJO representing log entries of the MrX's travel log.
 * <br>
 * Use the static factory methods {@link #hidden(Ticket)} and {@link #reveal(Ticket, int)} to
 * create new instances.
 */
public final class LogEntry implements Serializable {
	private static final long serialVersionUID = -6468835796153329259L;
	// because Java's stupid Optional isn't intend to be used as a field...
	private static final int HIDDEN = -1;
	private final Ticket ticket;
	private final int location;
	/**
	 * @param ticket the ticket used in this entry
	 * @return a log entry of a hidden round for Mrx
	 */
	public static LogEntry hidden(
			@Nonnull Ticket ticket) { return new LogEntry(ticket, HIDDEN); }
	/**
	 * @param ticket the ticket used in this entry
	 * @param location the location MrX is at during this reveal round
	 * @return a log entry of a reveal round for Mrx
	 */
	public static LogEntry reveal(@Nonnull Ticket ticket, int location) {
		if (location == HIDDEN) throw new IllegalArgumentException();
		return new LogEntry(ticket, location);
	}
	private LogEntry(@Nonnull Ticket ticket, int location) {
		this.ticket = Objects.requireNonNull(ticket);
		this.location = location;
	}
	/**
	 * @return the ticket in this log entry
	 */
	public Ticket ticket() { return ticket; }
	/**
	 * @return the location in this log entry, empty means MrX's location is hidden
	 */
	public Optional<Integer> location() {
		return location == HIDDEN ? Optional.empty() : Optional.of(location);
	}
	@Override public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LogEntry logEntry = (LogEntry) o;
		return location == logEntry.location && ticket == logEntry.ticket;
	}
	@Override public int hashCode() { return Objects.hash(ticket, location); }
}
