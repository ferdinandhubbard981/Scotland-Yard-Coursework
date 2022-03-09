package uk.ac.bris.cs.scotlandyard.model;

import org.junit.Test;

import uk.ac.bris.cs.scotlandyard.model.Board.GameState;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.BLUE;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.RED;
import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket.BUS;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket.DOUBLE;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket.SECRET;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket.TAXI;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket.UNDERGROUND;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.defaultDetectiveTickets;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.defaultMrXTickets;

/**
 * Tests actual game logic between players for the game state
 * <br>
 * <b>IMPORTANT: {@link GameState#advance(Move)} must be properly implemented for any of the
 * tests here to work properly!</b>
 */
public class GameStatePlayerTest extends ParameterisedModelTestBase {

	private static void assertTickets(Board state, Piece colour,
	                                  int taxi, int bus, int underground, int x2, int secret) {
		assertThat(state.getPlayerTickets(colour)).get().satisfies(ticketBoard -> {
			assertThat(ticketBoard.getCount(TAXI)).isEqualTo(taxi);
			assertThat(ticketBoard.getCount(BUS)).isEqualTo(bus);
			assertThat(ticketBoard.getCount(UNDERGROUND)).isEqualTo(underground);
			assertThat(ticketBoard.getCount(DOUBLE)).isEqualTo(x2);
			assertThat(ticketBoard.getCount(SECRET)).isEqualTo(secret);
		});
	}

	@Test public void testDetectiveNotPickingAMoveDoesNotAffectMrX() {
		var black = new Player(MRX, makeTickets(2, 0, 0, 0, 0), 45);
		var red = new Player(RED, makeTickets(2, 0, 0, 0, 0), 111);
		var blue = new Player(BLUE, makeTickets(0, 0, 0, 0, 0), 94);
		GameState state = gameStateFactory.build(standard24MoveSetup(), black, red, blue);
		state = state.advance(taxi(MRX, 45, 46));
		state = state.advance(taxi(RED, 111, 112));
		assertTickets(state, MRX, 2, 0, 0, 0, 0);
		assertTickets(state, RED, 1, 0, 0, 0, 0);
		assertTickets(state, BLUE, 0, 0, 0, 0, 0);
	}

	@Test public void testDetectiveTicketsGivenToMrXOnlyAfterUse() {
		var mrX = new Player(MRX, makeTickets(1, 1, 1, 0, 0), 45);
		var blue = new Player(BLUE, makeTickets(2, 0, 0, 0, 0), 94);

		GameState state = gameStateFactory.build(standard24MoveSetup(), mrX, blue);

		// blue taxi ticket given to black
		// NOTE: black uses the last taxi ticket but was given another one from
		// blue so the total taxi ticket for MrX is one

		state = state.advance(taxi(MRX, 45, 46));
		state = state.advance(taxi(BLUE, 94, 95));
		assertTickets(state, MRX, 1, 1, 1, 0, 0);
		assertTickets(state, BLUE, 1, 0, 0, 0, 0);
	}

	@Test public void testMrXMovesToDestinationAfterDoubleMove() {
		var mrX = new Player(MRX, makeTickets(2, 1, 1, 1, 0), 45);
		var blue = new Player(BLUE, makeTickets(2, 0, 0, 0, 0), 94);

		GameState state = gameStateFactory.build(
				new GameSetup(standardGraph(), moves(true, true, true)), mrX, blue);

		state = state.advance(x2(MRX, 45, TAXI, 46, TAXI, 47));
		state = state.advance(taxi(BLUE, 94, 95));
		assertThat(state.getMrXTravelLog()).last().isEqualTo(LogEntry.reveal(TAXI, 47));
	}

	@Test public void testMrXCorrectTicketDecrementsAfterDoubleMove() {
		var mrX = new Player(MRX, makeTickets(1, 1, 0, 1, 0), 45);
		var blue = new Player(BLUE, defaultDetectiveTickets(), 94);

		GameState state = gameStateFactory.build(standard24MoveSetup(), mrX, blue);

		// taxi, bus, and double tickets should decrement by 1

		state = state.advance(x2(MRX, 45, TAXI, 46, BUS, 34));
		assertTickets(state, MRX, 0, 0, 0, 0, 0);
	}

	@Test public void testMrXMovesToDestinationAfterTicketMove() {
		var mrX = new Player(MRX, makeTickets(1, 0, 0, 0, 0), 45);
		var blue = new Player(BLUE, defaultDetectiveTickets(), 94);

		// MrX reveals himself for all moves
		GameState state = gameStateFactory.build(
				new GameSetup(standardGraph(), moves(true, true)), mrX, blue);

		// black should move from 45 to 46
		state = state.advance(taxi(MRX, 45, 46));
		state = state.advance(taxi(BLUE, 94, 95));
		assertThat(state.getMrXTravelLog()).containsExactly(LogEntry.reveal(TAXI, 46));
	}

	@Test public void testMrXCorrectTicketDecrementsByOneAfterTicketMove() {
		var mrX = new Player(MRX, makeTickets(1, 0, 0, 0, 0), 45);
		var blue = new Player(BLUE, defaultDetectiveTickets(), 94);

		GameState state = gameStateFactory.build(standard24MoveSetup(), mrX, blue);

		// taxi should decrement by 1;
		state = state.advance(taxi(MRX, 45, 46));
		assertTickets(state, MRX, 0, 0, 0, 0, 0);
	}

	@Test public void testDetectiveMovesToDestinationAfterTicketMove() {
		var mrX = new Player(MRX, defaultMrXTickets(), 45);
		var blue = new Player(BLUE, makeTickets(1, 2, 3, 0, 0), 94);

		GameState state = gameStateFactory.build(standard24MoveSetup(), mrX, blue);

		// blue should move from 94 to 95

		state = state.advance(taxi(MRX, 45, 46));
		state = state.advance(taxi(BLUE, 94, 95));
		assertThat(state.getDetectiveLocation(BLUE)).contains(95);
	}

	@Test public void testDetectiveCorrectTicketDecrementsByOneAfterTicketMove() {
		var mrX = new Player(MRX, defaultMrXTickets(), 45);
		var blue = new Player(BLUE, makeTickets(2, 2, 3, 0, 0), 94);

		GameState state = gameStateFactory.build(standard24MoveSetup(), mrX, blue);

		// taxi should decrement by 1

		state = state.advance(taxi(MRX, 45, 46));
		state = state.advance(taxi(BLUE, 94, 95));
		assertTickets(state, BLUE, 1, 2, 3, 0, 0);
	}

	@Test public void testDetectiveLocationHoldsAfterPassMove() {
		var mrX = new Player(MRX, defaultMrXTickets(), 45);
		var red = new Player(RED, defaultDetectiveTickets(), 111);
		var blue = new Player(BLUE, makeTickets(0, 0, 0, 0, 0), 94);

		GameState state = gameStateFactory.build(standard24MoveSetup(), mrX, red, blue);

		// blue doesn't move
		state = state.advance(taxi(MRX, 45, 46));
		state = state.advance(taxi(RED, 111, 112));
		assertThat(state.getDetectiveLocation(BLUE)).contains(94);
		assertTickets(state, BLUE, 0, 0, 0, 0, 0);

	}

	@Test public void testDetectiveLocationAlwaysCorrect() {
		var mrX = new Player(MRX, defaultMrXTickets(), 45);
		var blue = new Player(BLUE, defaultDetectiveTickets(), 94);

		GameState state = gameStateFactory.build(
				new GameSetup(standardGraph(), moves(true, false, true)), mrX, blue);


		assertThat(state.getDetectiveLocation(BLUE)).contains(94);
		state = state.advance(taxi(MRX, 45, 46));
		state = state.advance(taxi(BLUE, 94, 93));
		assertThat(state.getDetectiveLocation(BLUE)).contains(93);
		state = state.advance(taxi(MRX, 46, 47));
		state = state.advance(taxi(BLUE, 93, 92));
		assertThat(state.getDetectiveLocation(BLUE)).contains(92);
	}

	@Test public void testMrXTravelLogCorrectRevealMove() {
		var mrX = new Player(MRX, defaultMrXTickets(), 45);
		var blue = new Player(BLUE, defaultDetectiveTickets(), 94);

		GameState state = gameStateFactory.build(
				new GameSetup(standardGraph(), moves(true, true, true)), mrX, blue);

		// Mr X's location should be available after every reveal move, but not at the start of
		// the game

		assertThat(state.getMrXTravelLog()).isEmpty();
		state = state.advance(taxi(MRX, 45, 46));
		state = state.advance(taxi(BLUE, 94, 93));
		assertThat(state.getMrXTravelLog()).containsExactly(
				LogEntry.reveal(TAXI, 46));
		state = state.advance(taxi(MRX, 46, 47));
		state = state.advance(taxi(BLUE, 93, 92));
		assertThat(state.getMrXTravelLog()).containsExactly(
				LogEntry.reveal(TAXI, 46), LogEntry.reveal(TAXI, 47));
	}

	@Test public void testMrXTravelLogCorrectOnHiddenMove() {
		var mrX = new Player(MRX, defaultMrXTickets(), 45);
		var blue = new Player(BLUE, defaultDetectiveTickets(), 94);

		GameState state = gameStateFactory.build(
				new GameSetup(standardGraph(), moves(true, false, false, true)), mrX, blue);

		// Mr X's location should be available after every reveal move, but otherwise give his
		// previous location
		assertThat(state.getMrXTravelLog()).isEmpty();
		state = state.advance(taxi(MRX, 45, 46));
		state = state.advance(taxi(BLUE, 94, 93));
		assertThat(state.getMrXTravelLog()).containsExactly(
				LogEntry.reveal(TAXI, 46));
		state = state.advance(taxi(MRX, 46, 47));
		state = state.advance(taxi(BLUE, 93, 92));
		assertThat(state.getMrXTravelLog()).containsExactly(
				LogEntry.reveal(TAXI, 46), LogEntry.hidden(TAXI));
		state = state.advance(taxi(MRX, 47, 62));
		state = state.advance(taxi(BLUE, 92, 73));
		assertThat(state.getMrXTravelLog()).containsExactly(
				LogEntry.reveal(TAXI, 46), LogEntry.hidden(TAXI), LogEntry.hidden(TAXI));
		state = state.advance(taxi(MRX, 62, 79));
		state = state.advance(taxi(BLUE, 73, 57));
		assertThat(state.getMrXTravelLog()).containsExactly(
						LogEntry.reveal(TAXI, 46),
						LogEntry.hidden(TAXI),
						LogEntry.hidden(TAXI),
						LogEntry.reveal(TAXI, 79));
	}

	@Test public void testMrXTravelLogCorrectForDoubleMove() {
		// TODO x2 reveal here
		var mrX = new Player(MRX, defaultMrXTickets(), 104);
		var blue = new Player(BLUE, defaultDetectiveTickets(), 174);
		GameState state = gameStateFactory.build(standard24MoveSetup(), mrX, blue);
		state = state.advance(taxi(MRX, 104, 116));
		state = state.advance(taxi(BLUE, 174, 161));
		state = state.advance(x2(MRX, 116,  TAXI, 117, TAXI, 129));
		assertThat(state.getMrXTravelLog()).containsExactly(
				LogEntry.hidden(TAXI),
				LogEntry.hidden(TAXI),
				LogEntry.reveal(TAXI, 129));
	}

	@Test public void testMrXTravelLogCorrectWithOneRevealMove() {
		var mrX = new Player(MRX, defaultMrXTickets(), 45);
		var blue = new Player(BLUE, defaultDetectiveTickets(), 94);
		GameState state = gameStateFactory.build(
				new GameSetup(standardGraph(), moves(true)), mrX, blue);
		state = state.advance(taxi(MRX, 45, 46));
		state = state.advance(taxi(BLUE, 94, 93));
		assertThat(state.getMrXTravelLog()).containsExactly(LogEntry.reveal(TAXI, 46));
	}

	@Test public void testMrXTravelLogCorrectWithOneHiddenMove() {
		var mrX = new Player(MRX, defaultMrXTickets(), 45);
		var blue = new Player(BLUE, defaultDetectiveTickets(), 94);
		GameState state = gameStateFactory.build(
				new GameSetup(standardGraph(), moves(false)), mrX, blue);
		state = state.advance(taxi(MRX, 45, 46));
		state = state.advance(taxi(BLUE, 94, 93));
		assertThat(state.getMrXTravelLog()).containsExactly(LogEntry.hidden(TAXI));
	}

}
