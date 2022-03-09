package uk.ac.bris.cs.scotlandyard.ui.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.Piece.MrX;
import uk.ac.bris.cs.scotlandyard.model.Player;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;

public class PlayerProperty<T extends Piece> {

	public static final int RANDOM = -1;

	private final BooleanProperty enabled = new SimpleBooleanProperty(true);
	private final ObjectProperty<T> piece = new SimpleObjectProperty<>();
	private final StringProperty name = new SimpleStringProperty();
	private final IntegerProperty location = new SimpleIntegerProperty(RANDOM);
	private final ObservableList<TicketProperty> tickets = FXCollections
			.observableArrayList(param -> new Observable[]{param.ticket, param.count});

	public PlayerProperty(T piece) {
		this.piece.set(piece);
		Map<Ticket, Integer> map = new TreeMap<>(piece == MrX.MRX ?
				ScotlandYard.defaultMrXTickets() :
				ScotlandYard.defaultDetectiveTickets());
		map.forEach((t, c) -> tickets.add(new TicketProperty(t, c)));
		this.enabled.set(true);
	}

	public boolean enabled() {
		return enabled.get();
	}

	public BooleanProperty enabledProperty() {
		return enabled;
	}

	public T piece() {
		return piece.get();
	}


	public boolean mrX() {
		return piece.get().isMrX();
	}

	public boolean detective() {
		return piece.get().isDetective();
	}

	public ObjectProperty<T> pieceProperty() {
		return piece;
	}

	public Optional<String> name() {
		return Optional.ofNullable(name.get())
				.flatMap(s -> Strings.isNullOrEmpty(s) ? Optional.empty() : Optional.of(s));
	}

	public StringProperty nameProperty() {
		return name;
	}

	public int location() {
		return location.get();
	}

	public IntegerProperty locationProperty() {
		return location;
	}

	public boolean randomLocation() {
		return location.isEqualTo(RANDOM).get();
	}


	public ObservableList<TicketProperty> tickets() {
		return tickets;
	}

	public ImmutableMap<Ticket, Integer> ticketsAsMap() {
		return tickets().stream().collect(
				ImmutableMap.toImmutableMap(TicketProperty::ticket, TicketProperty::count));
	}

	public List<Property<?>> observables() {
		return Arrays.asList(enabled, piece, name, location);
	}

//	public static List<PlayerProperty<Piece>> allDetectives(
//			Collection<PlayerProperty<Piece>> configs) {
//		return configs.stream().filter(PlayerProperty::detective).collect(Collectors.toList());
//	}
//
//	public static Optional<PlayerProperty<Piece>> mrX(Collection<PlayerProperty<Piece>> configs) {
//		return configs.stream().filter(PlayerProperty::mrX).findFirst();
//	}

	public Player asPlayer() {
		return new Player(piece(), ticketsAsMap(), location());
	}


	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("enabled", enabled).add("colour", piece)
				.add("name", name).add("location", location).toString();
	}
}
