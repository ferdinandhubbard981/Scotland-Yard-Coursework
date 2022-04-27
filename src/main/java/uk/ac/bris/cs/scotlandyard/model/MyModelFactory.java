package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.Board.*;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {
		return new MyModel(setup, mrX, detectives);
	}

	class MyModel implements Model {
		GameState state;
		ImmutableSet<Observer> observerSet;
		MyModel(GameSetup setup,
				Player mrX,
				ImmutableList<Player> detectives){
			this.state = new MyGameStateFactory().build(setup, mrX, detectives);
			this.observerSet = ImmutableSet.of();

		}

		public Board getCurrentBoard() {
			return state;
		}

		public void registerObserver(@Nonnull Observer observer) {
			if (observerSet.contains(observer)) throw new IllegalArgumentException();
			Set<Observer> newObserverSet = new HashSet<>(this.observerSet);
			newObserverSet.add(observer);
			this.observerSet = ImmutableSet.copyOf(newObserverSet);
		}

		public void unregisterObserver(@Nonnull Observer observer) {
			if (observer == null) throw new NullPointerException();
			if (!observerSet.contains(observer)) throw new IllegalArgumentException();
			Set<Observer> newObserverSet = this.observerSet.stream()
					.filter(obs -> obs != observer).collect(Collectors.toSet());
			this.observerSet = ImmutableSet.copyOf(newObserverSet);
		}

		public ImmutableSet<Observer> getObservers() {
			return this.observerSet;
		}

		public void chooseMove(@Nonnull Move move) {
			// TODO Advance the model with move, then notify all observers of what what just happened.
			//  you may want to use getWinner() to determine whether to send out Event.MOVE_MADE or Event.GAME_OVER
			this.state = state.advance(move);
			for (Observer observer : observerSet) {
				if (state.getWinner().isEmpty()) observer.onModelChanged(this.state, Observer.Event.MOVE_MADE);
				else observer.onModelChanged(this.state, Observer.Event.GAME_OVER);
			}

		}
	}
}
