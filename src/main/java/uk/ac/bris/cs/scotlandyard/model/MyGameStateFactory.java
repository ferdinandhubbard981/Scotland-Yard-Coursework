package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.List;

import javax.annotation.Nonnull;

import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Piece.Detective;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;

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
			final List<Player> detectives){	//need to change to immutable list
				this.setup = setup;
				this.remaining = remaining;
				this.log = log;
				this.mrX = mrX;
				this.detectives = detectives;
		
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
				//Don't know how to do this efficiently in streams
				for (int i = 0; i < detectives.size()-1; i++) {
					for (int j = i + 1; j < detectives.size(); j++) {
						if (detectives.get(i).location() == detectives.get(j).location()) throw new IllegalArgumentException();
					}
				}
			
		}



		@Override
		public GameSetup getSetup() {
			// TODO Auto-generated method stub
			return null;
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
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ImmutableSet<Piece> getWinner() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public GameState advance(Move move) {
			// TODO Auto-generated method stub
			return null;
		}
		
	};

		//throw new RuntimeException("Implement me!");
		
	
}
