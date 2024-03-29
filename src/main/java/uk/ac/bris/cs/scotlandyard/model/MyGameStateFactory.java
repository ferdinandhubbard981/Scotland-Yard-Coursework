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

			//checking for null inputs
			if (mrX == null) throw new NullPointerException();
			if (detectives == null) throw new NullPointerException();
			if (detectives.contains(null)) throw new NullPointerException();

			//check detectives have 0 x2 & secret tickets 
			detectives.forEach((det) -> {
				if (det.has(Ticket.DOUBLE)) throw new IllegalArgumentException();
				if (det.has(Ticket.SECRET)) throw new IllegalArgumentException();
			});

			//check no duplicate detectives (colour) & no overlapping
			HashMap<String, Boolean> found = new HashMap<>();
			HashMap<Integer, String> locations = new HashMap<>();
			for (Player detective : detectives) {
				String colour = detective.piece().webColour();
				if (found.containsKey(colour) || locations.get(detective.location()) != null)
					throw new IllegalArgumentException();
				found.put(colour, true);
				locations.put(detective.location(), detective.piece().webColour());
			}

			//check empty graph
			if (setup.graph.nodes().size() == 0) throw new IllegalArgumentException();
			//check empty moves
			if (setup.moves.isEmpty()) throw new IllegalArgumentException();

			//get mrX & detective moves
			Set<Move> mrXMoves = getMoves(ImmutableList.of(mrX), detectives);
			Set<Move> detectiveMoves = getMoves(detectives, detectives);

			//if mrX is surrounded by detectives then detectives win aka mrX has no moves left
			if (mrXMoves.isEmpty() && remaining.contains(mrX.piece()))
				this.winner = getDetectivesAsImmutableSet();
			//if a detective is on the same square as mrX then the detectives win
			for (Player detective : detectives) {
				if (mrX.location() == detective.location()) {
					this.winner = getDetectivesAsImmutableSet();
					break;
				}
			}
			//if detectives have no moves left or logbook full then mrX wins
			if (detectiveMoves.isEmpty() || (log.size() == setup.moves.size() && remaining.isEmpty()))
				this.winner = ImmutableSet.of(mrX.piece());

			//if none of the conditions above are met then carry on
			if (this.winner.isEmpty()) {
				//updates the ACTUAL moves list with the moves of the remaining players
				Set<Move> allMoves = new HashSet<>(mrXMoves);
				allMoves.addAll(detectiveMoves);
				moves = ImmutableSet.copyOf(getRemainingPlayersMoves(allMoves));
			}
		}
		private ImmutableSet<Piece> getDetectivesAsImmutableSet(){
			return ImmutableSet.copyOf(this.detectives
					.stream()
					.map(detective -> detective.piece())
					.collect(Collectors.toUnmodifiableSet())
			);
		}

		/**
		 * @implNote iterates over transport methods of "source" and returns available moves given tickets.
		 * @apiNote used for both mrX and detectives
		 */
		private ImmutableSet<SingleMove> getSingleMoves(
				GameSetup setup,
				ImmutableList<Player> detectives,
				Player player,
				int source) {

			//checking if source node exists
			if (!setup.graph.nodes().contains(source)) throw new IllegalArgumentException();

			ImmutableSet.Builder<SingleMove> playerMoves = ImmutableSet.builder();

			//get available tickets from player
			Set<Ticket> availableTickets = Stream.of(Ticket.values())
					.filter(ticketType -> player.tickets().get(ticketType) > 0)
					.collect(Collectors.toSet());

			for (int destination : setup.graph.adjacentNodes(source)) {
				//check if detective is on destination node
				if (detectiveOnLocation(destination, detectives)) continue;
				//gets all transport methods a player can use given their tickets, and adds to playerMoves
				setup.graph
						.edgeValueOrDefault(source, destination, ImmutableSet.of())
						.stream()
						.filter(transportMethod -> availableTickets.contains(transportMethod.requiredTicket()))
						.forEach(transportMethod -> playerMoves.add(new SingleMove(
								player.piece(),
								source,
								transportMethod.requiredTicket(),
								destination
						)));
				//if player has a secret ticket then add move using secret ticket
				if (availableTickets.contains(Ticket.SECRET))
					playerMoves.add(new SingleMove(player.piece(), source, Ticket.SECRET, destination));
			}
			return playerMoves.build();
		}

		/**
		 * @implNote calls & iterates through getSingleMoves, then calls again to find double moves
		 * @apiNote used for mrX only
		 */
		private ImmutableSet<DoubleMove> getDoubleMoves(
				GameSetup setup,
				ImmutableList<Player> detectives,
				Player player,
				int source) {
			ImmutableSet.Builder<DoubleMove> doubleMoves = ImmutableSet.builder();
			// check if contains x2 ticket and has enough logbook space
			if (player.tickets().get(Ticket.DOUBLE) == 0 || log.size() + 2 > setup.moves.size())
				return doubleMoves.build();

			//get single moves
			ImmutableSet<SingleMove> firstMoveList = getSingleMoves(setup, detectives, player, source);

			//iterate through every possible first move
			for (SingleMove move1 : firstMoveList) {
				//making hypothetical player that has used a ticket in order to find second move
				Player hypotheticalPlayer = player.use(move1.ticket);
				//getting second move & building DoubleMove from 2 SingleMoves
				getSingleMoves(setup, detectives, hypotheticalPlayer, move1.destination)
						.forEach(move2 -> doubleMoves.add(buildDoubleMove(move1, move2)));
			}
			return doubleMoves.build();
		}


		@Override
		public GameSetup getSetup() {
			return this.setup;
		}

		@Override
		public ImmutableSet<Piece> getPlayers() {
			return ImmutableSet.<Piece>builder()
					.add(this.mrX.piece())
					.addAll(this.detectives.stream()
							.map(Player::piece)
							.collect(Collectors.toList()))
					.build();
		}

		@Override
		public Optional<Integer> getDetectiveLocation(Detective detective) {
			String colour = detective.webColour();
			return detectives.stream()
					.filter(x -> x.piece().webColour().equals(colour))
					.map(Player::location).findFirst();
		}

		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			ImmutableList<Player> allPlayers = ImmutableList.<Player>builder()
					.addAll(this.detectives)
					.add(this.mrX)
					.build();
			String colour = piece.webColour();
			Optional<Player> referencedPlayer = allPlayers.stream()
					.filter(player -> player.piece().webColour().equals(colour))
					.findFirst();

			if (referencedPlayer.isEmpty()) return Optional.empty();
			return Optional.of(ticket -> referencedPlayer.get().tickets().get(ticket));
		}

		@Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return this.log;
		}

		@Override
		public ImmutableSet<Piece> getWinner() {
			return this.winner;
		}

		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			return this.moves;
		}

		@Override
		public GameState advance(Move move) {
			if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);

			if (move.commencedBy() == this.mrX.piece()) {
				//add move to log (checking if setup.move is hidden or not)
				List<LogEntry> newLog = getNewLog(move, this.setup, this.log);
				//take used tickets away from mrX & move position to destination
				this.mrX = this.mrX.use(move.tickets());
				this.mrX = this.mrX.at(getMoveDestination(move));
				//swap to the detectives turn (update the remaining variable)
				//mrX plays first therefore all the detectives have yet to play their turn
				ImmutableSet<Piece> newRemainingPlayers = getDetectivesAsImmutableSet();
				return new MyGameState(this.setup,
						newRemainingPlayers,
						ImmutableList.copyOf(newLog),
						this.mrX,
						ImmutableList.copyOf(detectives)
				);
			}
			//finding detective who made the move
			Player detective = this.detectives.stream()
					.filter(det -> det.piece() == move.commencedBy())
					.findFirst().get();
			int index = this.detectives.indexOf(detective);
			//move detective to destination & give ticket to mrX
			detective = detective.at(getMoveDestination(move)).use(move.tickets());
			mrX = mrX.give(getSingleMoveTicket(move));

			//lambda expression needs value to be final
			final Player finalDetective = detective;
			//Ensure that particular detective won't move again this round (remove from remaining players)
			Set<Piece> newRemainingPlayers = this.remaining.stream()
					.filter(det -> det != finalDetective.piece())
					.collect(Collectors.toSet());

			//getting newRemainingPlayers in the form of ImmutableList<Player> for checking of moves afterwards
			Set<Piece> finalNewRemainingPlayers = newRemainingPlayers;
			ImmutableList<Player> remainingDetectives = ImmutableList.copyOf(detectives.stream()
					.filter(det -> finalNewRemainingPlayers.contains(det.piece()))
					.toList());
			//if remaining detectives have no more moves to play then swap to mrX turn (update remaining variable)
			if (getMoves(remainingDetectives, detectives).isEmpty())
				newRemainingPlayers = Set.of(this.mrX.piece());
			if (this.setup.moves.size() == this.log.size())
				newRemainingPlayers = Set.of();

			List<Player> mutableDetectives = new ArrayList<>(this.detectives);
			mutableDetectives.set(index, detective);
			return new MyGameState(this.setup,
					ImmutableSet.copyOf(newRemainingPlayers),
					this.log, this.mrX,
					ImmutableList.copyOf(mutableDetectives)
			);
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
			for (Move move : allMoves) {
				for (Piece piece : this.remaining) {
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

		//visitor pattern functions
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

		//add new entry in log depending on move type
		List<LogEntry> getNewLog(Move move, GameSetup setup, List<LogEntry> log) {
			return move.accept(new Move.Visitor<>() {
				public List<LogEntry> visit(SingleMove singleMove) {
					List<LogEntry> newLog = new ArrayList<>(log);
					if (!setup.moves.get(newLog.size())) newLog.add(LogEntry.hidden(singleMove.ticket));
					else newLog.add(LogEntry.reveal(singleMove.ticket, singleMove.destination));
					return newLog;
				}

				public List<LogEntry> visit(DoubleMove doubleMove) {
					List<LogEntry> newLog = new ArrayList<>(log);
					if (!setup.moves.get(newLog.size())) newLog.add(LogEntry.hidden(doubleMove.ticket1));
					else newLog.add(LogEntry.reveal(doubleMove.ticket1, doubleMove.destination1));
					if (!setup.moves.get(newLog.size())) newLog.add(LogEntry.hidden(doubleMove.ticket2));
					else newLog.add(LogEntry.reveal(doubleMove.ticket2, doubleMove.destination2));
					return newLog;
				}
			});
		}

		//returns the ticket type of given move
		Ticket getSingleMoveTicket(Move move) {
			return move.accept(new Visitor<>() {
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
