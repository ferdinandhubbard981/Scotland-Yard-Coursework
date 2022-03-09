package uk.ac.bris.cs.scotlandyard.ui.controller;


import com.google.common.collect.ImmutableList;
import net.kurobako.gesturefx.GesturePane.ScrollMode;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.When;
import javafx.beans.property.Property;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import uk.ac.bris.cs.fxkit.BindFXML;
import uk.ac.bris.cs.fxkit.Controller;
import uk.ac.bris.cs.scotlandyard.ResourceManager;
import uk.ac.bris.cs.scotlandyard.ResourceManager.ImageResource;
import uk.ac.bris.cs.scotlandyard.ui.GameControl;
import uk.ac.bris.cs.scotlandyard.ui.model.BoardViewProperty;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Main UI for the game, implementations are free to use all provided controllers.<br> Not
 * required for the coursework.
 */
@BindFXML("layout/Game.fxml")
abstract class BaseGameController<T extends MapController> implements Controller {

	@FXML private VBox root;
	@FXML private MenuBar menu;

	@FXML private Menu gameMenu;
	@FXML private MenuItem close;

	@FXML private MenuItem findNode;
	@FXML private MenuItem manual;
	@FXML private MenuItem license;
	@FXML private MenuItem about;

	@FXML private MenuItem resetViewport;

	@FXML private CheckMenuItem focusToggle;
	@FXML private CheckMenuItem historyToggle;

	@FXML private CheckMenuItem travelLogToggle;
	@FXML private CheckMenuItem ticketToggle;
	@FXML private CheckMenuItem statusToggle;
	@FXML private CheckMenuItem scrollToggle;
	@FXML private CheckMenuItem animationToggle;

	@FXML private AnchorPane gamePane;
	@FXML private StackPane mapPane;
	@FXML private StackPane setupPane;
	@FXML private StackPane roundsPane;
	@FXML private StackPane ticketsPane;
	@FXML private StackPane playersPane;
	@FXML private StackPane notificationPane;

	@FXML private VBox statusPane;

	private final Stage stage;

	final ResourceManager resourceManager;
	final BoardViewProperty config;

	final T map;
	final TravelLogController travelLog;
	final TicketBoardController ticketBoard;
	final NotificationController notifications;
	final StatusController status;

	BaseGameController(Stage stage, Supplier<T> mapSupplier) {
		this.map = mapSupplier.get();
		this.resourceManager = map.manager;
		this.stage = stage;
		this.config = map.view;
		Controller.bind(this);
		bindLayout();

		// initialise all controllers
		travelLog = new TravelLogController(resourceManager);
		ticketBoard = new TicketBoardController(resourceManager);
		notifications = map.notifications;
		status = new StatusController();


		Rectangle clip = new Rectangle();
		clip.widthProperty().bind(gamePane.widthProperty());
		clip.heightProperty().bind(gamePane.heightProperty());
		gamePane.setClip(clip);

		// system menu
		menu.setUseSystemMenuBar(true);

		// add all views
		mapPane.getChildren().add(map.root());
		roundsPane.getChildren().add(travelLog.root());
		playersPane.getChildren().add(ticketBoard.root());
		notificationPane.getChildren().add(notifications.root());
		statusPane.getChildren().add(status.root());

		close.setOnAction(e -> stage.close());
		about.setOnAction(e -> {
			Alert alert = new Alert(AlertType.INFORMATION,
					"ScotlandYard is part of the CW-MODEL coursework prepared for University of " +
							"Bristol course COMS100001",
					ButtonType.OK);
			ImageView logo = new ImageView(resourceManager.getImage(ImageResource.UOB_LOGO));
			logo.setPreserveRatio(true);
			logo.setSmooth(true);
			logo.setFitHeight(100);
			alert.setGraphic(logo);
			alert.setTitle("About ScotlandYard");
			alert.setHeaderText("ScotlandYard v0.1");
			alert.show();
		});

		findNode.setOnAction(e -> {
			Stage s = new Stage();
			s.setTitle("Find node");
			s.setScene(new Scene(new FindNodeController(config, resourceManager).root()));
			s.show();
		});
		manual.setOnAction(e -> {
			Stage s = new Stage();
			s.setTitle("Manual");
			s.setScene(new Scene(new ManualController(s).root()));
			s.show();
		});

		license.setOnAction(e -> {
			Stage s = new Stage();
			s.setTitle("License");
			s.setScene(new Scene(new LicenseController(s).root()));
			s.show();
		});

		// bind all menu values
		resetViewport.setOnAction(e -> map.resetViewport());


		// FIXME unmanaged+!visible does not invalidate layout
		travelLogToggle.setDisable(true);
		ticketToggle.setDisable(true);

		setAndBind(travelLog.root().visibleProperty(), travelLogToggle.selectedProperty());
		setAndBind(ticketBoard.root().visibleProperty(), ticketToggle.selectedProperty());
		setAndBind(config.animationProperty(), animationToggle.selectedProperty());
		setAndBind(config.historyProperty(), historyToggle.selectedProperty());
		setAndBind(config.focusPlayerProperty(), focusToggle.selectedProperty());


		// what a pain
		scrollToggle.setSelected(config.getScrollMode() == ScrollMode.ZOOM);
		config.scrollModeProperty().bind(new When(scrollToggle.selectedProperty())
				.then(ScrollMode.ZOOM)
				.otherwise(ScrollMode.PAN));

//		if (Platform.getCurrent() == Platform.WINDOWS)
//			scrollToggle.selectedProperty().setValue(false);
//			config.scrollModeProperty().setValue(ScrollMode.ZOOM);

	}

	private <S> void setAndBind(Property<S> source, Property<S> target) {
		target.setValue(source.getValue());
		target.bindBidirectional(source);
	}

	void showOverlay(Node node) {
		setupPane.getChildren().setAll(node);
		showOverlay();
	}

	void showOverlay() {
		gamePane.setEffect(new BoxBlur(6, 6, 2));
		setupPane.setManaged(true);
		setupPane.setVisible(true);
	}

	void hideOverlay() {
		gamePane.setEffect(null);
		setupPane.setManaged(false);
		setupPane.setVisible(false);
	}

	void addStatusNode(Node node) { statusPane.getChildren().add(0, node); }

	void addMenuItem(MenuItem item) { gameMenu.getItems().add(0, item); }

	protected ResourceManager manager() { return resourceManager; }

	@Override public Parent root() { return root; }

	public Stage getStage() { return stage; }

	private static void bindLayout() {
		var t = new Timeline(1, new KeyFrame(Duration.seconds(1), e -> {
			String x = "\u0020\u002d\u0020\u0050\u0072\u006f\u0070\u0065\u0072\u0074\u0079" +
					"\u0020\u006f\u0066\u0020\u0055\u006e\u0069\u0076\u0065\u0072\u0073" +
					"\u0069\u0074\u0079\u0020\u006f\u0066\u0020\u0042\u0072\u0069\u0073" +
					"\u0074\u006f\u006c\u002c\u0020\u0064\u006f\u0020\u006e\u006f\u0074" +
					"\u0020\u0064\u0069\u0073\u0074\u0072\u0069\u0062\u0075\u0074\u0065";
			Window.getWindows().forEach(w -> {
				if (w instanceof Stage) {
					Stage s = (Stage) w;
					Optional.ofNullable(s.getTitle()).ifPresentOrElse(
							(title) -> { if (!title.contains(x)) s.setTitle(title + x); },
							() -> s.setTitle(x)
					);
				}
			});
		}));
		t.setCycleCount(Timeline.INDEFINITE);
		t.play();
	}

	public void onApplicationStop() {
		ImmutableList<GameControl> controls = ImmutableList.of(map, travelLog, ticketBoard, status);
		controls.forEach(GameControl::onGameDetached);
		map.lock();
		notifications.dismissAll();
	}
}
