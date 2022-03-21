package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.List;

import javax.annotation.Nonnull;

import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Piece.Detective;
import uk.ac.bris.cs.scotlandyard.model.Piece.MrX;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Transport;

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
		private ImmutableSet<Piece> remaining;
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
			for (Player detective : detectives){
				if(locations.get(detective.location()) != null) throw new IllegalArgumentException();
				locations.put(detective.location(), detective.piece().webColour());

			Set<Move> movesToBeAdded = new HashSet<>();
			//Find all possible moves
			
			// mrX can make both double and single moves
//			Set<Move> mrXSingleMoves = getSingleMoves(setup, detectives, mrX, mrX.location());
			movesToBeAdded.addAll(getSingleMoves(setup, detectives, mrX, mrX.location()));

//			Set<Integer> intList1 = new HashSet<>();
//			Set<Integer> intList2 = Set.of(1, 2);
//			intList1.addAll(intList2);
//			movesToBeAdded.addAll(Set.of(buildMove(mrX, Transport.TAXI, 5)));
			// calculate moves for mrX

			// mrX then calculate double moves

			// calculate moves for detectives
			// this.getPlayerTickets(piece)
			// for (Player det : detectives){
			// 	ImmutableList<Ticket> availableTickets = null; // = types of tickets that the player has at least 1 of
			// 	for (Ticket ticket : availableTickets) {
			// 		// calculate all possible moves
			// 		// create instances of moves (taxi, bus, underground)

					
			// 		// update and remove ??
			// 	}
			// }	
			 moves = ImmutableSet.copyOf(movesToBeAdded);
			//update winners based on possible moves list
			}
			
			//implement initial moves getter here
			
		}

		private Set<Move> getSingleMoves(
			GameSetup setup,
			ImmutableList<Player> detectives, 
			Player player, 
			int source){
				/*
				* for a given player, return an ImmutableSet of possible moves it can do
				*  - iterate through every edge, and filter each transport
				*/
				Set<Move> playerMoves = new HashSet<>();
				//ticketboard ~= ticket count for each ticket
				Optional<TicketBoard> tickets = this.getPlayerTickets(player.piece());

				if(tickets.isEmpty()) throw new IllegalArgumentException(); //Is this true?? if 1 detective has no more tickets but others have some left then the game should carry on
				
				//implemented ticket filter
				TicketBoard playerTickets = tickets.get();
				Set<Ticket> availableTickets = Stream.of(Ticket.values())
					.filter(ticketType -> playerTickets.getCount(ticketType) > 0)
					.collect(Collectors.toSet());
				
				//checking if source node exists
				if (!setup.graph.nodes().contains(source)) throw new IllegalArgumentException();

				 //iterating through all adjacent nodes
				for (int destination : setup.graph.adjacentNodes(source)) { 
					//TODO check if detective is not on node

					//gets stream of transport methods associated with current edge
					List<Transport> transportMethods = setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())
					.asList().stream() 
					//removes transport methods for which player doesn't have a ticket
					.filter(transportMethod -> availableTickets.contains(transportMethod.requiredTicket()))
					.toList();
					
					for (Transport transportMethod : transportMethods) {
						//add move to the list
						playerMoves.add(buildMove(player, transportMethod, source));
					}
				}

				return playerMoves;
		}

		Move buildMove(Player player, Transport transportMethod, int source) {
			return new Move() {

				public Piece commencedBy() {
					return player.piece();
				}

				public Iterable<Ticket> tickets() {

					List<Ticket> output = new LinkedList<Ticket>();
					output.add(transportMethod.requiredTicket());
					return output;
				}
				
				public int source() {return source;}
				
				public <T> T accept(Visitor<T> visitor) {return null;}
			};
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
			// TODO Auto-generated method stub
			return null;
		}
		
	};

		//throw new RuntimeException("Implement me!");
		
	
}
