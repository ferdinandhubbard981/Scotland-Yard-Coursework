package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.commonmark.node.Visitor;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Piece.Detective;
import uk.ac.bris.cs.scotlandyard.model.Piece.MrX;
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
		private List<Player> detectives;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;
		
		//constructor that gets called by the build function in MyGameStateFactory
		private MyGameState(
			final GameSetup setup, 
			final ImmutableSet<Piece> remaining, 
			final ImmutableList<LogEntry> log, 
			final Player mrX, 
			final ImmutableList<Player> detectives){

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
			if(setup.graph.nodes().size() == 0) throw new IllegalArgumentException();
			//check empty moves
			if(setup.moves.isEmpty()) throw new IllegalArgumentException();

			//check detective location overlaps test: testLocationOverlapBetweenDetectivesShouldThrow
			HashMap<Integer, String> locations = new HashMap<>();
			for (Player detective : detectives) {
				if (locations.get(detective.location()) != null) throw new IllegalArgumentException();
				locations.put(detective.location(), detective.piece().webColour());
			}
			Set<Move> mrXMoves = new HashSet<>();
			Set<Move> detectiveMoves = new HashSet<>();
			//get mrx single and double moves
			mrXMoves.addAll(getSingleMoves(setup, detectives, mrX, mrX.tickets(), mrX.location()));
			mrXMoves.addAll(getDoubleMoves(setup, detectives, mrX, mrX.location()));
			//Find all detective moves
			for (Player det : detectives) {
				detectiveMoves.addAll(getSingleMoves(setup, detectives, det, det.tickets(), det.location()));
			}

			//TODO if mrX is surrounded by detectives then detectives win aka mrX has no moves left
			if (mrXMoves.isEmpty()) winner = ImmutableSet.copyOf(detectives.asList().stream().map(det -> det.piece()).collect(Collectors.toUnmodifiableSet()));
			//TODO if a detective is on the same square than mrX then the detectives win
			for (Player detective : detectives) {
				if (mrX.location() == detective.location()) winner = ImmutableSet.copyOf(detectives.asList().stream().map(det -> det.piece()).collect(Collectors.toUnmodifiableSet()));
			}

			//if detectives have no moves left then mrX wins
			if (detectiveMoves.isEmpty()) winner = ImmutableSet.of(mrX.piece());
			//TODO if mrX log book is full then mrx wins
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
			ImmutableMap<Ticket, Integer> playerTickets,
			int source){
				/*
				* for a given player, return a set of possible moves it can do
				*  - iterate through every edge, and filter each transport
				*/
				Set<SingleMove> playerMoves = new HashSet<>();

				//implemented ticket filter

				Set<Ticket> availableTickets = Stream.of(Ticket.values())
					.filter(ticketType -> playerTickets.get(ticketType) > 0)
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
			Set<DoubleMove> doubleMoves= new HashSet<>();
			//get single moves
			ImmutableSet<SingleMove> firstMoveList = ImmutableSet.copyOf(getSingleMoves(setup, detectives, player, player.tickets(), source));
			// check if contains x2 ticket
			if (player.tickets().get(Ticket.DOUBLE) == 0)  return doubleMoves;

			//check if mrX has enough space in his travel log for double move
			if (log.size() + 2 > setup.moves.size()) return doubleMoves;

			//iterate through every possible first move
			for (SingleMove move1 : firstMoveList){
				//getting player tickets after first move i.e. removing the ticket that he used on the first move
				Map<Ticket, Integer> newPlayerTickets = modifyPlayerTickets(player.tickets(), ImmutableMap.of(move1.ticket, -1));
				//generating set of second moves by using getSingleMoves with the updated ticket list
				Set<SingleMove> secondMoveList = getSingleMoves(setup, detectives, player, player.tickets(), move1.destination);
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

			if(referencedPlayer.isEmpty()) return Optional.empty();
			//END
			//for valid players 
			return Optional.of(new TicketBoard(){
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
			if (!moves.contains(move)) throw new IllegalArgumentException();
			move.accept(new Move.Visitor<Void>() {
				
				@Override public Void visit(SingleMove singleMove){
					//implement what happens when a single move is done
					//remove current move from list of all moves
						//and update to reflect current player position
					//change position of player
					//remove ticket -> give to mrX if player is not mrX else destroy ticket
					return null;
				}

				@Override public Void visit(DoubleMove doubleMove){
					//implement what happens when a double move is done	
					//remove move from list and update
					//change position of player (i.e. mrX)
					//remove the two tickets
					return null;
				}
			});
			return null;
		}


		//helper functions

		//check if detective is on location
		boolean detectiveOnLocation(int location, ImmutableList<Player> detectives) {
			for (Player detective : detectives) {
				if (detective.location() == location) return true;
			}
			return false;
		}

		//return what a players ticket map would be after a singleMove (for use in finding second move)
		Map<Ticket, Integer> modifyPlayerTickets(ImmutableMap<Ticket, Integer> playerTickets,
												 ImmutableMap<Ticket, Integer> ticketChange) {
			Map<Ticket, Integer> newTickets = new HashMap<>();
			for (Ticket ticket : Ticket.values()) {
				if (ticketChange.containsKey(ticket)) {
					newTickets.put(ticket, playerTickets.get(ticket) + ticketChange.get(ticket));
				}
			}
			return newTickets;
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
			for (Piece piece : remaining) {
				for (Move move : allMoves) {
					if (move.commencedBy() == piece) output.add(move);
				}
			}
			return output;
		}

	};

}
