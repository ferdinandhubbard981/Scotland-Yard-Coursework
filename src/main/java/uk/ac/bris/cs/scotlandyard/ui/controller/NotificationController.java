package uk.ac.bris.cs.scotlandyard.ui.controller;

import java.util.HashMap;
import java.util.Map;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import uk.ac.bris.cs.fxkit.BindFXML;
import uk.ac.bris.cs.fxkit.Controller;
import uk.ac.bris.cs.scotlandyard.ui.controller.NotificationController.NotificationBuilder.Notification;

/**
 * Controller for stackable notifications.<br> Not required for the coursework.
 */
@BindFXML("layout/Notification.fxml") final class NotificationController implements Controller {

	@FXML private VBox root;

	private final Map<String, Notification> notifications = new HashMap<>();

	NotificationController() { Controller.bind(this); }

	void show(String key, Notification notification) {
		Platform.runLater(() -> {
			notifications.compute(key, (k, last) -> {
				if (last != null) last.dismiss();
				return notification;
			});
			StackPane container = new StackPane(notification.root());
			container.setId(key);
			container.getStyleClass().add("notification");
			root.getChildren().add(container);
		});
	}

	void dismissAll() {
		Platform.runLater(() -> {
			root.getChildren().clear();
			notifications.values().forEach(Notification::dismiss);
			notifications.clear();
		});

	}

	public static class NotificationBuilder {

		private final VBox root = new VBox();
		private final HBox actions = new HBox();
		private final ProgressBar timer = new ProgressBar();
		private Timeline timeline;

		NotificationBuilder(String titleText) {
			root.setMinWidth(300);
			root.setSpacing(8);
			root.setAlignment(Pos.CENTER);
			Label title = new Label();
			title.setContentDisplay(ContentDisplay.RIGHT);
			title.setGraphicTextGap(12);
			title.setGraphic(actions);
			title.setText(titleText);
			timer.setManaged(false);
			timer.visibleProperty().bind(timer.managedProperty());
			timer.setMaxWidth(Double.MAX_VALUE);
			root.getChildren().addAll(title, timer);
		}

		NotificationBuilder addAction(String text, Runnable callback, boolean focus) {
			Button action = new Button(text);
			action.setOnAction(e -> callback.run());
			actions.getChildren().add(action);
			if (focus) Platform.runLater(action::requestFocus);
			return this;
		}

		Notification create() { return () -> root; }

		Notification create(Duration duration, Runnable callback) {
			timer.setManaged(true);
			timeline = new Timeline(15);
			timeline.getKeyFrames().addAll(
					new KeyFrame(Duration.ZERO, new KeyValue(timer.progressProperty(), 1)),
					new KeyFrame(duration, new KeyValue(timer.progressProperty(), 0)));
			timeline.setOnFinished(e -> callback.run());
			timeline.play();
			return new Notification() {

				@Override public void dismiss() { timeline.stop(); }
				@Override public Node root() { return root; }
			};
		}

		interface Notification {
			default void dismiss() {}
			Node root();
		}

	}

	@Override public Parent root() { return root; }
}
