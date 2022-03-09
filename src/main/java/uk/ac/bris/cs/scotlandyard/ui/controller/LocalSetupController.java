package uk.ac.bris.cs.scotlandyard.ui.controller;

import com.google.common.collect.ImmutableList;

import net.kurobako.gesturefx.GesturePane;
import net.kurobako.gesturefx.GesturePane.FitMode;
import net.kurobako.gesturefx.GesturePane.ScrollBarPolicy;

import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.atlassian.fugue.Option;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import uk.ac.bris.cs.fxkit.BindFXML;
import uk.ac.bris.cs.fxkit.Controller;
import uk.ac.bris.cs.fxkit.LambdaStringConverter;
import uk.ac.bris.cs.fxkit.SpinnerTableCell;
import uk.ac.bris.cs.fxkit.interpolator.DecelerateInterpolator;
import uk.ac.bris.cs.scotlandyard.ResourceManager;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.Piece.MrX;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;
import uk.ac.bris.cs.scotlandyard.ui.ColourTableCell;
import uk.ac.bris.cs.scotlandyard.ui.MapPreviewPane;
import uk.ac.bris.cs.scotlandyard.ui.model.BoardViewProperty;
import uk.ac.bris.cs.scotlandyard.ui.model.ModelProperty;
import uk.ac.bris.cs.scotlandyard.ui.model.PlayerProperty;
import uk.ac.bris.cs.scotlandyard.ui.model.TicketProperty;

import static javafx.util.Duration.millis;

/**
 * A controller that creates {@link ModelProperty} for the game.<br> Not required for the coursework.
 */
@BindFXML(value = "layout/LocalSetup.fxml", css = "style/localsetup.css")
final class LocalSetupController implements Controller {

	private static final int RANDOM = -1;
	private final ResourceManager manager;
	private final BoardViewProperty boardConfig;

	@FXML private VBox root;

	@FXML private ChoiceBox<Option<Ai>> mrXAi;
	@FXML private ChoiceBox<Option<Ai>> detectivesAi;

	// players config tab
	@FXML private GridPane playerEditor;
	@FXML private TableView<PlayerProperty<? super Piece>> playerTable;
	@FXML private TableColumn<PlayerProperty<Piece>, Boolean> enabled;
	@FXML private TableColumn<PlayerProperty<Piece>, Piece> colour;
	@FXML private Label playerColour;
	@FXML private TextField playerName;
	@FXML private ComboBox<Integer> playerLocation;
	@FXML private TableView<TicketProperty> playerTickets;
	@FXML private TableColumn<TicketProperty, Ticket> playerTicketType;
	@FXML private TableColumn<TicketProperty, Number> playerTicketCount;
	@FXML private StackPane playerLocationContainer;

	@FXML private Slider timeout;
	@FXML private Label timeoutHint;

	@FXML private Spinner<Integer> moveCount;
	@FXML private FlowPane moveConfig;

	@FXML private Button start;


	private ObservableList<PlayerProperty<? super Piece>> playerEntries = FXCollections
			.observableArrayList(v -> new Observable[]{v.enabledProperty(),});
	private final ImmutableList<Ai> availableAIs;
	private final EnumSet<Features> features;

	public enum Features {
		NAME, LOCATION, AI, TICKETS
	}

	LocalSetupController(ResourceManager manager,
	                     BoardViewProperty boardConfig,
	                     ModelProperty config,
	                     ImmutableList<Ai> availableAIs,
	                     EnumSet<Features> features,
	                     Consumer<ModelProperty> consumer) {
		Controller.bind(this);
		this.manager = Objects.requireNonNull(manager);
		this.boardConfig = Objects.requireNonNull(boardConfig);
		this.availableAIs = Objects.requireNonNull(availableAIs);
		this.features = Objects.requireNonNull(features);

		BooleanBinding blackSelected = Bindings.isNotEmpty(
				playerEntries.filtered(PlayerProperty::mrX).filtered(PlayerProperty::enabled));
		BooleanBinding atLeastTwoPlayer = Bindings
				.size(playerEntries.filtered(PlayerProperty::enabled)).greaterThan(1);

		bindRoundConfig(config);
		bindPlayersConfig(config);
		bindAiForSide(config.mrXAiProperty(), mrXAi);
		bindAiForSide(config.detectivesAiProperty(), detectivesAi);

		start.disableProperty().bind(blackSelected.and(atLeastTwoPlayer).not());
		start.setOnAction(e -> {
			ModelProperty property = createGameConfig();
			consumer.accept(property);
		});
	}

	private void bindAiForSide(ObjectProperty<Option<Ai>> source, ChoiceBox<Option<Ai>> aiOption) {
		aiOption.setItems(FXCollections.observableArrayList(ImmutableList.<Option<Ai>>builder()
				.add(Option.none())
				.addAll(availableAIs.stream().map(Option::some).collect(Collectors.toList()))
				.build()));
		aiOption.setConverter(LambdaStringConverter.forwardOnly("N/A(Human)",
				a -> a.fold(() -> "N/A(Human)", Ai::name)));
		aiOption.getSelectionModel().select(source.get());
		aiOption.setDisable(!features.contains(Features.AI));
	}

	private void bindPlayersConfig(ModelProperty model) {
		playerTable.setItems(playerEntries);
		playerEntries.addAll(model.everyone());
		enabled.setCellValueFactory(p -> p.getValue().enabledProperty());
		enabled.setCellFactory(tc -> new CheckBoxTableCell<>());
		colour.setCellValueFactory(p -> p.getValue().pieceProperty());
		colour.setCellFactory(tc -> new ColourTableCell<>());

		var selectionModel = playerTable.getSelectionModel();
		selectionModel.setSelectionMode(SelectionMode.SINGLE);
		playerEditor.visibleProperty().bind(selectionModel.selectedIndexProperty().isNotEqualTo(-1));
		playerTicketType.setCellValueFactory(p -> p.getValue().ticketProperty());
		playerTicketCount.setCellValueFactory(p -> p.getValue().countProperty());
		playerTicketCount.setCellFactory(cb -> new SpinnerTableCell<>(0, 100));
		selectionModel.selectedItemProperty().addListener((o, p, c) -> bindPlayerConfig(p, c));

		selectionModel.select(0);

	}

	private void bindPlayerConfig(PlayerProperty<? extends Piece> previous,
	                              PlayerProperty<? extends Piece> current) {
		if (current == null) return;
		if (previous != null) previous.observables().forEach(Property::unbind);
		selections.unsubscribe();

		FadeTransition transition = new FadeTransition(millis(250), playerEditor);
		transition.setInterpolator(new DecelerateInterpolator(2f));
		transition.setFromValue(0);
		transition.setToValue(1);
		transition.play();

		playerEditor.disableProperty().bind(current.enabledProperty().not());
		playerColour.setText(current.piece().toString() + " player");

		playerName.setText(current.name().orElse(""));
		playerName.setDisable(!features.contains(Features.NAME));
		current.nameProperty().bind(playerName.textProperty());


//		playerAI.getSelectionModel().select(current.ai());
//		current.aiProperty().bind(playerAI.getSelectionModel().selectedItemProperty());

		bindPlayerLocation(current);
		playerTickets.setItems(current.tickets());
		playerTickets.setDisable(!features.contains(Features.TICKETS));
	}

	private Subscription selections = Subscription.EMPTY;

	private void bindPlayerLocation(PlayerProperty<? extends Piece> current) {
		MapPreviewPane preview;
		Node previewNode = playerLocationContainer.lookup("#locationPreview");
		if (previewNode == null) {
			preview = new MapPreviewPane(manager);
			preview.setId("locationPreview");
			GesturePane pane = new GesturePane(preview);
			pane.setScrollBarPolicy(ScrollBarPolicy.AS_NEEDED);
			pane.scrollModeProperty().bind(boardConfig.scrollModeProperty());
			pane.setMinScale(Double.NEGATIVE_INFINITY);
			pane.setFitMode(FitMode.FIT);
			playerLocationContainer.getChildren().add(0, pane);
			Platform.runLater(() -> pane.zoomTo(0, Point2D.ZERO));
		} else {
			preview = (MapPreviewPane) previewNode;
		}

		boolean disabled = !features.contains(Features.LOCATION);
		playerLocation.setDisable(disabled);
		playerLocationContainer.setDisable(disabled);

		current.locationProperty().unbind();
		selections.unsubscribe();
		preview.reset();

		var otherPlayers = playerEntries.stream().filter(p -> p != current)
				.filter(p -> !p.randomLocation()).filter(PlayerProperty::enabled)
				.collect(Collectors.toList());

		Set<Integer> occupiedLocation = otherPlayers.stream().map(PlayerProperty::location)
				.collect(Collectors.toSet());
		otherPlayers.forEach(p -> preview.annotate(p.location(), p.piece()));

		List<Integer> locations = new ArrayList<>(current.piece() == MrX.MRX
				? ScotlandYard.MRX_LOCATIONS : ScotlandYard.DETECTIVE_LOCATIONS);

		List<Integer> availableLocations = new ArrayList<>(locations);
		availableLocations.removeAll(occupiedLocation);

		preview.highlight(availableLocations);

		ArrayList<Integer> selectableLocations = new ArrayList<>(availableLocations);
		selectableLocations.add(0, RANDOM);
		LambdaStringConverter<Integer> converter = LambdaStringConverter
				.forwardOnly(i -> {
					if (i == null) return "???";
					return i == RANDOM ? "Random" : i.toString();
				});
		playerLocation.setItems(FXCollections.observableList(selectableLocations));
		playerLocation.setConverter(converter);
		playerLocation.setCellFactory(cb -> {
			TextFieldListCell<Integer> cell = new TextFieldListCell<>(converter);
			EasyBind.subscribe(cell.hoverProperty(),
					hovered -> preview.annotate(cell.getItem(), current.piece()));
			return cell;
		});
		SingleSelectionModel<Integer> model = playerLocation.getSelectionModel();
		selections = EasyBind.subscribe(model.selectedItemProperty(), location -> {
			if (location != null && location != RANDOM)
				preview.annotate(location, current.piece());
		});

		model.select((Integer) current.location());
		current.locationProperty().bind(model.selectedItemProperty());
	}

	private void bindRoundConfig(ModelProperty initialValue) {
		// timeout
		timeoutHint.textProperty().bind(EasyBind.map(timeout.valueProperty(), Number::doubleValue)
				.map(Math::round).map(String::valueOf));
		timeout.valueProperty().setValue(initialValue.timeoutProperty().get().getSeconds());

		IntFunction<ToggleButton> mapper = i -> {
			ToggleButton button = new ToggleButton(String.valueOf(i + 1));
			button.setPrefWidth(45);
			boolean b = i >= initialValue.revealRounds().size() ? false
					: initialValue.revealRounds().get(i);
			button.setSelected(b);
			return button;
		};

		// rounds
		ObservableList<Node> roundToggles = moveConfig.getChildren();
		roundToggles.addAll(IntStream.range(0, initialValue.revealRounds().size())
				.mapToObj(mapper)
				.collect(Collectors.toList()));

		moveCount.setValueFactory(new IntegerSpinnerValueFactory(1, 99, roundToggles.size()));
		EasyBind.subscribe(moveCount.valueProperty(), count -> {
			int modelCount = roundToggles.size();
			if (count == 0) {
				roundToggles.clear();
			} else if (count < modelCount) {
				roundToggles.remove(count, modelCount);
			} else if (count > modelCount) {
				IntStream.range(modelCount, count).mapToObj(mapper)
						.collect(Collectors.toCollection(() -> roundToggles));
			}
		});

	}

	ModelProperty createGameConfig() {

		// fill in all the random locations
		var availableLocation = new ArrayList<>(ScotlandYard.DETECTIVE_LOCATIONS);
		availableLocation.removeAll(playerEntries.stream()
				.filter(PlayerProperty::enabled)
				.filter(PlayerProperty::detective)
				.filter(p -> !p.randomLocation())
				.map(PlayerProperty::location)
				.collect(Collectors.toSet()));
		Collections.shuffle(availableLocation);
		var deque = new ArrayDeque<>(availableLocation);
		playerEntries.forEach(p -> p.locationProperty().unbind());
		playerEntries.filtered(PlayerProperty::randomLocation).forEach(p -> {
			if (p.mrX()) {
				p.locationProperty().set(ScotlandYard.MRX_LOCATIONS.get(
						new Random().nextInt(ScotlandYard.MRX_LOCATIONS.size())));
			} else p.locationProperty().set(deque.pop());
		});

		return new ModelProperty(
				Duration.ofSeconds(Math.round(timeout.getValue())),
				moveConfig.getChildren().stream()
						.map(ToggleButton.class::cast)
						.map(ToggleButton::isSelected)
						.collect(ImmutableList.toImmutableList()),
				ImmutableList.copyOf(playerEntries.filtered(PlayerProperty::enabled)),
				manager.getGraph(),
				mrXAi.valueProperty().get(),
				detectivesAi.valueProperty().get());
	}


	@Override public Parent root() { return root; }

}
