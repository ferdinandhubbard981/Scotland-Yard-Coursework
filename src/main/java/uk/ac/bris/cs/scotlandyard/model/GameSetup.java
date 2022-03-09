package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;

import java.io.Serializable;
import java.util.Objects;

import javax.annotation.Nonnull;

import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Transport;

/**
 * A POJO containing the ScotlandYard game graph and the MrX's reveal moves
 */
public final class GameSetup implements Serializable {
	private static final long serialVersionUID = -4214739769363149939L;

	/**
	 * The graph where nodes are stations in integers and edges as sets of transports
	 */
	public final ImmutableValueGraph<Integer, ImmutableSet<Transport>> graph;
	/**
	 * MrX reveal moves; false is hidden, true is reveal
	 */
	public final ImmutableList<Boolean> moves;
	public GameSetup(@Nonnull ImmutableValueGraph<Integer, ImmutableSet<Transport>> graph,
	                 @Nonnull ImmutableList<Boolean> moves) {
		this.graph = Objects.requireNonNull(graph);
		this.moves = Objects.requireNonNull(moves);
	}
	@Override public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		return Objects.equals(graph, ((GameSetup) o).graph) &&
				Objects.equals(moves, ((GameSetup) o).moves);
	}
	@Override public int hashCode() { return Objects.hash(graph, moves); }
}
