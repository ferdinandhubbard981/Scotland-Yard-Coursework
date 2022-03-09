package uk.ac.bris.cs.scotlandyard.ui.controller;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import net.kurobako.gesturefx.GesturePane;
import net.kurobako.gesturefx.GesturePane.FitMode;
import net.kurobako.gesturefx.GesturePane.ScrollBarPolicy;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import io.atlassian.fugue.Option;
import io.atlassian.fugue.Pair;
import io.atlassian.fugue.Unit;
import javafx.animation.Interpolator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.util.Duration;
import uk.ac.bris.cs.fxkit.BindFXML;
import uk.ac.bris.cs.fxkit.Controller;
import uk.ac.bris.cs.fxkit.interpolator.DecelerateInterpolator;
import uk.ac.bris.cs.scotlandyard.ResourceManager;
import uk.ac.bris.cs.scotlandyard.ResourceManager.ImageResource;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Model;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Move.DoubleMove;
import uk.ac.bris.cs.scotlandyard.model.Move.FunctionalVisitor;
import uk.ac.bris.cs.scotlandyard.model.Move.SingleMove;
import uk.ac.bris.cs.scotlandyard.model.Move.Visitor;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;
import uk.ac.bris.cs.scotlandyard.ui.GameControl;
import uk.ac.bris.cs.scotlandyard.ui.Utils;
import uk.ac.bris.cs.scotlandyard.ui.controller.NotificationController.NotificationBuilder;
import uk.ac.bris.cs.scotlandyard.ui.model.BoardViewProperty;
import uk.ac.bris.cs.scotlandyard.ui.model.ModelProperty;

import static io.atlassian.fugue.Option.none;
import static io.atlassian.fugue.Option.some;
import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;

/**
 * Map that holds playing pieces and draws annotations.<br> Not required for the coursework.
 */
@BindFXML("layout/Map.fxml")
class MapController implements Controller, GameControl {

	private static final Duration DURATION = Duration.millis(400);

	@FXML private Pane root;
	@FXML private ImageView mapView;
	@FXML private Pane historyPane;
	@FXML private Pane shadow;
	@FXML private Pane counterPane;
	@FXML private Pane hintPane;

	private final Pane mask;

	final NotificationController notifications;
	final BoardViewProperty view;
	final GesturePane gesturePane;
	final ResourceManager manager;

	final Map<Piece, CounterController> counters = new HashMap<>();
	final Map<Integer, MoveHintController> hints = new HashMap<>();
	final Map<Piece, Path> historyPaths = new HashMap<>();


	MapController(ResourceManager manager,
	              NotificationController notifications,
	              BoardViewProperty view) {
		Controller.bind(this);
		this.manager = requireNonNull(manager);
		this.notifications = requireNonNull(notifications);
		this.view = requireNonNull(view);
		StackPane pane = new StackPane(root);
		shadow.setStyle("-fx-background-color: rgba(0,0, 0, 0.4)");

		mask = new Pane();
		mask.setBlendMode(BlendMode.OVERLAY);
		shadow.getChildren().add(mask);

		gesturePane = new GesturePane(pane);
		gesturePane.setScrollBarPolicy(ScrollBarPolicy.NEVER);
		gesturePane.setClipEnabled(false);
		gesturePane.setFitMode(FitMode.FIT);
		gesturePane.setMinScale(0.1f);
		gesturePane.scrollModeProperty().bind(view.scrollModeProperty());
		gesturePane.setOnMouseClicked(e -> {
			if (e.getButton() == MouseButton.SECONDARY) {
				gesturePane.cover();
			} else if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
				gesturePane.animate(Duration.millis(200))
						.interpolateWith(Interpolator.EASE_BOTH)
						.zoomBy(gesturePane.getCurrentScale(),
								gesturePane.targetPointAt(new Point2D(e.getX(), e.getY()))
										.orElse(gesturePane.targetPointAtViewportCentre()));
			}
		});
		historyPane.visibleProperty().bind(view.historyProperty());
		Image image = manager.getImage(ImageResource.MAP);
		mapView.setImage(image);
		lockSize(image.getWidth(), image.getHeight(), root, historyPane, mask);
		Platform.runLater(() -> gesturePane.zoomTo(0, Point2D.ZERO));
	}

	private static void lockSize(double width, double height, Region... regions) {
		for (Region region : regions) {
			region.setPrefSize(width, height);
			region.setMaxSize(width, height);
			region.setMinSize(width, height);
//			region.resize(width, height);
		}
	}

	Model model;
	ModelProperty config;
	Consumer<ImmutableSet<Piece>> timeout;

	Option<ExecutorService> aiExecutor = none();
	Option<Ai> mrXAi = none();
	Option<Ai> detectiveAi = none();

	@Override public void onGameAttach(
			Model model, ModelProperty config, Consumer<ImmutableSet<Piece>> timeout) {
		this.model = requireNonNull(model);
		this.config = requireNonNull(config);
		this.timeout = requireNonNull(timeout);
		unlock();
		counters.clear();
		counterPane.getChildren().clear();
		historyPaths.clear();
		historyPane.getChildren().clear();
		for (var player : config.everyone()) {
			CounterController counter = new CounterController(manager, view.animationProperty(),
					player.piece(), player.location());
			counters.put(player.piece(), counter);
			counterPane.getChildren().add(counter.root());

			// setup initial path history
			Path path = new Path();
			path.setFill(Color.TRANSPARENT);
			path.setStroke(Color.web(player.piece().webColour()));
			path.setStrokeWidth(30d);
			path.setOpacity(0.5);
			historyPane.getChildren().add(path);
			historyPaths.put(player.piece(), path);
		}

		if (config.getMrXAi().isDefined() || config.getDetectivesAi().isDefined()) {
			view.historyProperty().set(true);
		}

		aiExecutor = some(runInContainment(() -> {
			mrXAi = config.getMrXAi();
			detectiveAi = config.getDetectivesAi();
			mrXAi.forEach(Ai::onStart);
			detectiveAi.forEach(Ai::onStart);
			return Executors.newCachedThreadPool(new ThreadFactoryBuilder()
					.setNameFormat("ai-thread-%d")
					.setUncaughtExceptionHandler((t, e) -> Utils.handleFatalException(new RuntimeException("An ai instance crashed on thread " + t.getName(), e)))
					.build());
		}));
		advanceModel(model);
	}

	@Override public void onGameDetached() {
		clearMoveHints();
		lock();
		runInContainment(() -> {
			mrXAi.forEach(Ai::onTerminate);
			detectiveAi.forEach(Ai::onTerminate);
			aiExecutor.forEach(x -> runInContainment(x::shutdownNow));
			return Unit.VALUE;
		});
	}

	private static <T> T runInContainment(Callable<T> r) {
		try {
			return r.call();
		} catch (Throwable e) {
			Utils.handleFatalException(e);
			throw new AssertionError();
		}
	}

	double maxLength() { return Math.max(root.getWidth(), root.getHeight()); }

	Runnable requestAi(Model board, Ai ai) {
		//var terminate = new AtomicBoolean(false);
		var moves = board.getCurrentBoard().getAvailableMoves();
		drawMoveHighlights(moves);
		aiExecutor.forEach(x -> x.submit(() -> {
			try {
				final var move = ai.pickMove(board.getCurrentBoard(), new Pair<>(config.timeoutProperty().get().getSeconds(), TimeUnit.SECONDS));
				if (!moves.contains(move)) {
					Utils.handleFatalException(
							new Exception("Ai(" + ai.name() + ") selected an invalid move, got: " + move + ", was expecting one of " + moves));
				} else {
					Platform.runLater(() -> selectAndMove(board, move));
				}
			} catch (Exception e) {
				Utils.handleFatalException(new Exception("Ai(" + ai.name() + ") " +
						"threw an exception while picking a move", e));
			}
		}, aiExecutor));
		return () -> handleAITimeOut(ai);
	}

	private void handleAITimeOut(Ai ai) {
		aiExecutor.forEach(x -> {
			try {
				x.awaitTermination(1l, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Utils.handleFatalException(
						new Exception("Ai(" + ai.name() + ") was interrupted during the bail-out grace period.", e));
			}
		});
	}

	Runnable requestHuman(ImmutableSet<Move> moves, Consumer<Move> moveCallback) {
		clearMoveHints();
		BiFunction<Integer, Integer, MoveHintController> mapping = (source, location) ->
				new MoveHintController(manager, this,
						source, location, moveCallback);
		// attach tickets to hint
		for (Move move : moves) {
			move.accept(new Visitor<Unit>() {
				@Override public Unit visit(SingleMove move) {
					hints.computeIfAbsent(move.destination, t -> mapping.apply(move.source(), t)).addMove(move);
					return Unit.VALUE;
				}

				@Override public Unit visit(DoubleMove move) {
					hints.computeIfAbsent(move.destination1, t -> mapping.apply(move.source(), t));
					hints.computeIfAbsent(move.destination2, t -> mapping.apply(move.source(), t)).addMove(move);
					return Unit.VALUE;
				}
			});
		}
		hintPane.getChildren().setAll(hints.values().stream()
				.map(MoveHintController::root)
				.collect(Collectors.toList()));
		drawMoveHighlights(moves);
		return () -> {};
	}

	void advanceModel(Model board) {
		var moves = board.getCurrentBoard().getAvailableMoves();
		var pieces = moves.stream().map(Move::commencedBy).collect(ImmutableSet.toImmutableSet());
		if (moves.isEmpty())
			throw new AssertionError("Model returned empty moves, did it pass all tests?");


		var mrX = moves.stream().map(Move::commencedBy)
				.collect(ImmutableSet.toImmutableSet())
				.equals(ImmutableSet.of(MRX));

		if (mrX) counters.get(MRX).animateVisibility(true);

		final Runnable terminateAction;
		if (mrX && mrXAi.isDefined()) {
			terminateAction = requestAi(board, mrXAi.get());
		} else if (!mrX && detectiveAi.isDefined()) {
			terminateAction = requestAi(board, detectiveAi.get());
		} else {
			terminateAction = requestHuman(
					board.getCurrentBoard().getAvailableMoves(), m -> selectAndMove(model, m));
		}

		notifications.show("notify_timeout",
				new NotificationBuilder(
						"Waiting for " + pieces + " to make a move").create(
						Duration.millis(config.timeoutProperty().get().toMillis()),
						() -> {
							terminateAction.run();
							notifications.dismissAll();
							timeout.accept(pieces.stream().anyMatch(Piece::isMrX) ?
									board.getCurrentBoard().getPlayers().stream()
											.filter(Piece::isDetective)
											.collect(ImmutableSet.toImmutableSet()) :
									ImmutableSet.of(MRX));
						}));


	}

	void selectAndMove(Model model, Move m) {
		notifications.dismissAll();
		clearMoveHints();
		var counter = counters.get(m.commencedBy());
		m.accept(new Visitor<Unit>() {
			@Override public Unit visit(SingleMove move) {
				counter.animateTicketMove(move.destination, some(() -> {
					counter.location(move.destination);
					counter.updateLocation();
					model.chooseMove(m);
					drawHistory(move, move.commencedBy());
				}));

				return Unit.VALUE;
			}
			@Override public Unit visit(DoubleMove move) {
				counter.animateTicketMove(move.destination1,
						some(() -> {
							counter.location(move.destination1);
							counter.animateTicketMove(move.destination2,
									some(() -> {
										counter.location(move.destination2);
										counter.updateLocation();
										model.chooseMove(m);
										drawHistory(move, move.commencedBy());
									}));
						}));
				return Unit.VALUE;
			}
		});

	}

	@Override public void onModelChanged(@Nonnull Board board, @Nonnull Event event) {
		if (event != Event.MOVE_MADE) return;
		counters.get(MRX).animateVisibility(Iterables.getLast(board.getMrXTravelLog()).location().isPresent());
		advanceModel(model);
	}

	MoveHintController hintAt(int node) { return hints.get(node); }
	Collection<MoveHintController> allHints() { return hints.values(); }


	private void drawMoveHighlights(ImmutableSet<Move> moves) {
		var destinations = moves.stream().flatMap(a -> a.accept(new FunctionalVisitor<>(
				m -> ImmutableSet.of(m.destination),
				m -> ImmutableSet.of(m.destination1, m.destination2))).stream())
				.collect(ImmutableSet.toImmutableSet());

		for (Integer location : destinations) {
			Point2D point = manager.coordinateAtNode(location);
			Circle circle = new Circle(ScotlandYard.MAP_NODE_SIZE);
			circle.setFill(Color.WHITE);
			circle.setTranslateX(point.getX());
			circle.setTranslateY(point.getY());
			circle.setOpacity(1);
			circle.setStyle("-fx-effect: dropshadow(two-pass-box, white, " + ScotlandYard.MAP_NODE_SIZE * 10 + ", 0.6, 0, 0)");
			mask.getChildren().add(circle);
		}
	}

	private void clearMoveHints() {
		hints.values().forEach(MoveHintController::discard);
		hints.clear();
		hintPane.getChildren().clear();
		mask.getChildren().clear();
	}

	private void drawHistory(Move move, Piece piece) {
		var source = coordinateAtNode(move.source());
		historyPaths.get(piece).getElements().addAll(
				move.accept(new FunctionalVisitor<ImmutableList<PathElement>>(
						m -> {
							var target = coordinateAtNode(m.destination);
							return ImmutableList.of(
									new MoveTo(source.getX(), source.getY()),
									new LineTo(target.getX(), target.getY()));
						},
						m -> {
							var target1 = coordinateAtNode(m.destination1);
							var target2 = coordinateAtNode(m.destination2);
							return ImmutableList.of(
									new MoveTo(source.getX(), source.getY()),
									new LineTo(target1.getX(), target1.getY()),
									new LineTo(target2.getX(), target2.getY()));
						})));
	}

	Point2D coordinateAtNode(int node) { return manager.coordinateAtNode(node); }

	@Override public Parent root() { return gesturePane; }
	void resetViewport() {
		gesturePane.animate(DURATION)
				.interpolateWith(DecelerateInterpolator.DEFAULT)
				.zoomTo(0, gesturePane.targetPointAtViewportCentre());
	}

	void lock() { List.of(hintPane).forEach(p -> p.setVisible(false)); }

	void unlock() { List.of(hintPane).forEach(p -> p.setVisible(true)); }

}
