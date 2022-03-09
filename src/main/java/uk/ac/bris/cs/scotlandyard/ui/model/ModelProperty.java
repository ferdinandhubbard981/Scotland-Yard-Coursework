package uk.ac.bris.cs.scotlandyard.ui.model;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;

import java.time.Duration;
import java.util.Objects;

import io.atlassian.fugue.Option;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import uk.ac.bris.cs.scotlandyard.ResourceManager;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Transport;

public class ModelProperty {

	private final ObjectProperty<Duration> timeout = new SimpleObjectProperty<>();
	private final ObservableList<Boolean> revealRounds = FXCollections.observableArrayList();

	private final ObjectProperty<Option<Ai>> mrXAi =
			new SimpleObjectProperty<>(Option.none());
	private final ObjectProperty<Option<Ai>> detectivesAi =
			new SimpleObjectProperty<>(Option.none());

	private final ObservableList<PlayerProperty<? super Piece>> players =
			FXCollections.observableArrayList();
	private final ObjectProperty<ImmutableValueGraph<Integer, ImmutableSet<Transport>>> graph =
			new SimpleObjectProperty<>();

	public ModelProperty(Duration timeout,
	                     ImmutableList<Boolean> revealRounds,
	                     ImmutableList<? extends PlayerProperty<? super Piece>> players,
	                     ImmutableValueGraph<Integer, ImmutableSet<Transport>> graph,
	                     Option<Ai> mrXAi,
	                     Option<Ai> detectivesAi) {
		this.timeout.set(Objects.requireNonNull(timeout));
		this.revealRounds.addAll(Objects.requireNonNull(revealRounds));
		this.players.addAll(Objects.requireNonNull(players));
		this.graph.set(Objects.requireNonNull(graph));
		this.mrXAi.set(Objects.requireNonNull(mrXAi));
		this.detectivesAi.set(Objects.requireNonNull(detectivesAi));
	}

	public static ModelProperty createDefault(ResourceManager manager) {
		return new ModelProperty(Duration.ofSeconds(30), ScotlandYard.STANDARD24MOVES,
				ScotlandYard.ALL_PIECES.stream()
						.map(PlayerProperty::new)
						.collect(ImmutableList.toImmutableList()),
				manager.getGraph(),
				Option.none(), Option.none()
		);
	}

	public ObjectProperty<Duration> timeoutProperty() { return timeout; }
	public ObservableList<Boolean> revealRounds() { return revealRounds; }

	public ObjectProperty<ImmutableValueGraph<Integer, ImmutableSet<Transport>>> graphProperty() {
		return graph;
	}

	public PlayerProperty<? super Piece> mrX() {
		return players.stream()
				.filter(PlayerProperty::mrX)
				.findFirst().orElseThrow();
	}

	public ImmutableList<PlayerProperty<? super Piece>> detectives() {
		return players.stream()
				.filter(PlayerProperty::detective)
				.collect(ImmutableList.toImmutableList());
	}

	public ImmutableList<PlayerProperty<? super Piece>> everyone() {
		return ImmutableList.copyOf(players);
	}

	public Option<Ai> getMrXAi() { return mrXAi.get(); }
	public ObjectProperty<Option<Ai>> mrXAiProperty() { return mrXAi; }
	public Option<Ai> getDetectivesAi() { return detectivesAi.get(); }
	public ObjectProperty<Option<Ai>> detectivesAiProperty() { return detectivesAi; }


	@Override public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("timeout", timeout)
				.add("revealRounds", revealRounds)
				.add("players", players).toString();
	}
}
