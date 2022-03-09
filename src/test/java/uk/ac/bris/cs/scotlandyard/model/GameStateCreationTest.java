package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ValueGraphBuilder;

import org.junit.Test;

import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Transport;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.BLUE;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.GREEN;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.RED;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.WHITE;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.YELLOW;
import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.STANDARD24MOVES;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.defaultDetectiveTickets;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.defaultMrXTickets;

/**
 * Tests game state initialisation
 * <p>
 * Note that exception message testing is intentionally left out as throwing the
 * correct exception type is enough to proof the given model is designed to spec
 */
public class GameStateCreationTest extends ParameterisedModelTestBase {

	@Test(expected = NullPointerException.class)
	public void testNullMrXShouldThrow() {
		gameStateFactory.build(standard24MoveSetup(),
				null,
				new Player(RED, defaultDetectiveTickets(), 42));
	}

	@Test(expected = NullPointerException.class)
	public void testNullDetectiveShouldThrow() {
		gameStateFactory.build(standard24MoveSetup(),
				new Player(MRX, defaultMrXTickets(), 42),
				null);
	}

	@Test(expected = NullPointerException.class)
	public void testAnyNullDetectiveShouldThrow() {
		gameStateFactory.build(standard24MoveSetup(),
				new Player(MRX, defaultMrXTickets(), 42),
				new Player(RED, defaultDetectiveTickets(), 43),
				new Player(GREEN, defaultDetectiveTickets(), 44),
				null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoMrXShouldThrow() {
		gameStateFactory.build(standard24MoveSetup(),
				new Player(BLUE, defaultMrXTickets(), 1),
				new Player(RED, defaultMrXTickets(), 2));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMoreThanOneMrXShouldThrow() {
		gameStateFactory.build(standard24MoveSetup(),
				new Player(MRX, defaultMrXTickets(), 1),
				new Player(MRX, defaultMrXTickets(), 2));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSwappedMrXShouldThrow() {
		gameStateFactory.build(standard24MoveSetup(),
				bluePlayer(),
				blackPlayer());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDuplicateDetectivesShouldThrow() {
		gameStateFactory.build(standard24MoveSetup(),
				new Player(MRX, defaultMrXTickets(), 41),
				new Player(BLUE, defaultDetectiveTickets(), 42),
				new Player(BLUE, defaultDetectiveTickets(), 42));

	}

	@Test(expected = IllegalArgumentException.class)
	public void testLocationOverlapBetweenDetectivesShouldThrow() {
		gameStateFactory.build(standard24MoveSetup(),
				new Player(MRX, defaultMrXTickets(), 41),
				new Player(BLUE, defaultDetectiveTickets(), 42),
				new Player(GREEN, defaultDetectiveTickets(), 42));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDetectiveHaveSecretTicketShouldThrow() {
		gameStateFactory.build(standard24MoveSetup(),
				blackPlayer(),
				new Player(BLUE, makeTickets(1, 1, 1, 0, 1), 2));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDetectiveHaveDoubleTicketShouldThrow() {
		gameStateFactory.build(standard24MoveSetup(),
				blackPlayer(),
				new Player(BLUE, makeTickets(1, 1, 1, 1, 0), 2));
	}


	@Test(expected = IllegalArgumentException.class)
	public void testEmptyMovesShouldThrow() {
		gameStateFactory.build(
				new GameSetup(standardGraph(), ImmutableList.of()),
				blackPlayer(),
				bluePlayer());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyGraphShouldThrow() {
		gameStateFactory.build(
				new GameSetup(ValueGraphBuilder.undirected()
						.<Integer, ImmutableSet<Transport>>immutable()
						.build(), STANDARD24MOVES),
				blackPlayer(),
				bluePlayer());
	}

	@Test public void testTwoPlayerWorks() {
		gameStateFactory.build(standard24MoveSetup(),
				blackPlayer(),
				bluePlayer());
	}

	@Test public void testSixPlayerWorks() {
		gameStateFactory.build(standard24MoveSetup(),
				blackPlayer(),
				redPlayer(),
				greenPlayer(),
				bluePlayer(),
				yellowPlayer(),
				whitePlayer());
	}

	@Test public void testGetMoveMatchesSupplied() {
		GameState state = gameStateFactory.build(
				new GameSetup(standardGraph(), ImmutableList.of(true, false, true, true)),
				blackPlayer(),
				bluePlayer());
		assertThat(state.getSetup().moves).containsExactly(true, false, true, true);
	}

	@Test public void testGetGraphMatchesSupplied() {
		GameState state = gameStateFactory.build(
				standard24MoveSetup(),
				blackPlayer(),
				bluePlayer());
		assertThat(state.getSetup().graph).isEqualTo(standardGraph());
	}

	@Test public void testGetPlayersMatchesSupplied() {
		GameState state = gameStateFactory.build(standard24MoveSetup(),
				blackPlayer(),
				redPlayer(),
				greenPlayer(),
				bluePlayer());
		assertThat(state.getPlayers()).containsExactlyInAnyOrder(MRX, RED, GREEN, BLUE);
	}

	@Test public void testWinningPlayerIsEmptyInitially() {
		GameState state = gameStateFactory.build(standard24MoveSetup(),
				blackPlayer(),
				redPlayer(),
				greenPlayer(),
				bluePlayer(),
				yellowPlayer(),
				whitePlayer());
		assertThat(state.getWinner()).isEmpty();
	}

	@Test public void testGetDetectiveLocationMatchesSupplied() {
		GameState state = gameStateFactory.build(standard24MoveSetup(),
				blackPlayer(),
				new Player(RED, defaultDetectiveTickets(), 26),
				new Player(BLUE, defaultDetectiveTickets(), 50),
				new Player(GREEN, defaultDetectiveTickets(), 94),
				new Player(WHITE, defaultDetectiveTickets(), 155),
				new Player(YELLOW, defaultDetectiveTickets(), 174));
		assertThat(state.getDetectiveLocation(RED)).hasValue(26);
		assertThat(state.getDetectiveLocation(BLUE)).hasValue(50);
		assertThat(state.getDetectiveLocation(GREEN)).hasValue(94);
		assertThat(state.getDetectiveLocation(WHITE)).hasValue(155);
		assertThat(state.getDetectiveLocation(YELLOW)).hasValue(174);
	}

	@Test public void testGetPlayerLocationForNonExistentPlayerIsEmpty() {
		GameState state = gameStateFactory.build(standard24MoveSetup(),
				blackPlayer(),
				redPlayer(),
				yellowPlayer());
		assertThat(state.getDetectiveLocation(RED)).isNotEmpty();
		assertThat(state.getDetectiveLocation(BLUE)).isEmpty();
		assertThat(state.getDetectiveLocation(GREEN)).isEmpty();
		assertThat(state.getDetectiveLocation(WHITE)).isEmpty();
		assertThat(state.getDetectiveLocation(YELLOW)).isNotEmpty();
	}

	@Test public void testGetPlayerTicketsMatchesSupplied() {
		var mrX = new Player(MRX, makeTickets(1, 2, 3, 4, 5), 1);
		var blue = new Player(BLUE, makeTickets(5, 4, 3, 0, 0), 2);
		GameState game = gameStateFactory.build(standard24MoveSetup(), mrX, blue);
		assertTicketCount(game, MRX, 1, 2, 3, 4, 5);
		assertTicketCount(game, BLUE, 5, 4, 3, 0, 0);
	}

	@Test public void testGetPlayerTicketsForNonExistentPlayerIsEmpty() {
		var mrX = new Player(MRX, makeTickets(1, 2, 3, 4, 5), 1);
		var blue = new Player(BLUE, makeTickets(5, 4, 3, 0, 0), 2);
		GameState state = gameStateFactory.build(standard24MoveSetup(), mrX, blue);

		// notice that when the player(colour) exists, getPlayerTickets() returns ticket count
		// even if it's zero

		assertThat(state.getPlayerTickets(MRX)).isNotEmpty();
		assertThat(state.getPlayerTickets(BLUE)).isNotEmpty();

		assertThat(state.getPlayerTickets(RED)).isEmpty();
		assertThat(state.getPlayerTickets(GREEN)).isEmpty();
		assertThat(state.getPlayerTickets(YELLOW)).isEmpty();
		assertThat(state.getPlayerTickets(WHITE)).isEmpty();
	}


}
