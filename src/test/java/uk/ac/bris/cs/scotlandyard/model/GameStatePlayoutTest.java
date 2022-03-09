package uk.ac.bris.cs.scotlandyard.model;

import org.junit.Test;

import uk.ac.bris.cs.scotlandyard.model.Board.GameState;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.BLUE;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.GREEN;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.RED;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.WHITE;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.YELLOW;
import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket.BUS;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket.TAXI;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket.UNDERGROUND;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.defaultDetectiveTickets;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.defaultMrXTickets;

/**
 * Tests complete game play-outs.
 * <br>
 * <b>IMPORTANT: {@link GameState#advance(Move)} must be properly implemented for any of the
 * tests here to work properly!</b>
 */
public class GameStatePlayoutTest extends ParameterisedModelTestBase {

	@Test public void testSimpleGame() {
		var mrX = new Player(MRX, defaultMrXTickets(), 106);
		var red = new Player(RED, defaultDetectiveTickets(), 91);
		var green = new Player(GREEN, defaultDetectiveTickets(), 29);
		var blue = new Player(BLUE, defaultDetectiveTickets(), 94);
		var white = new Player(WHITE, defaultDetectiveTickets(), 50);
		var yellow = new Player(YELLOW, defaultDetectiveTickets(), 138);
		GameState state = gameStateFactory.build(standard24MoveSetup(),
				mrX, red, green, blue, white, yellow);
		state = state
				.advance(x2(MRX, 106, TAXI, 105, BUS, 87))

				.advance(taxi(YELLOW, 138, 152))
				.advance(taxi(WHITE, 50, 49))
				.advance(bus(BLUE, 94, 77))
				.advance(taxi(GREEN, 29, 41))
				.advance(taxi(RED, 91, 105))

				.advance(taxi(MRX, 87, 88))

				.advance(bus(RED, 105, 87))
				.advance(taxi(WHITE, 49, 66))
				.advance(taxi(BLUE, 77, 96))
				.advance(taxi(YELLOW, 152, 138))
				.advance(taxi(GREEN, 41, 54))

				.advance(x2(MRX, 88, TAXI, 89, UNDERGROUND, 67))

				.advance(taxi(WHITE, 66, 67)); // MrX captured here
		assertGameIsOver(state);
		assertThat(state.getWinner()).containsExactlyInAnyOrder(RED, GREEN, BLUE, WHITE, YELLOW);
	}

}