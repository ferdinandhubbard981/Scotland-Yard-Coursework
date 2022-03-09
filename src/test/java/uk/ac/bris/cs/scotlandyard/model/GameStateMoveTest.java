package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;

import uk.ac.bris.cs.scotlandyard.model.Board.GameState;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.BLUE;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.GREEN;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.RED;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.WHITE;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.YELLOW;
import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket.BUS;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket.TAXI;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.defaultDetectiveTickets;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.defaultMrXTickets;

/**
 * Tests round related logic of the game state
 * <br>
 * <b>IMPORTANT: {@link GameState#advance(Move)} must be properly implemented for any of the
 * tests here to work properly!</b>
 */
public class GameStateMoveTest extends ParameterisedModelTestBase {

	static void assertMovesOnlyCommencedBy(ImmutableSet<Move> moves, Piece... commencors) {
		var actual = moves.stream()
				.map(Move::commencedBy)
				.collect(ImmutableSet.toImmutableSet());
		assertThat(actual).containsExactlyInAnyOrder(commencors);
	}

	@Test public void testCorrectMovePlayersForAllMoves() {
		var mrX = new Player(MRX, defaultMrXTickets(), 106);
		var red = new Player(RED, defaultDetectiveTickets(), 91);
		var green = new Player(GREEN, defaultDetectiveTickets(), 29);
		var blue = new Player(BLUE, defaultDetectiveTickets(), 94);
		var white = new Player(WHITE, defaultDetectiveTickets(), 50);
		var yellow = new Player(YELLOW, defaultDetectiveTickets(), 138);
		GameState state = gameStateFactory.build(standard24MoveSetup(),
				mrX, red, green, blue, white, yellow);
		assertThat(state.getAvailableMoves()).allMatch(a -> a.commencedBy() == MRX);

		state = state.advance(x2(MRX, 106, TAXI, 105, BUS, 87));
		assertMovesOnlyCommencedBy(state.getAvailableMoves(), RED, GREEN, BLUE, WHITE, YELLOW);

		state = state.advance(taxi(YELLOW, 138, 152));
		assertMovesOnlyCommencedBy(state.getAvailableMoves(), RED, GREEN, BLUE, WHITE);

		state = state.advance(taxi(WHITE, 50, 49));
		assertMovesOnlyCommencedBy(state.getAvailableMoves(), RED, GREEN, BLUE);

		state = state.advance(bus(BLUE, 94, 77));
		assertMovesOnlyCommencedBy(state.getAvailableMoves(), RED, GREEN);

		state = state.advance(taxi(GREEN, 29, 41));
		assertMovesOnlyCommencedBy(state.getAvailableMoves(), RED);

		state = state.advance(taxi(RED, 91, 105));
		assertThat(state.getAvailableMoves()).allMatch(a -> a.commencedBy() == MRX);
	}

	@Test public void testMoveIncrementsCorrectlyForDoubleMove() {
		var mrX = new Player(MRX, defaultMrXTickets(), 45);
		var blue = new Player(BLUE, defaultDetectiveTickets(), 94);
		GameState state = gameStateFactory.build(standard24MoveSetup(), mrX, blue);
		assertThat(state.getMrXTravelLog()).hasSize(0);
		state = state.advance(x2(MRX, 45, TAXI, 32, TAXI, 19));
		assertThat(state.getMrXTravelLog()).hasSize(2);
	}

	@Test public void testMrXIsTheFirstToPlay() {
		var mrX = new Player(MRX, defaultMrXTickets(), 35);
		var blue = new Player(BLUE, defaultDetectiveTickets(), 26);
		GameState state = gameStateFactory.build(standard24MoveSetup(), mrX, blue);
		assertThat(state.getAvailableMoves()).allMatch(m -> m.commencedBy().isMrX());
	}

	@Test public void testMoveRotationFor2Moves() {
		var mrX = new Player(MRX, defaultMrXTickets(), 35);
		var blue = new Player(BLUE, defaultDetectiveTickets(), 50);
		var red = new Player(RED, defaultDetectiveTickets(), 26);

		gameStateFactory.build(standard24MoveSetup(), mrX, blue, red)
				.advance(taxi(MRX, 35, 22))
				.advance(taxi(BLUE, 50, 37))
				.advance(taxi(RED, 26, 15))
				.advance(x2(MRX, 22, BUS, 65, TAXI, 64))
				.advance(taxi(BLUE, 37, 24))
				.advance(bus(RED, 15, 29))
				.advance(taxi(MRX, 64, 65));
		// no exceptions, everything should work
	}

	@Test public void testIllegalMoveNotInGivenMovesWillThrow() {
		var mrX = new Player(MRX, defaultMrXTickets(), 35);
		var blue = new Player(BLUE, defaultDetectiveTickets(), 26);
		GameState state = gameStateFactory.build(standard24MoveSetup(), mrX, blue);
		// supplying a illegal tickets to the given consumer should not be
		// allowed in this case, BUS ticket with destination 20 is not included
		// in the given list
		assertThatThrownBy(() -> state.advance(bus(MRX, 35, 30)))
				.isInstanceOf(IllegalArgumentException.class);
	}

}
