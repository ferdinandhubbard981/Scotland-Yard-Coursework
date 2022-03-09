package uk.ac.bris.cs.scotlandyard.ui.controller;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;

import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import uk.ac.bris.cs.scotlandyard.ResourceManager;
import uk.ac.bris.cs.scotlandyard.ResourceManager.ImageResource;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.GameSetup;
import uk.ac.bris.cs.scotlandyard.model.Model;
import uk.ac.bris.cs.scotlandyard.model.Model.Observer;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Move.FunctionalVisitor;
import uk.ac.bris.cs.scotlandyard.model.MyModelFactory;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.Player;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;
import uk.ac.bris.cs.scotlandyard.ui.GameControl;
import uk.ac.bris.cs.scotlandyard.ui.controller.LocalSetupController.Features;
import uk.ac.bris.cs.scotlandyard.ui.controller.NotificationController.NotificationBuilder;
import uk.ac.bris.cs.scotlandyard.ui.model.BoardViewProperty;
import uk.ac.bris.cs.scotlandyard.ui.model.ModelProperty;
import uk.ac.bris.cs.scotlandyard.ui.model.PlayerProperty;

import static uk.ac.bris.cs.scotlandyard.ui.Utils.handleFatalException;

public final class LocalGameController extends BaseGameController<MapController> {

	public static LocalGameController newGame(ResourceManager manager, Stage stage) {
		var controller = new LocalGameController(manager, stage, new BoardViewProperty());
		stage.setTitle("ScotlandYardNG");
		Parent root = controller.root();
		Scene scene = new Scene(root, 1280, 800);
		scene.getStylesheets().add(Resources.getResource("style/global.css").toExternalForm());
		stage.setScene(scene);
		stage.getIcons().add(manager.getImage(ImageResource.ICON));
		stage.show();
		return controller;
	}

	private LocalGameController(ResourceManager manager, Stage stage, BoardViewProperty property) {
		super(stage, () -> new MapController(manager, new NotificationController(), property));
	}

	@Override public void initialize(URL location, ResourceBundle resources) {
		MenuItem newGame = new MenuItem("New game");
		MenuItem reset = new MenuItem("Reset (discards current game)");
		newGame.setOnAction(e -> LocalGameController.newGame(resourceManager, new Stage()));
		reset.setOnAction(e -> {
			getStage().close();
			LocalGameController.newGame(resourceManager, new Stage());
		});
		addMenuItem(newGame);
		addMenuItem(reset);
		setupGame();
	}

	private void setupGame() {
		var startScreen = new LocalSetupController(resourceManager, config,
				ModelProperty.createDefault(resourceManager),
				ResourceManager.scanAis(),
				EnumSet.allOf(Features.class),
				this::createGame);
		showOverlay(startScreen.root());
	}

	void notifyGameOver(Model model, ImmutableList<? extends GameControl> controls,
	                    ModelProperty setup, ImmutableSet<Piece> winners) {
		controls.forEach(GameControl::onGameDetached);
		controls.forEach(model::unregisterObserver);
		map.lock();
		notifications.dismissAll();
		notifications.show("notify_gameover",
				new NotificationBuilder(
						"Game over, winner is \n" + winners)
						.addAction("Start again(same location)", () -> {
							notifications.dismissAll();
							createGame(setup);
						}, true)
						.addAction("Main menu", () -> {
							notifications.dismissAll();
							setupGame();
						}, false).create());
	}

	interface RecordingModel extends Model {
		ImmutableList<String> recorded();
	}
	static final class TestRecordingModelFactory implements Factory<Model> {
		private final Factory<Model> original;
		TestRecordingModelFactory(Factory<Model> original) {this.original = original;}

		private static String mkPlayerName(Player p) {
			return p.isMrX() ? "mrX" : p.piece().toString().toLowerCase();
		}

		private static String mkPlayerLn(Player p) {
			return String.format("var %s = new Player(%s, makeTickets(%d,%d,%d,%d,%d), %d)",
					mkPlayerName(p), p.piece(),
					p.tickets().getOrDefault(Ticket.TAXI, 0),
					p.tickets().getOrDefault(Ticket.BUS, 0),
					p.tickets().getOrDefault(Ticket.UNDERGROUND, 0),
					p.tickets().getOrDefault(Ticket.DOUBLE, 0),
					p.tickets().getOrDefault(Ticket.SECRET, 0),
					p.location());
		}

		private static String mkTicketLn(Ticket ticket) {
			//@formatter:off
			switch (ticket) {
				case TAXI: return "taxi";
				case BUS: return "bus";
				case UNDERGROUND: return "underg";
				case DOUBLE: return "x2";
				case SECRET: return "secret";
				default:
					throw new AssertionError();
			}
			//@formatter:on
		}

		private static String mkMoveLn(Move move) {
			return move.accept(new FunctionalVisitor<>(m ->
					String.format("%s(%s, %d, %d)",
							mkTicketLn(m.ticket), m.commencedBy(), m.source(), m.destination),
					m -> String.format("%s(%s, %d,  %s, %d, %s, %d)",
							mkTicketLn(Ticket.DOUBLE), m.commencedBy(), m.source(),
							m.ticket1, m.destination1, m.ticket2, m.destination2)));
		}

		@Nonnull @Override public RecordingModel build(GameSetup setup, Player mrX,
		                                               ImmutableList<Player> detectives) {
			var model = original.build(setup, mrX, detectives);
			var lines = new ArrayList<String>();
			lines.add(mkPlayerLn(mrX));
			detectives.stream().map(TestRecordingModelFactory::mkPlayerLn).forEach(lines::add);
			var gameName = "game";
			var gameSetup = "standard24RoundSetup()";
			var xs = mkPlayerName(mrX);
			var ds = detectives.stream().map(TestRecordingModelFactory::mkPlayerName)
					.collect(Collectors.joining(", "));
			lines.add(String.format("GameState %s = gameStateFactory.build(%s, %s, %s);",
					gameName, gameSetup, xs, ds));

			return new RecordingModel() {
				@Override
				public ImmutableList<String> recorded() { return ImmutableList.copyOf(lines); }
				@Override @Nonnull public Board getCurrentBoard() {return model.getCurrentBoard();}
				@Override public void registerObserver(@Nonnull Observer observer) {
					model.registerObserver(observer);
				}
				@Override public void unregisterObserver(@Nonnull Observer observer) {
					model.unregisterObserver(observer);
				}
				@Override @Nonnull public ImmutableSet<Observer> getObservers() {
					return model.getObservers();
				}
				@Override public void chooseMove(@Nonnull Move move) {
					lines.add(String.format("%s = %s.advance(%s);",
							gameName, gameName, mkMoveLn(move)));
					model.chooseMove(move);
				}
			};
		}
	}


	private void createGame(ModelProperty setup) {
		hideOverlay();
		try {
			var modelFactory = (new MyModelFactory());
			var model = modelFactory.build(new GameSetup(
							setup.graphProperty().get(),
							ImmutableList.copyOf(setup.revealRounds())),
					setup.mrX().asPlayer(),
					setup.detectives().stream()
							.map(PlayerProperty::asPlayer)
							.collect(ImmutableList.toImmutableList()));

			// XXX var causes LambdaFactory related errors
			ImmutableList<GameControl> controls = ImmutableList.of(map, travelLog, ticketBoard,
					status);
			controls.forEach(model::registerObserver);
			controls.forEach(l -> l.onGameAttach(model, setup, timeoutWinner -> {
				notifyGameOver(model, controls, setup, timeoutWinner);
			}));
			model.registerObserver(new Observer() {
				@Override public void onModelChanged(@Nonnull Board board, @Nonnull Event event) {
					if (event == Event.GAME_OVER) {
//						model.recorded().forEach(a -> System.out.println(a));
						Platform.runLater(() -> notifyGameOver(model, controls, setup,
								board.getWinner()));
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			handleFatalException(e);
		}
	}


}
