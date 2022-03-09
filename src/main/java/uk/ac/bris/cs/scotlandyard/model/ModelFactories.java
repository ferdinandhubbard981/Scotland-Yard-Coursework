package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.function.Supplier;

import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

/**
 * Not part of the coursework, required for the tests to find your factory implementations.
 */
public class ModelFactories {

	/**
	 * @return factories that will be used throughout the parameterised tests.
	 */
	public static ImmutableList<
			Entry<
					Supplier<Factory<GameState>>,
					Supplier<Factory<Model>>
					>
			> factories() {
		return ImmutableList.of(
				new SimpleImmutableEntry<>(MyGameStateFactory::new, MyModelFactory::new));
	}


}
