package uk.ac.bris.cs.scotlandyard.model;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Includes all test for the actual game model
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
		GameStateCreationTest.class,
		GameStateGameOverTest.class,
		GameStateMoveTest.class,
		GameStatePlayerTest.class,
		GameStateDetectivesAvailableMovesTest.class,
		GameStateMrXAvailableMovesTest.class,
		GameStatePlayoutTest.class,
		ModelObserverTest.class
})
public class AllTest {}
