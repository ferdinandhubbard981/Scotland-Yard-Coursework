package uk.ac.bris.cs.scotlandyard.ui.controller;

import org.fxmisc.easybind.EasyBind;

import java.net.URL;
import java.util.OptionalInt;
import java.util.ResourceBundle;

import io.atlassian.fugue.Option;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import uk.ac.bris.cs.fxkit.BindFXML;
import uk.ac.bris.cs.fxkit.Controller;
import uk.ac.bris.cs.fxkit.interpolator.DecelerateInterpolator;
import uk.ac.bris.cs.scotlandyard.ResourceManager;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;
import uk.ac.bris.cs.scotlandyard.ui.Utils;

@BindFXML("layout/Counter.fxml") final class CounterController implements Controller {

	private static final double DEFAULT_OPACITY = 1;
	private static final double HOVER_OPACITY = 0.2;

	@FXML private VBox root;
	@FXML private Circle piece;

	private final ResourceManager manager;
	private final BooleanProperty animated;
	private final SimpleIntegerProperty locationProperty = new SimpleIntegerProperty();

	CounterController(ResourceManager manager,
	                  BooleanProperty animated,
	                  Piece piece,
	                  int location) {
		this.manager = manager;
		this.animated = animated;
		this.locationProperty.set(location);
		Controller.bind(this);
		Color color = Color.web(piece.webColour()).desaturate();
		this.piece.setRadius((piece.isMrX() ? 1.2 : 0.8) * ScotlandYard.MAP_NODE_SIZE);
		this.piece.setFill(color);
		this.piece.setOpacity(DEFAULT_OPACITY);
		this.piece.setOnMouseEntered(e -> {
			Utils.fadeTo(this.piece, HOVER_OPACITY);
			Utils.scaleTo(this.piece, 2);
		});
		this.piece.setOnMouseExited(e -> {
			Utils.fadeTo(this.piece, DEFAULT_OPACITY);
			Utils.scaleTo(this.piece, 1);
		});
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		EasyBind.subscribe(root.layoutBoundsProperty(), b -> updateLocation());
	}

	void updateLocation() {
		// TODO due to timing differences, counter translation might not be
		// compensated relative to itself in some cases; this could be fixed by
		// binding to width and height properties of the current root
		piece.setVisible(false);
		location().ifPresent(location -> {
			piece.setVisible(true);
			Point2D node = positionAtNode(location);
			root.setTranslateX(node.getX());
			root.setTranslateY(node.getY());
		});
	}

	void animateTicketMove(int destination, Option<Runnable> callback) {
		if (!animated.get()
				|| destination == 0
				|| location().isEmpty()
				|| destination == location().orElse(0)) {
			callback.forEach(Runnable::run);
			return;
		}
		Point2D from = positionAtNode(location().getAsInt());
		Point2D to = positionAtNode(destination);
		TranslateTransition tt = new TranslateTransition(Duration.millis(250), root);
		tt.setInterpolator(new DecelerateInterpolator(2f));
		tt.setFromX(from.getX());
		tt.setToX(from.getY());
		tt.setToX(to.getX());
		tt.setToY(to.getY());
		tt.play();
		tt.setOnFinished(e -> Platform.runLater(() -> callback.forEach(Runnable::run)));
	}
	public void animateVisibility(boolean visible) {
		if (!animated.get()) root.setVisible(visible);
		else {
			Utils.scaleTo(root, visible ? 1 : 2);
			Utils.fadeTo(root, visible ? 1 : 0);
		}
	}

	OptionalInt location() {
		int location = locationProperty.get();
		return location == 0 ? OptionalInt.empty() : OptionalInt.of(location);
	}

	public void location(int location) {
		locationProperty.set(location);
	}

	private Point2D positionAtNode(int node) {
		// offset relative to the centre
		return manager.coordinateAtNode(node)
				.subtract(root().getLayoutBounds().getWidth() / 2,
						root().getLayoutBounds().getHeight() / 2);
	}

	@Override
	public Parent root() {
		return root;
	}
}
