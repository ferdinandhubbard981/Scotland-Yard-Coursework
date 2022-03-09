package uk.ac.bris.cs.scotlandyard.model;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Model.Observer;
import uk.ac.bris.cs.scotlandyard.model.Model.Observer.Event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.BLUE;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.RED;
import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket.TAXI;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.defaultDetectiveTickets;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.defaultMrXTickets;

/**
 * Tests observer related features of the model
 * <br>
 * <b>IMPORTANT: {@link GameState} must be fully implemented for any of the
 * tests here to work properly!</b>
 */
public class ModelObserverTest extends ParameterisedModelTestBase {

	// convenience for creating 6 valid player at non-overlapping locations
	private Model createValidSixPlayerGame() {
		return modelFactory.build(standard24MoveSetup(),
				blackPlayer(),
				redPlayer(),
				greenPlayer(),
				bluePlayer(),
				whitePlayer(),
				yellowPlayer());
	}

	@SuppressWarnings("ConstantConditions")
	@Test(expected = NullPointerException.class)
	public void testRegisterNullObserverShouldThrow() {
		createValidSixPlayerGame().registerObserver(null);
	}

	@SuppressWarnings("ConstantConditions")
	@Test(expected = NullPointerException.class)
	public void testUnregisterNullObserverShouldThrow() {
		createValidSixPlayerGame().unregisterObserver(null);
	}

	@Test public void testRegisterAndUnregisterObserver() {
		Model model = createValidSixPlayerGame();
		Observer spectator = new Observer() {};
		model.registerObserver(spectator);
		assertThat(model.getObservers()).containsExactlyInAnyOrder(spectator);
		model.unregisterObserver(spectator);
		assertThat(model.getObservers()).isEmpty();
	}

	@Test public void testRegisterAndUnregisterMoreThanOneObserver() {
		Model model = createValidSixPlayerGame();
		Observer a = new Observer() {};
		Observer b = new Observer() {};
		model.registerObserver(a);
		model.registerObserver(b);
		assertThat(model.getObservers()).containsExactlyInAnyOrder(a, b);
		model.unregisterObserver(a);
		assertThat(model.getObservers()).containsExactlyInAnyOrder(b);
		model.unregisterObserver(b);
		assertThat(model.getObservers()).isEmpty();
	}

	@Test public void testRegisterSameObserverTwiceShouldThrow() {
		Model model = createValidSixPlayerGame();
		Observer spectator = new Observer() {};
		// can't register the same spectator
		model.registerObserver(spectator);
		assertThatThrownBy(() -> model.registerObserver(spectator))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test public void testUnregisterIllegalObserverShouldThrow() {
		Model model = createValidSixPlayerGame();
		// can't unregister a spectator that has never been registered before
		assertThatThrownBy(() -> model.unregisterObserver(new Observer() {}))
				.isInstanceOf(IllegalArgumentException.class);
	}

	// Mockito verifies whether methods on classes/interfaces are called with the correct parameter
	// for the tests below, you do not have to understand how Mockito works, just read the tests
	// like an English sentence.

	@Test public void testOnBoardChangedAfterMoveSelect() {
		var mrX = new Player(MRX, defaultMrXTickets(), 45);
		var red = new Player(RED, defaultDetectiveTickets(), 111);
		var blue = new Player(BLUE, defaultDetectiveTickets(), 94);
		Observer observer = Mockito.mock(Observer.class);
		InOrder ordered = Mockito.inOrder(observer);
		Model game = modelFactory.build(standard24MoveSetup(), mrX, red, blue);
		game.registerObserver(observer);
		game.chooseMove(x2(MRX, 45, TAXI, 46, TAXI, 47));
		ordered.verify(observer)
				.onModelChanged(boardEq(game.getCurrentBoard()), Mockito.eq(Event.MOVE_MADE));
		game.chooseMove(taxi(RED, 111, 112));
		ordered.verify(observer)
				.onModelChanged(boardEq(game.getCurrentBoard()), Mockito.eq(Event.MOVE_MADE));
		game.chooseMove(taxi(BLUE, 94, 95));
		ordered.verify(observer)
				.onModelChanged(boardEq(game.getCurrentBoard()), Mockito.eq(Event.MOVE_MADE));
		ordered.verifyNoMoreInteractions();
	}


	@Test public void testMrXCaptureShouldNotifyGameOverOnce() {
		var mrX = new Player(MRX, defaultMrXTickets(), 45);
		var red = new Player(RED, defaultDetectiveTickets(), 47);
		Observer observer = Mockito.mock(Observer.class);
		InOrder ordered = Mockito.inOrder(observer);
		Model game = modelFactory.build(standard24MoveSetup(), mrX, red);
		game.registerObserver(observer);
		game.chooseMove(taxi(MRX, 45, 46));
		ordered.verify(observer)
				.onModelChanged(Mockito.any(), Mockito.any());
		game.chooseMove(taxi(RED, 47, 46));
		ordered.verify(observer)
				.onModelChanged(boardEq(game.getCurrentBoard()), Mockito.eq(Event.GAME_OVER));
		ordered.verifyNoMoreInteractions();
		assertThat(game.getCurrentBoard().getWinner()).containsExactlyInAnyOrder(RED);
	}


	@Test public void testFinalMoveShouldNotifyGameOverOnce() {
		var mrX = new Player(MRX, defaultMrXTickets(), 45);
		var red = new Player(RED, defaultDetectiveTickets(), 111);
		Observer observer = Mockito.mock(Observer.class);
		InOrder ordered = Mockito.inOrder(observer);
		Model game = modelFactory.build(new GameSetup(standardGraph(), moves(true)), mrX, red);
		game.registerObserver(observer);
		game.chooseMove(taxi(MRX, 45, 46));
		ordered.verify(observer)
				.onModelChanged(Mockito.any(), Mockito.any());
		game.chooseMove(taxi(RED, 111, 112));
		ordered.verify(observer)
				.onModelChanged(boardEq(game.getCurrentBoard()), Mockito.eq(Event.GAME_OVER));
		ordered.verifyNoMoreInteractions();
		assertThat(game.getCurrentBoard().getWinner()).containsExactlyInAnyOrder(MRX);
	}

	// creates a argument matcher to check whether the board is *value* equal to the given board
	private static Board boardEq(Board that) {
		final var snapshot = new ImmutableBoard(that);
		return Mockito.argThat(b -> snapshot.equals(new ImmutableBoard(b)));
	}


}
