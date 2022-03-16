package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Optional;

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
			ImmutableList<Player> detectives) {
		//checking for null inputs
		if (mrX == null) throw new NullPointerException();
		if (detectives == null) throw new NullPointerException();
		if (detectives.contains(null)) throw new NullPointerException();
		//end 

		//check detectives have 0 x2 & secret tickets 
		detectives.forEach((det) -> {
			if (det.has(Ticket.DOUBLE)) throw new IllegalArgumentException();
			if (det.has(Ticket.SECRET)) throw new IllegalArgumentException();
		});
		//end
		
		//check no duplicate detectives (colour)
		HashMap<String, Boolean> found = new HashMap<>();
		for (int i = 0; i < detectives.size(); i++) {
			String colour = detectives.get(i).piece().webColour();
			if (found.containsKey(colour)) throw new IllegalArgumentException();
			found.put(colour, true);
		}
		//end
		
		//check empty graph & empty moves
		if(setup.graph.nodes().size() == 0) throw new IllegalArgumentException();
		if(setup.moves.size() == 0) throw new IllegalArgumentException();
		//end

		//check detective locations match supplied
		for(Player detective : detectives){
			//if (detective.location() != setup.graph)
		}
		//end







		
		return new GameState() {

			@Override
			public GameSetup getSetup() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ImmutableSet<Piece> getPlayers() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Optional<Integer> getDetectiveLocation(Detective detective) {
				// TODO Auto-generated method stub
				
				return null;
			}

			@Override
			public Optional<TicketBoard> getPlayerTickets(Piece piece) {
				// TODO Auto-generated method stub
				return null;
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
}
