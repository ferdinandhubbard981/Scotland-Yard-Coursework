package uk.ac.bris.cs.scotlandyard.model;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import com.google.common.io.Resources;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import uk.ac.bris.cs.scotlandyard.model.Piece.Detective;
import uk.ac.bris.cs.scotlandyard.model.Piece.MrX;

import static java.util.Collections.shuffle;

/**
 * A utility class containing things needed for the ScotlandYard game
 */
public final class ScotlandYard {

	private ScotlandYard() {}

	/**
	 * All detective game pieces
	 */
	public static final ImmutableSet<Piece> DETECTIVES = ImmutableSet.of(
			Detective.RED,
			Detective.GREEN,
			Detective.BLUE,
			Detective.WHITE,
			Detective.YELLOW);
	/**
	 * All game pieces
	 */
	public static final ImmutableList<Piece> ALL_PIECES = ImmutableList.of(
			MrX.MRX,
			Detective.RED,
			Detective.GREEN,
			Detective.BLUE,
			Detective.WHITE,
			Detective.YELLOW);

	public static final Comparator<Piece> PIECE_VALUE_ORDER =
			Comparator.comparingInt(ALL_PIECES::indexOf);

	/**
	 * Reveal moves for a 24 move game
	 */
	public static final ImmutableSet<Integer> REVEAL_MOVES =
			ImmutableSet.of(3, 8, 13, 18, 24);

	/**
	 * All possible initial locations for detectives
	 */
	public static final ImmutableList<Integer> DETECTIVE_LOCATIONS =
			ImmutableList.of(26, 29, 50, 53, 91, 94, 103, 112, 117, 123, 138, 141, 155, 174);

	/**
	 * All possible initial locations for Mr.X
	 */
	public static final ImmutableList<Integer> MRX_LOCATIONS =
			ImmutableList.of(35, 45, 51, 71, 78, 104, 106, 127, 132, 166, 170, 172);


	/**
	 * Tickets a detective should have
	 */
	public static final ImmutableSet<Ticket> DETECTIVE_TICKETS = ImmutableSet.of(
			Ticket.TAXI,
			Ticket.BUS,
			Ticket.UNDERGROUND);
	/**
	 * Tickets Mr.X should have
	 */
	public static final ImmutableSet<Ticket> MRX_TICKETS =
			ImmutableSet.copyOf(EnumSet.allOf(Ticket.class));

	public static final ImmutableList<Boolean> STANDARD24MOVES = IntStream.rangeClosed(1, 24)
			.mapToObj(REVEAL_MOVES::contains)
			.collect(ImmutableList.toImmutableList());

	/**
	 * Generates a list of randomly selected, mutually exclusive detective
	 * starting locations(using {@link #DETECTIVE_LOCATIONS})
	 *
	 * @param seed the random seed
	 * @param n number of detectives
	 * @return a list of locations
	 */
	@Nonnull public static ImmutableList<Integer> generateDetectiveLocations(int seed, int n) {
		if (n > DETECTIVE_LOCATIONS.size())
			throw new IllegalArgumentException("n > max detective locations");
		var locations = IntStream.range(0, DETECTIVE_LOCATIONS.size()).boxed()
				.collect(Collectors.toList());
		shuffle(locations, new Random(seed));
		return locations.stream().limit(n)
				.map(DETECTIVE_LOCATIONS::get)
				.collect(ImmutableList.toImmutableList());
	}

	/**
	 * Randomly selects a location from {@link #MRX_LOCATIONS}
	 *
	 * @param seed the random seed
	 * @return the selected location
	 */
	public static int generateMrXLocation(int seed) {
		return MRX_LOCATIONS.get(new Random(seed).nextInt(MRX_LOCATIONS.size()));
	}

	/**
	 * @return mutable map of default tickets for mrX
	 */
	@Nonnull public static ImmutableMap<Ticket, Integer> defaultMrXTickets() {
		return ImmutableMap.of(
				Ticket.TAXI, 4,
				Ticket.BUS, 3,
				Ticket.UNDERGROUND, 3,
				Ticket.DOUBLE, 2,
				Ticket.SECRET, 5);
	}

	/**
	 * @return mutable map of default tickets for detectives
	 */
	@Nonnull public static ImmutableMap<Ticket, Integer> defaultDetectiveTickets() {
		return ImmutableMap.of(
				Ticket.TAXI, 11,
				Ticket.BUS, 8,
				Ticket.UNDERGROUND, 4,
				Ticket.SECRET, 0,
				Ticket.DOUBLE, 0);
	}

	@Nonnull public static InputStream pngMapAsStream() {
		return ScotlandYard.class.getResourceAsStream("/map_large_waifu2x_2x.jpg");
	}

	public static final float MAP_SCALE = 2f;
	public static final int MAP_OFFSET = 60;
	public static final float MAP_NODE_SIZE = 20f * MAP_SCALE;

	@Nonnull public static ImmutableMap<Integer, Entry<Integer, Integer>>
	pngMapPositionEntries() throws IOException {
		List<String> lines = Resources.readLines(Resources.getResource("pos.txt"),
				StandardCharsets.UTF_8);
		var builder = ImmutableMap.<Integer, Entry<Integer, Integer>>builder();
		for (String line : lines) {
			Integer[] values = Stream.of(line.split("\\s+")).map(Integer::parseInt)
					.toArray(Integer[]::new);
			if (values.length != 3) continue;
			builder.put(values[0],
					new SimpleImmutableEntry<>(
							Math.round((values[1] + MAP_OFFSET) * MAP_SCALE),
							Math.round((values[2] + MAP_OFFSET) * MAP_SCALE)));
		}
		return builder.build();
	}

	@Nonnull public static ImmutableValueGraph<Integer, ImmutableSet<Transport>>
	standardGraph() throws IOException {
		return readGraph(Resources.toString(
				Resources.getResource("graph.txt"), StandardCharsets.UTF_8));
	}

	@Nonnull public static ImmutableValueGraph<Integer, ImmutableSet<Transport>>
	readGraph(@Nonnull String content) {
		List<String> lines = content.lines().collect(Collectors.toList());
		if (lines.isEmpty()) throw new IllegalArgumentException("No lines");
		int currentLine = 0;

		String[] topLine = lines.get(currentLine++).split(" ");
		int numberOfNodes = Integer.parseInt(topLine[0]);
		int numberOfEdges = Integer.parseInt(topLine[1]);

		MutableValueGraph<Integer, ImmutableSet<Transport>> graph =
				ValueGraphBuilder.undirected()
						.expectedNodeCount(numberOfNodes)
						.build();

		for (int i = 0; i < numberOfNodes; i++) {
			String line = lines.get(currentLine++);
			if (line.isEmpty()) continue;
			graph.addNode(Integer.parseInt(line));
		}

		for (int i = 0; i < numberOfEdges; i++) {
			String line = lines.get(currentLine++);
			if (line.isEmpty()) continue;

			String[] s = line.split(" ");
			if (s.length != 3) throw new IllegalArgumentException("Bad edge line:" + line);

			var pair = EndpointPair.unordered(Integer.parseInt(s[0]), Integer.parseInt(s[1]));
			//noinspection ConstantConditions
			graph.putEdgeValue(pair, ImmutableSet.<Transport>builder()
					.addAll(graph.edgeValueOrDefault(pair, ImmutableSet.of()))
					.add(Transport.valueOf(s[2].toUpperCase(Locale.ENGLISH))).build());
		}
		return ImmutableValueGraph.copyOf(graph);
	}


	/**
	 * Represents tickets in the ScotlandYard game
	 */
	public enum Ticket {TAXI, BUS, UNDERGROUND, DOUBLE, SECRET}

	/**
	 * Represents transportation types of the map routes in the ScotlandYard game
	 */
	public enum Transport {
		TAXI, BUS, UNDERGROUND, FERRY;
		@Nonnull public Ticket requiredTicket() {
			switch (this) {
				case TAXI:
					return Ticket.TAXI;
				case BUS:
					return Ticket.BUS;
				case UNDERGROUND:
					return Ticket.UNDERGROUND;
				case FERRY:
					return Ticket.SECRET;
				default:
					throw new AssertionError();
			}
		}
	}


	/**
	 * A generic factory used to create a game state or model
	 */
	public interface Factory<T> {
		/**
		 * Create an instance of the parameterised type given the parameters required for
		 * ScotlandYard game
		 *
		 * @param setup the game setup
		 * @param mrX MrX player
		 * @param detectives detective players
		 * @return an instance of the parameterised type
		 */
		@Nonnull T build(GameSetup setup, Player mrX, ImmutableList<Player> detectives);
		/**
		 * Delegates to {@link #build(GameSetup, Player, ImmutableList)}, mainly used for tests
		 * where the detectives will be passed on in code.
		 *
		 * @param setup the game setup
		 * @param mrX MrX player
		 * @param first the first detective
		 * @param rest the rest of the detective
		 * @return an instance of the parameterised type
		 */
		@Nonnull default T build(GameSetup setup, Player mrX, Player first, Player... rest) {
			return build(setup, mrX, ImmutableList.copyOf(Lists.asList(first, rest)));
		}
	}

}
