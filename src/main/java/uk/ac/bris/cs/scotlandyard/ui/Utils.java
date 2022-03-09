package uk.ac.bris.cs.scotlandyard.ui;

import com.google.common.base.Throwables;

import java.io.IOException;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.Duration;
import uk.ac.bris.cs.fxkit.Controller;
import uk.ac.bris.cs.fxkit.interpolator.DecelerateInterpolator;
import uk.ac.bris.cs.scotlandyard.ResourceManager;

/**
 * Not required for the coursework.
 */
public class Utils {

	public static void translate(Node node, Point2D point2D) {
		node.setTranslateX(point2D.getX());
		node.setTranslateY(point2D.getY());
	}

	public static double scale(double x, double xMin, double xMax,
	                           double min, double max) {
		return ((max - min) * (x - xMin) / (xMax - xMin)) + min;
	}

	public static void scaleTo(Node node, double value) {
		scaleTo(node, Duration.millis(250), value);
	}

	public static void fadeTo(Node node, double value) {
		fadeTo(node, Duration.millis(250), value);
	}

	public static void scaleTo(Node node, Duration d, double value) {
		ScaleTransition transition = new ScaleTransition(d, node);
		transition.setInterpolator(DecelerateInterpolator.DEFAULT);
		transition.setToX(value);
		transition.setToY(value);
		transition.play();
	}

	public static void fadeTo(Node node, Duration d, double value) {
		FadeTransition transition = new FadeTransition(d, node);
		transition.setInterpolator(DecelerateInterpolator.DEFAULT);
		transition.setToValue(value);
		transition.play();
	}

	public static Point2D translate(Node node) {
		return new Point2D(node.getTranslateX(), node.getTranslateY());
	}

	public static ResourceManager setupResources() {
		Controller.setGlobalCSS("style/global.css");
		ResourceManager manager = new ResourceManager();
		try {
			manager.loadAllResources();
		} catch (IOException e) {
			handleFatalException(e);
		}
		return manager;
	}

	public static void handleFatalException(Throwable throwable) {
		throwable.printStackTrace();
		Platform.runLater(() -> {
			try {
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Fatal error");
				alert.setHeaderText("Scotland Yard has crashed");
				alert.setContentText("Scotland Yard was unable to continue due to a fatal error," +
						" " +
						"the program will now exit");
				alert.getDialogPane().setExpandableContent(createExceptionView(throwable));
				alert.getDialogPane().setExpanded(true);
				alert.getDialogPane().getScene().getWindow().sizeToScene();
				alert.showAndWait();
			} finally {
				Platform.exit();
			}
		});
	}

	public static void handleNonFatalException(Throwable exception, String message) {
		exception.printStackTrace();
		Platform.runLater(() -> {
			Alert alert = new Alert(AlertType.WARNING);
			alert.setTitle("Exception");
			alert.setHeaderText(message);
			alert.getDialogPane().setExpandableContent(createExceptionView(exception));
			alert.getDialogPane().getScene().getWindow().sizeToScene();
			alert.showAndWait();
		});
	}

	private static Parent createExceptionView(Throwable throwable) {
		Label label = new Label("The exception stacktrace was (also dumped to stderr):");
		TextArea textArea = new TextArea(Throwables.getStackTraceAsString(throwable));
		textArea.setEditable(false);
		textArea.setWrapText(true);
		textArea.setMaxWidth(Double.MAX_VALUE);
		textArea.setMaxHeight(Double.MAX_VALUE);
		textArea.setPrefColumnCount(55);
		GridPane.setVgrow(textArea, Priority.ALWAYS);
		GridPane.setHgrow(textArea, Priority.ALWAYS);
		GridPane expContent = new GridPane();
		expContent.setMaxWidth(Double.MAX_VALUE);
		expContent.add(label, 0, 0);
		expContent.add(textArea, 0, 1);
		return expContent;
	}

}
