package uk.ac.bris.cs.scotlandyard.ui.controller;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import io.atlassian.fugue.Option;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import uk.ac.bris.cs.fxkit.BindFXML;
import uk.ac.bris.cs.fxkit.Controller;
import uk.ac.bris.cs.scotlandyard.ResourceManager;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.LogEntry;
import uk.ac.bris.cs.scotlandyard.model.Model;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;
import uk.ac.bris.cs.scotlandyard.ui.GameControl;
import uk.ac.bris.cs.scotlandyard.ui.model.ModelProperty;

/**
 * Controller for travel log the records Mr.X move.<br> Not required for the coursework.
 */
@BindFXML("layout/TravelLog.fxml")
public final class TravelLogController implements Controller, GameControl {

	@FXML private StackPane root;
	@FXML private TableView<RoundEntry> logTable;
	@FXML private TableColumn<RoundEntry, RoundEntry> logRound;
	@FXML private TableColumn<RoundEntry, Ticket> logTicket;
	@FXML private TableColumn<RoundEntry, String> logLocation;

	private final ObservableList<RoundEntry> entries = FXCollections.observableArrayList();

	TravelLogController(ResourceManager manager) {
		Controller.bind(this);
		logRound.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
		logRound.setCellFactory(l -> new TableCell<>() {
			@Override protected void updateItem(RoundEntry item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) setGraphic(null);
				else {
					Label label = new Label(item.round + "");
					label.setAlignment(Pos.CENTER);
					label.setPrefSize(30, 30);
					label.setMinSize(30, 30);
					if (item.reveal) label.setStyle("" +
							"    -fx-border-radius: 50%;\n" +
							"    -fx-border-color: white;\n" +
							"    -fx-border-style: solid;\n" +
							"    -fx-border-width: 2px;");
					setGraphic(label);
				}
			}
		});
		logTicket.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().entry.map(LogEntry::ticket).getOrNull()));
		logTicket.setCellFactory(param -> new TicketTableCell(manager));
		logLocation.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().entry
				.flatMap(a -> Option.fromOptional(a.location()))
				.map(Objects::toString).getOrElse("")));

		root.managedProperty().bind(root.visibleProperty());
		logTable.setItems(entries);
	}

	@Override public void onGameAttach(Model model, ModelProperty config,
	                                   Consumer<ImmutableSet<Piece>> timeout) {
		update(model.getCurrentBoard());
	}

	private void update(Board board) {
		var rounds = board.getSetup().moves;
		var log = board.getMrXTravelLog();
		entries.setAll(Streams.mapWithIndex(rounds.stream(), (reveal, i) -> new RoundEntry(
				(int) (i + 1), reveal,
				i >= log.size() ? Option.none() : Option.some(log.get((int) i))))
				.collect(Collectors.toList()));
	}

	@Override
	public void onModelChanged(@Nonnull Board board, @Nonnull Event event) { update(board); }
	@Override public Parent root() { return root; }

	private static class RoundEntry {
		private final int round;
		private final boolean reveal;
		private final Option<LogEntry> entry;
		public RoundEntry(int round, boolean reveal, Option<LogEntry> entry) {
			this.round = round;
			this.reveal = reveal;
			this.entry = entry;
		}
	}

	private static final class TicketTableCell extends TableCell<RoundEntry, Ticket> {

		private final ImageView view = new ImageView();
		private final ResourceManager manager;

		TicketTableCell(ResourceManager manager) {
			this.manager = manager;
			view.setFitWidth(50);
			view.setPreserveRatio(true);
			setGraphic(view);
		}

		@Override
		protected void updateItem(Ticket item, boolean empty) {
			super.updateItem(item, empty);
			if (empty || item == null) setGraphic(null);
			else {
				setGraphic(view);
				view.setImage(manager.getTicket(item));
			}
		}

	}
}
