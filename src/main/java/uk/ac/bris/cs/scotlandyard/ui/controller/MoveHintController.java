package uk.ac.bris.cs.scotlandyard.ui.controller;

import com.google.common.collect.ImmutableList;

import org.fxmisc.easybind.EasyBind;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;
import javafx.util.Duration;
import uk.ac.bris.cs.fxkit.BindFXML;
import uk.ac.bris.cs.fxkit.Controller;
import uk.ac.bris.cs.scotlandyard.ResourceManager;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Move.FunctionalVisitor;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;
import uk.ac.bris.cs.scotlandyard.ui.Utils;

/**
 * Controller for move hints with highlighting.<br> Not required for the coursework.
 */
@BindFXML("layout/MoveHint.fxml") final class MoveHintController implements Controller {

	private static final String HIGHLIGHTED = "highlighted";

	private final BooleanProperty highlight = new SimpleBooleanProperty();

	private final ResourceManager manager;
	private final MapController board;

	private final List<Move> moves = new ArrayList<>();

	@FXML private Pane root;
	@FXML private Circle piece;
	private boolean discarded = false;

	MoveHintController(ResourceManager manager, MapController board,
	                   int source, int target,
	                   Consumer<Move> moveConsumer) {
		Controller.bind(this);
		this.manager = manager;
		this.board = board;

		EasyBind.subscribe(highlight, v -> {
			ObservableList<String> styles = piece.getStyleClass();
			Function<String, Boolean> function = (v ? styles::add : styles::remove);
			function.apply(HIGHLIGHTED);
		});

		if (moveConsumer != null) setupMoveOptions(moveConsumer);

		piece.setOnMouseEntered(e -> Utils.scaleTo(piece, 1.5));

		piece.setOnMouseExited(e -> Utils.scaleTo(piece, 1));

		// Platform.runLater(() -> {
		Point2D location = board.coordinateAtNode(target);
		piece.setTranslateX(location.getX());
		piece.setTranslateY(location.getY());
		piece.setRadius(30 * ScotlandYard.MAP_SCALE);
		piece.setScaleX(0.5);
		piece.setScaleY(0.5);
		piece.setOpacity(0);
		Platform.runLater(() -> {
			var ms =
					Utils.scale(board.coordinateAtNode(source).distance(board.coordinateAtNode(target)),
							0, board.maxLength(), 0, 2000);
			Duration d = Duration.millis(ms);
			Utils.fadeTo(piece, d, 1);
			Utils.scaleTo(piece, d, 1);
		});
	}

	private ContextMenu lastMenu = null;
	private void setupMoveOptions(Consumer<Move> moveConsumer) {
		piece.setOnMouseClicked(e -> {
			if (lastMenu != null) lastMenu.hide();
			lastMenu = new ContextMenu();
			lastMenu.getStyleClass().add("move-menu");
			for (Move move : moves) {
				MenuItem item = new MenuItem();
				Node graphic = move.accept(new FunctionalVisitor<>(
						sm -> mkColouredBox(sm.commencedBy(),
								new ImageView(manager.getTicket((sm).ticket))),
						dm -> mkColouredBox(dm.commencedBy(),
								new ImageView(manager.getTicket(dm.ticket1)),
								new ImageView(manager.getTicket(dm.ticket2)))));
				item.setGraphic(graphic);
				EasyBind.subscribe(graphic.hoverProperty(), hover -> {
					if (discarded) return;
					move.accept(new FunctionalVisitor<>(
							m -> ImmutableList.of(m.destination),
							m -> ImmutableList.of(m.destination1, m.destination2)))
							.forEach(d -> board.hintAt(d).highlight.set(true));
				});
				item.setOnAction(a -> moveConsumer.accept(move));
				lastMenu.getItems().add(item);

			}
			Point2D p = piece.localToScreen(piece.getCenterX(), piece.getCenterY());
			lastMenu.show(piece, p.getX(), p.getY());
			lastMenu.setOnHidden(a -> {
				lastMenu = null;
				board.allHints().forEach(c -> c.highlight.set(false));
			});
		});
	}

	private Node mkColouredBox(Piece piece, Node... nodes) {
		var circle = new Circle(10f, Color.web(piece.webColour()));
		circle.setStroke(Color.WHITE);
		circle.setStrokeType(StrokeType.OUTSIDE);
		circle.setStrokeWidth(6f);
		var box = new HBox(4, circle);
		box.setAlignment(Pos.CENTER);
		box.getChildren().addAll(nodes);
		return box;

	}

	public void discard() { this.discarded = true; }

	void addMove(Move move) { this.moves.add(move); }

	@Override public Parent root() { return root; }


}
