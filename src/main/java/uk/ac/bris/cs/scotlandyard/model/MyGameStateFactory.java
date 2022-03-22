package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Piece.Detective;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Transport;
import uk.ac.bris.cs.scotlandyard.model.Move.*;

//TODO encapsulate all large blocks of code into many small helper functions with meaningful names for clarity
/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

	@Nonnull @Override public GameState build(
		GameSetup setup,
		Player mrX,
		ImmutableList<Player> detectives){
			return new MyGameState(setup, ImmutableSet.of(mrX.piece()), ImmutableList.of(), mrX, detectives);
	}

		
	private final class MyGameState implements GameState {
		private GameSetup setup;
		private ImmutableSet<Piece> remaining; //the players who have yet to play in the round??
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private ImmutableList<Player> detectives;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;

		//constructor that gets called by the build function in MyGameStateFactory
		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final ImmutableList<Player> detectives) {

			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			this.winner = ImmutableSet.of();
			this.moves = ImmutableSet.of();
			//making list of all players for convenience
//			List<Player> allPlayers = new ArrayList<>();
//			allPlayers.addAll(detectives);
//			allPlayers.add(mrX);

			//checking for null inputs
			if (mrX == null) throw new NullPointerException();
			if (detectives == null) throw new NullPointerException();
			if (detectives.contains(null)) throw new NullPointerException();

			//check detectives have 0 x2 & secret tickets 
			detectives.forEach((det) -> {
				if (det.has(Ticket.DOUBLE)) throw new IllegalArgumentException();
				if (det.has(Ticket.SECRET)) throw new IllegalArgumentException();
			});

			//check no duplicate detectives (colour)
			HashMap<String, Boolean> found = new HashMap<>();
			for (int i = 0; i < detectives.size(); i++) {
				String colour = detectives.get(i).piece().webColour();
				if (found.containsKey(colour)) throw new IllegalArgumentException();
				found.put(colour, true);
			}

			//check empty graph
			if (setup.graph.nodes().size() == 0) throw new IllegalArgumentException();
			//check empty moves
			if (setup.moves.isEmpty()) throw new IllegalArgumentException();

			//check detective location overlaps test: testLocationOverlapBetweenDetectivesShouldThrow
			HashMap<Integer, String> locations = new HashMap<>();
			for (Player detective : detectives) {
				if (locations.get(detective.location()) != null) throw new IllegalArgumentException();
				locations.put(detective.location(), detective.piece().webColour());
			}
			//get mrX moves
			Set<Move> mrXMoves = getMoves(ImmutableList.of(mrX), detectives);
			//get detective moves
			Set<Move> detectiveMoves = getMoves(detectives, detectives);

			//if mrX is surrounded by detectives then detectives win aka mrX has no moves left
			if (mrXMoves.isEmpty())
				winner = ImmutableSet.copyOf(detectives.asList().stream().map(det -> det.piece()).collect(Collectors.toUnmodifiableSet()));
			//if a detective is on the same square than mrX then the detectives win
			for (Player detective : detectives) {
				if (mrX.location() == detective.location()) {
					winner = ImmutableSet.copyOf(detectives.asList().stream().map(det -> det.piece()).collect(Collectors.toUnmodifiableSet()));
					break;
				}
			}

			//if detectives have no moves left then mrX wins
			if (detectiveMoves.isEmpty()) winner = ImmutableSet.of(mrX.piece());
			//if mrX log book is full then mrx wins
			if (log.size() == setup.moves.size()) winner = ImmutableSet.of(mrX.piece());
			//if none of the conditions above are met then carry on
			if (winner.isEmpty()) {
				//updates the ACTUAL moves list with the moves of the remaining players
				Set<Move> allMoves = new HashSet<>(mrXMoves);
				allMoves.addAll(detectiveMoves);
				moves = ImmutableSet.copyOf(getRemainingPlayersMoves(allMoves));
			}


			//implement initial moves getter here???

		}

		private Set<SingleMove> getSingleMoves(
				GameSetup setup,
				ImmutableList<Player> detectives,
				Player player,
				int source) {
			/*
			 * for a given player, return a set of possible moves it can do
			 *  - iterate through every edge, and filter each transport
			 */
			Set<SingleMove> playerMoves = new HashSet<>();

			//implemented ticket filter

			Set<Ticket> availableTickets = Stream.of(Ticket.values())
					.filter(ticketType -> player.tickets().get(ticketType) > 0)
					.collect(Collectors.toSet());

			//checking if source node exists
			if (!setup.graph.nodes().contains(source)) throw new IllegalArgumentException();

			//iterating through all adjacent nodes
			for (int destination : setup.graph.adjacentNodes(source)) {
				//check if detective is on destination node
				if (detectiveOnLocation(destination, detectives)) continue;
				//gets stream of transport methods associated with current edge
				List<Transport> transportMethods = setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())
						.asList().stream()
						//removes transport methods for which player doesn't have a ticket
						.filter(transportMethod -> availableTickets.contains(transportMethod.requiredTicket()))
						.toList();

				for (Transport transportMethod : transportMethods) {
					//add move to the list
					playerMoves.add(new SingleMove(player.piece(), source, transportMethod.requiredTicket(), destination));
				}
				//if player has a secret ticket then add move using secret ticket
				if (availableTickets.contains(Ticket.SECRET))
					playerMoves.add(new SingleMove(player.piece(), source, Ticket.SECRET, destination));

			}

			return playerMoves;
		}

		Set<DoubleMove> getDoubleMoves(
				GameSetup setup,
				ImmutableList<Player> detectives,
				Player player,
				int source) {
			//declare variables
			Set<DoubleMove> doubleMoves = new HashSet<>();
			//get single moves
			ImmutableSet<SingleMove> firstMoveList = ImmutableSet.copyOf(getSingleMoves(setup, detectives, player, source));
			// check if contains x2 ticket
			if (player.tickets().get(Ticket.DOUBLE) == 0) return doubleMoves;

			//check if mrX has enough space in his travel log for double move
			if (log.size() + 2 > setup.moves.size()) return doubleMoves;

			//iterate through every possible first move
			for (SingleMove move1 : firstMoveList) {
				//making hypothetical player that has used a ticket in order to find second move
				Player hypotheticalPlayer = player.use(move1.ticket);
				//getting second move
				Set<SingleMove> secondMoveList = getSingleMoves(setup, detectives, hypotheticalPlayer, move1.destination);
				// iterate through every possible second move
				for (SingleMove move2 : secondMoveList) {
					//build DoubleMove from two SingleMove
					doubleMoves.add(buildDoubleMove(move1, move2));
				}
			}
			return doubleMoves;
		}


		@Override
		public GameSetup getSetup() {
			//implemented getSetup
			return this.setup;
		}

		@Override
		public ImmutableSet<Piece> getPlayers() {
			//check getPlayersMatchesSupplied
			return ImmutableSet.<Piece>builder()
					.add(this.mrX.piece())
					.addAll(this.detectives.stream()
							.map(detective -> detective.piece())
							.collect(Collectors.toList()))
					.build();
			//END
		}

		@Override
		public Optional<Integer> getDetectiveLocation(Detective detective) {
			String colour = detective.webColour();
			//find detective in list with matching colour
			return detectives.stream()
					.filter(x -> x.piece().webColour() == colour)
					.map(x -> x.location()).findFirst();
		}

		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			//check getPlayerTicketsForNonExistentPlayerIsEmpty
			ImmutableList<Player> allPlayers = ImmutableList.<Player>builder()
					.addAll(this.detectives)
					.add(this.mrX)
					.build();
			String colour = piece.webColour();
			Optional<Player> referencedPlayer = allPlayers.stream()
					.filter(player -> player.piece().webColour().equals(colour))
					.findFirst();

			if (referencedPlayer.isEmpty()) return Optional.empty();
			//END
			//for valid players 
			return Optional.of(new TicketBoard() {
				@Override
				public int getCount(Ticket ticket) {
					return referencedPlayer.get().tickets().get(ticket);
				}
			});
			//END
		}

		@Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			//implemented get log
			return this.log;
		}

		@Override
		public ImmutableSet<Piece> getWinner() {
			return this.winner;

			//Implement logic for checking if there is a winner in contructor or advance?

			//check if any detective location == mrx location

			//check if mrxs travel log count == max number of moves
			// if (log.size() == setup.moves.size()) return new ImmutableSet<
		}

		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			return this.moves;
		}

		@Override
		public GameState advance(Move move) {
			//added framework for advance??
			if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);

			if (move.commencedBy() == mrX.piece()) { //mrX played this move
				//add move to log (checking if setup.move is hidden or not)
				List<LogEntry> newLog = getNewLog(move, setup, log);
				//take used tickets away from mrX
				this.mrX = this.mrX.use(move.tickets());
				//move mrX position to destination
				this.mrX = this.mrX.at(getMoveDestination(move));
				//swap to the detectives turn (update the remaining variable)
				//mrX plays first therefore all the detectives have yet to play their turn
				//we don't need to check that at least 1 detective has at least 1 move here
				//because we check for that in win conditions
				ImmutableSet<Piece> newRemainingPlayers = ImmutableSet.copyOf(detectives.stream().map(det -> det.piece()).collect(Collectors.toSet()));
				//return gamesState
				return new MyGameState(setup, newRemainingPlayers, ImmutableList.copyOf(newLog), this.mrX, ImmutableList.copyOf(detectives));

			} else {
				//finding detective who made the move
				int index = detectives.indexOf(detectives.stream().filter(det -> det.piece() == move.commencedBy()).findFirst().get());
				Player detective = detectives.get(index);
//				Player detective = detectives.stream().filter(det -> det.piece() == move.commencedBy()).findFirst().get();
				//move detective to move.destination
				detective = detective.at(getMoveDestination(move));
				//take used ticket from detective and give to mrX
				detective = detective.use(move.tickets());
				mrX = mrX.give(getSingleMoveTicket(move));
				//lambda expression needs value to be final
				final Player finalDetective = detective;
				//Ensure that particular detective won't move again this round (remove from remaining players)
				Set<Piece> newRemainingPlayers = remaining.stream().filter(det -> det != finalDetective.piece())
						.collect(Collectors.toSet());
				//lambda expression needs value to be final
				ImmutableSet<Piece> immutableNewRemainingPlayers = ImmutableSet.copyOf(newRemainingPlayers);
				//getting newRemainingPlayers in the form of ImmutableList<Player> for checking of moves afterwards
				ImmutableList<Player> remainingDetectives = ImmutableList.copyOf(detectives.stream()
						.filter(det -> immutableNewRemainingPlayers.contains(det.piece()))
						.toList());
				//if remaining detectives have no more moves to play then swap to mrX turn (update remaining variable)
				if (getMoves(remainingDetectives, detectives).isEmpty()) newRemainingPlayers = Set.of(mrX.piece());
				//TODO error detective is not referencing the detective in the list detectives
				List<Player> mutableDetectives = new ArrayList<>(this.detectives);
				mutableDetectives.set(index, detective);
				return new MyGameState(setup, ImmutableSet.copyOf(newRemainingPlayers), log, mrX, ImmutableList.copyOf(mutableDetectives));

			}
		}


		//helper functions

		//check if detective is on location
		boolean detectiveOnLocation(int location, ImmutableList<Player> detectives) {
			for (Player detective : detectives) {
				if (detective.location() == location) return true;
			}
			return false;
		}

		//combine two singleMoves into a double move
		DoubleMove buildDoubleMove(SingleMove move1, SingleMove move2) {
			DoubleMove doubleMove = new DoubleMove(move1.commencedBy(), move1.source(), move1.ticket, move1.destination,
					move2.ticket, move2.destination);
			return doubleMove;
		}

		//gets the moves of the players who are in the remaining players set;
		Set<Move> getRemainingPlayersMoves(Set<Move> allMoves) {
			Set<Move> output = new HashSet<>();
			for (Piece piece : this.remaining) {
				for (Move move : allMoves) {
					if (move.commencedBy() == piece) output.add(move);
				}
			}
			return output;
		}

		//Find all moves for players list
		Set<Move> getMoves(List<Player> players, List<Player> detectives) {
			Set<Move> playerMoves = new HashSet<>();
			for (Player player : players) {
				playerMoves.addAll(getSingleMoves(this.setup, ImmutableList.copyOf(detectives), player, player.location()));
				playerMoves.addAll(getDoubleMoves(this.setup, ImmutableList.copyOf(detectives), player, player.location()));

			}
			return playerMoves;
		}

		//find all moves for player
		Set<Move> getMoves(Player player, List<Player> detectives) {
			Set<Move> playerMoves = new HashSet<>();
			playerMoves.addAll(getSingleMoves(setup, ImmutableList.copyOf(detectives), player, player.location()));
			playerMoves.addAll(getDoubleMoves(setup, ImmutableList.copyOf(detectives), player, player.location()));
			return playerMoves;
		}

		//visitor functions
		int getMoveDestination(Move move) {
			return move.accept(new Move.Visitor<Integer>() {

				public Integer visit(SingleMove singleMove) {
					return singleMove.destination;
				}

				public Integer visit(DoubleMove doubleMove) {
					return doubleMove.destination2;
				}
			});
		}

		List<LogEntry> getNewLog(Move move, GameSetup setup, List<LogEntry> log) {
			return move.accept(new Move.Visitor<List<LogEntry>>() {
				public List<LogEntry> visit(SingleMove singleMove) {
					List<LogEntry> newLog = new ArrayList<>(log);
					if (setup.moves.get(newLog.size()) == true) newLog.add(LogEntry.hidden(singleMove.ticket));
					else newLog.add(LogEntry.reveal(singleMove.ticket, singleMove.source()));
					return newLog;
				}

				public List<LogEntry> visit(DoubleMove doubleMove) {
					List<LogEntry> newLog = new ArrayList<>(log);
					if (setup.moves.get(newLog.size()) == true) newLog.add(LogEntry.hidden(doubleMove.ticket1));
					else newLog.add(LogEntry.reveal(doubleMove.ticket1, doubleMove.source()));
					if (setup.moves.get(newLog.size()) == true) newLog.add(LogEntry.hidden(doubleMove.ticket2));
					else newLog.add(LogEntry.reveal(doubleMove.ticket2, doubleMove.destination1));
					return newLog;
				}
			});
		}

		Ticket getSingleMoveTicket(Move move) {
			return move.accept(new Visitor<Ticket>() {

				public Ticket visit(SingleMove singleMove) {
					return singleMove.ticket;
				}

				public Ticket visit(DoubleMove doubleMove) {
					throw new IllegalArgumentException("getSingleMoveTicket was called with double move");
				}

			});
		}
	};

}
