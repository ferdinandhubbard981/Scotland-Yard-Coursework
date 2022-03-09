package uk.ac.bris.cs.scotlandyard.ui.controller;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import uk.ac.bris.cs.fxkit.BindFXML;
import uk.ac.bris.cs.fxkit.Controller;
import uk.ac.bris.cs.scotlandyard.ResourceManager;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Model;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.Piece.MrX;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;
import uk.ac.bris.cs.scotlandyard.ui.GameControl;
import uk.ac.bris.cs.scotlandyard.ui.model.ModelProperty;

/**
 * Controller for the ticket counter.<br> Not required for the coursework.
 */
@BindFXML("layout/Players.fxml")
final class TicketBoardController implements Controller, GameControl {

	@FXML private VBox root;
	@FXML private VBox playerContainer;

	private final ResourceManager manager;
	private final Map<Piece, PlayerView> controllers = new HashMap<>();

	TicketBoardController(ResourceManager manager) {
		this.manager = manager;
		Controller.bind(this);
		root.managedProperty().bind(root.visibleProperty());
	}

	@Override public void onGameAttach(Model model, ModelProperty config,
	                                   Consumer<ImmutableSet<Piece>> timeout) {
		config.everyone().forEach(player -> controllers.computeIfAbsent(player.piece(),
				c -> {
					PlayerView v = new PlayerView(manager, c, player.name().orElse(""));
					VBox.setVgrow(v.root(), Priority.ALWAYS);
					playerContainer.getChildren().add(v.root());
					return v;
				}).update(model.getCurrentBoard()));
	}

	@Override
	public void onGameDetached() {
		controllers.clear();
		playerContainer.getChildren().clear();
		GameControl.super.onGameDetached();
	}

	@Override public void onModelChanged(@Nonnull Board board, @Nonnull Event event) {
		controllers.values().forEach(c -> c.update(board));
	}

	@Override public Parent root() { return root; }

	@BindFXML("layout/Ticket.fxml") static class TicketView implements Controller {
		@FXML private HBox root;
		@FXML private ImageView ticket;
		@FXML private Label count;
		@FXML private Label bar;
		private TicketView(Ticket ticket, ResourceManager manager) {
			Controller.bind(this);
			this.ticket.setImage(manager.getTicket(ticket));
		}
		void updateCount(int count) {
			this.count.setText(String.format("%3d", count));
			if (this.bar.getText().length() != count) {
				this.bar.setText(String.join("", Collections.nCopies(count, "|")));
			}
		}
		@Override public Parent root() { return root; }
	}

	@BindFXML("layout/Player.fxml") static class PlayerView implements Controller {
		@FXML private VBox root;
		@FXML private Label label;
		@FXML private Pane tickets;
		private final Piece piece;
		private final String name;
		private final Map<Ticket, TicketView> ticketMap = new HashMap<>();
		private final ResourceManager manager;
		private PlayerView(ResourceManager manager, Piece piece, String name) {
			this.manager = manager;
			this.piece = piece;
			this.name = name;
			Controller.bind(this);
			var c = Color.web(piece.webColour()).darker().saturate();
			root.setStyle("-fx-background-color: " +
					"linear-gradient(from 100% 100% to 0% 0%," +
					"rgb(" + c.getRed() * 255 + ", " + c.getGreen() * 255 + ", " + c.getBlue() * 255 + " ), " +
					"#2a2a2a )");
		}
		void update(Board view) {
			label.setText(Strings.isNullOrEmpty(name) ? piece.toString() : name);
			Stream.of(Ticket.values())
					.filter(t -> hasTicket(piece, t))
					.forEachOrdered(ticket ->
							ticketMap.computeIfAbsent(ticket, t -> {
								TicketView controller = new TicketView(t, manager);
								this.tickets.getChildren().add(controller.root());
								return controller;
							}).updateCount(view.getPlayerTickets(piece)
									.map(x -> x.getCount(ticket))
									.orElseThrow(AssertionError::new)));
		}
		private static boolean hasTicket(Piece piece, Ticket ticket) {
			return piece == MrX.MRX
					? ScotlandYard.MRX_TICKETS.contains(ticket)
					: ScotlandYard.DETECTIVE_TICKETS.contains(ticket);
		}
		@Override public Parent root() { return root; }
	}

}
