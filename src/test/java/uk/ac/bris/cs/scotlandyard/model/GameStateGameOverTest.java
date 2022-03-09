package uk.ac.bris.cs.scotlandyard.model;

import org.junit.Test;

import uk.ac.bris.cs.scotlandyard.model.Board.GameState;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.BLUE;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.GREEN;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.RED;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.WHITE;
import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.defaultDetectiveTickets;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.defaultMrXTickets;

/**
 * Tests related to whether the game state reports game over correctly
 * <br>
 * <b>IMPORTANT: {@link GameState#advance(Move)} must be properly implemented for any of the
 * tests here to work properly!</b>
 */
public class GameStateGameOverTest extends ParameterisedModelTestBase {

	@Test public void testStartMoveShouldThrowIfGameAlreadyOver() {
		var mrX = new Player(MRX, defaultMrXTickets(), 86);
		var blue = new Player(BLUE, makeTickets(0, 0, 0, 0, 0), 105);
		GameState game = gameStateFactory.build(standard24MoveSetup(), mrX, blue);
		assertThat(game.getWinner()).isNotEmpty();
		assertThat(game.getAvailableMoves()).isEmpty(); // should be empty once winner exists
	}

	@Test public void testGameNotOverWhenThereIsStillMovesLeft() {
		var mrX = new Player(MRX, defaultMrXTickets(), 86);
		var blue = new Player(BLUE, defaultDetectiveTickets(), 85);
		GameState state = gameStateFactory.build(
				new GameSetup(standardGraph(), moves(true, false, false)), mrX, blue);
		state = state.advance(taxi(MRX, 86, 103));
		state = state.advance(taxi(BLUE, 85, 68));
		assertGameIsNotOver(state);
		state = state.advance(taxi(MRX, 103, 102));
		state = state.advance(taxi(BLUE, 68, 51));
		assertGameIsNotOver(state);
	}

	@Test public void testGameOverAfterAllMovesUsed() {
		var mrX = new Player(MRX, defaultMrXTickets(), 86);
		var blue = new Player(BLUE, defaultDetectiveTickets(), 85);

		GameState state = gameStateFactory.build(
				new GameSetup(standardGraph(), moves(true)), mrX, blue);

		state = state.advance(taxi(MRX, 86, 103));
		state = state.advance(taxi(BLUE, 85, 68));
		assertGameIsOver(state);
	}

	@Test public void testGameOverIfAllDetectivesStuck() {
		var mrX = new Player(MRX, defaultMrXTickets(), 86);
		var blue = new Player(BLUE, makeTickets(1, 0, 0, 0, 0), 105);
		var red = new Player(RED, makeTickets(1, 0, 0, 0, 0), 70);

		GameState state = gameStateFactory.build(standard24MoveSetup(), mrX, blue, red);
		state = state.advance(taxi(MRX, 86, 104));
		state = state.advance(taxi(BLUE, 105, 106)); // Blue runs out of tickets after this
		state = state.advance(taxi(RED, 70, 71)); // Red runs out of tickets after this
		// All detectives ran out of tickets, they are stuck
		assertGameIsOver(state);
	}

	@Test public void testGameOverIfMrXStuck() {
		var mrX = new Player(MRX, makeTickets(1, 1, 1, 0, 0), 86);
		var blue = new Player(BLUE, defaultDetectiveTickets(), 108);
		GameState state = gameStateFactory.build(standard24MoveSetup(), mrX, blue);

		// MrX picks the last taxi ticket and lands on a spot where there
		// is no other method of transport, he can no longer move
		state = state.advance(taxi(MRX, 86, 104));
		// MrX will receive an extra bus ticket but he is still stuck
		state = state.advance(bus(BLUE, 108, 105));
		// game is over because MrX is stuck and cannot make a move (no other detectives can
		// move because they are waiting for MrX, thus game cannot proceed because no one
		// can move at this point; in this case, MrX loses the game by foolishly walking
		// into a bad spot)
		assertGameIsOver(state);
	}

	@Test public void testGameNotOverIfMrXWasFreedBeforeNextRotation() {
		var mrX = new Player(MRX, makeTickets(1, 1, 1, 0, 0), 86);
		var blue = new Player(BLUE, defaultDetectiveTickets(), 108);
		var red = new Player(RED, defaultDetectiveTickets(), 134);
		GameState state = gameStateFactory.build(standard24MoveSetup(), mrX, blue, red);

		// MrX uses the last taxi ticket and lands on a spot where there is no
		// other method of transport, he can no longer move
		state = state.advance(taxi(MRX, 86, 104));
		// MrX will receive an extra bus ticket but he is still stuck
		state = state.advance(bus(BLUE, 108, 105));
		// MrX will receive an extra TAXI ticket, he is now freed
		state = state.advance(taxi(RED, 134, 118));
		// game is not over because MrX still has a spare TAXI ticket that he can use
		assertGameIsNotOver(state);
	}

	@Test public void testGameOverIfMrXCaptured() {
		var mrX = new Player(MRX, defaultMrXTickets(), 86);
		var blue = new Player(BLUE, defaultDetectiveTickets(), 85);
		GameState state = gameStateFactory.build(standard24MoveSetup(), mrX, blue);
		state = state.advance(taxi(MRX, 86, 103));
		state = state.advance(taxi(BLUE, 85, 103)); // MrX captured at 103
		assertGameIsOver(state);
	}

	@Test public void testDetectiveWinsIfMrXCornered() {
		var mrX = new Player(MRX, defaultMrXTickets(), 103);
		var blue = new Player(BLUE, makeTickets(0, 0, 0, 0, 0), 68);
		var red = new Player(RED, makeTickets(0, 0, 0, 0, 0), 84);
		var green = new Player(GREEN, defaultDetectiveTickets(), 102);
		GameState state = gameStateFactory.build(standard24MoveSetup(), mrX, blue, red, green);
		// MrX moves to 85, of which 2/3 connecting nodes are occupied by blue(68) and red(84)
		state = state.advance(taxi(MRX, 103, 85));
		// green then cuts MrX off by moving to 103, MrX is now cornered and cannot move
		state = state.advance(taxi(GREEN, 102, 103));
		assertGameIsOver(state);
	}

	@Test public void testGameNotOverIfSomeDetectivesAreStuck() {
		var mrX = new Player(MRX, defaultMrXTickets(), 40);
		var blue = new Player(BLUE, makeTickets(2, 0, 0, 0, 0), 39); // note 2 taxi tickets
		var red = new Player(RED, makeTickets(2, 0, 0, 0, 0), 1); // note 2 taxi tickets
		var green = new Player(GREEN, makeTickets(0, 0, 0, 0, 0), 41); // green is stuck from the beginning

		GameState state = gameStateFactory.build(standard24MoveSetup(), mrX, blue, red, green);

		state = state.advance(taxi(MRX, 40, 52)); // down to 1 taxi after this
		state = state.advance(taxi(BLUE, 39, 51));
		state = state.advance(taxi(RED, 1, 9));
		// green can't move, should be MrX's turn and the game should not be over

		assertGameIsNotOver(state);
	}

	@Test public void testGameNotOverIfMrXCorneredButCanStillEscape() {
		var mrX = new Player(MRX, defaultMrXTickets(), 40);
		var blue = new Player(BLUE, makeTickets(0, 0, 0, 0, 0), 39);
		var red = new Player(RED, makeTickets(0, 0, 0, 0, 0), 51);
		var white = new Player(WHITE, makeTickets(0, 0, 0, 0, 0), 69);
		var green = new Player(GREEN, defaultDetectiveTickets(), 41);

		GameState state = gameStateFactory.build(standard24MoveSetup(),
				mrX, blue, red, white, green);

		state = state.advance(taxi(MRX, 40, 52));
		// green then cuts MrX off by moving to 40 but MrX can still escape by  taking a
		//bus/secret to 41/13/67/86 or even a double move

		state = state.advance(taxi(GREEN, 41, 40));
		// no detectives can move at this point, allowing MrX to escape
		assertGameIsNotOver(state);
	}

	@Test public void testGameNotOverBeforeAnyMoveWithNonTerminatingConfiguration() {
		var mrX = new Player(MRX, defaultMrXTickets(), 86);
		var firstDetective = new Player(BLUE, defaultDetectiveTickets(), 108);
		GameState state = gameStateFactory.build(standard24MoveSetup(), mrX, firstDetective);
		assertGameIsNotOver(state);// game is not over with initial non-terminating setup
	}

	@Test public void testGameOverBeforeAnyMoveWithTerminatingConfiguration() {
		// blue cannot move and is the only detective, the game is over by  default
		var blue = new Player(BLUE, makeTickets(0, 0, 0, 0, 0), 108);
		var mrX = new Player(MRX, defaultMrXTickets(), 86);
		GameState state = gameStateFactory.build(standard24MoveSetup(), mrX, blue);
		assertGameIsOver(state);// game is over with initial condition terminating setup
	}

	@Test public void testWinningPlayerIsEmptyBeforeGameOver() {
		var mrX = new Player(MRX, defaultMrXTickets(), 86);
		var blue = new Player(BLUE, defaultDetectiveTickets(), 108);
		GameState state = gameStateFactory.build(standard24MoveSetup(), mrX, blue);
		state = state.advance(taxi(MRX, 86, 104));
		state = state.advance(bus(BLUE, 108, 105));
		assertGameIsNotOver(state);
	}

	@Test public void testWinningPlayerOnlyContainsBlackIfMrXWins() {
		var mrX = new Player(MRX, defaultMrXTickets(), 86);
		var blue = new Player(BLUE, makeTickets(1, 0, 0, 0, 0), 108);
		GameState state = gameStateFactory.build(standard24MoveSetup(), mrX, blue);
		state = state.advance(taxi(MRX, 86, 104));
		state = state.advance(taxi(BLUE, 108, 105)); // blue uses his last move
		assertGameIsOver(state);
		assertThat(state.getWinner()).containsExactlyInAnyOrder(MRX);
	}

	@Test public void testWinningPlayerOnlyContainAllDetectivesIfDetectiveWins() {
		var mrX = new Player(MRX, defaultMrXTickets(), 86);
		var blue = new Player(BLUE, defaultDetectiveTickets(), 85);
		var red = new Player(RED, defaultDetectiveTickets(), 111);
		GameState state = gameStateFactory.build(standard24MoveSetup(), mrX, blue, red);
		state = state.advance(taxi(MRX, 86, 103));
		state = state.advance(taxi(BLUE, 85, 103)); // MrX captured at 103
		assertGameIsOver(state);
		assertThat(state.getWinner()).containsExactlyInAnyOrder(BLUE, RED);
	}
}
