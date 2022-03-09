package uk.ac.bris.cs.scotlandyard.auxiliary;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ValueGraphBuilder;

import org.junit.Test;

import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Transport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the graph reader. This should always pass but is not part of the assignment.
 */
public class ScotlandYardGraphReaderTest {

	@Test public void testValidFile() {
		assertThat(ScotlandYard.readGraph(String.join("\n",
				"3 3",
				"1", "2", "3",
				"1 2 Ferry",
				"1 2 Bus",
				"1 3 Bus"))).isEqualTo(
				ValueGraphBuilder.undirected()
						.immutable()
						.addNode(1)
						.addNode(2)
						.addNode(3)
						.putEdgeValue(1, 2, ImmutableSet.of(Transport.BUS, Transport.FERRY))
						.putEdgeValue(1, 3, ImmutableSet.of(Transport.BUS)).build());
	}

	@Test(expected = Exception.class) public void testEmptyInputShouldThrow() {
		ScotlandYard.readGraph("");
	}

	@Test(expected = Exception.class) public void testBadFirstLine() {
		ScotlandYard.readGraph("Foo Bar Baz");
	}

	@Test(expected = Exception.class) public void testBadNodeCount() {
		ScotlandYard.readGraph(String.join("\n", "4 1", "1", "2", "3", "1 2 Ferry"));
	}

	@Test(expected = Exception.class) public void testBadEdgeCount() {
		ScotlandYard.readGraph(String.join("\n", "3 5", "1", "2", "3", "1 2 Ferry"));
	}

	@Test(expected = Exception.class) public void testBadNode() {
		ScotlandYard.readGraph(String.join("\n", "1 0", "Foo"));
	}

	@Test(expected = Exception.class) public void testBadEdge() {
		ScotlandYard.readGraph(String.join("\n", "2 1", "1", "2", "Foo Bar Baz"));
	}

}