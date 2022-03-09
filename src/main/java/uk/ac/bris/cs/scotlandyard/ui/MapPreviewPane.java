package uk.ac.bris.cs.scotlandyard.ui;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javafx.animation.TranslateTransition;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import uk.ac.bris.cs.fxkit.interpolator.DecelerateInterpolator;
import uk.ac.bris.cs.scotlandyard.ResourceManager;
import uk.ac.bris.cs.scotlandyard.ResourceManager.ImageResource;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.Piece.Detective;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

/**
 * Lightweight map with annotation, preview and circling capabilities.
 * <br>
 * Not required for the coursework.
 */
public class MapPreviewPane extends Pane {

	private final Pane annotations = new Pane();
	private final Pane mask = new Pane();
	private final ResourceManager manager;

	public MapPreviewPane(ResourceManager manager) {
		this.manager = manager;
		ImageView mapView = new ImageView();
		Pane shadow = new Pane();
		getChildren().addAll(mapView, shadow, annotations);
		Image image = manager.getImage(ImageResource.MAP);
		mapView.setImage(image);
		shadow.setStyle("-fx-background-color: rgba(0,0, 0, 0.5)");
		setMinSize(image.getWidth(), image.getHeight());
//		resize(image.getWidth(), image.getHeight());
		shadow.setPrefSize(image.getWidth(), image.getHeight());
		annotations.setPrefSize(image.getWidth(), image.getHeight());
		mask.setBlendMode(BlendMode.OVERLAY);
		shadow.getChildren().add(mask);
	}

	public void reset() {
		clearAnnotations();
		clearHighlights();
	}

	private void clearAnnotations() {
		playerMap.clear();
		annotations.getChildren().clear();
	}

	private final Map<Piece, Node> playerMap = new HashMap<>();

	public void annotate(Integer location, Piece piece) {
		if (location == null || location == -1) {

			Node node = playerMap.get(piece);
			if (node != null) {
				annotations.getChildren().remove(node);
				playerMap.remove(piece);
			}
			return;
		}

		Node node = playerMap.computeIfAbsent(piece, c -> {
			Circle circle = new Circle(30);
			circle.setStroke(Color.WHITE);
			circle.setStrokeWidth(5);
			circle.setFill(Color.web(piece.webColour()));
			annotations.getChildren().addAll(circle);
			return circle;
		});
		Point2D point = manager.coordinateAtNode(location);

		if (Utils.translate(node).equals(Point2D.ZERO)) {
			Utils.translate(node, point);
			return;
		}

		if (point.distance(node.getTranslateX(), node.getTranslateY()) > 1) {
			TranslateTransition t = new TranslateTransition(Duration.millis(120), node);
			t.setInterpolator(DecelerateInterpolator.DEFAULT);
			t.setToX(point.getX());
			t.setToY(point.getY());
			t.play();
		} else {
			Utils.translate(node, point);
		}

	}

	private void clearHighlights() {
		mask.setVisible(false);
	}

	public void highlight(Collection<Integer> locations) {
		if (locations.isEmpty()) {
			clearHighlights();
			return;
		}

		mask.getChildren().clear();
		for (Integer location : locations) {
			Point2D point = manager.coordinateAtNode(location);
			Circle circle = new Circle(ScotlandYard.MAP_NODE_SIZE);
			circle.setFill(Color.web(Detective.WHITE.webColour()));
			circle.setTranslateX(point.getX());
			circle.setTranslateY(point.getY());
			circle.setOpacity(0.8);
			circle.setStyle("-fx-effect: dropshadow(two-pass-box, white, 50, 0.5, 0, 0)");
			mask.getChildren().add(circle);
		}

		mask.setVisible(true);

	}


}
