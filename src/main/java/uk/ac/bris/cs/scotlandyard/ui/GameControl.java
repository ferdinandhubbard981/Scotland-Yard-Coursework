package uk.ac.bris.cs.scotlandyard.ui;

import com.google.common.collect.ImmutableSet;

import java.util.function.Consumer;

import uk.ac.bris.cs.scotlandyard.model.Model;
import uk.ac.bris.cs.scotlandyard.model.Model.Observer;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.ui.model.ModelProperty;

/**
 * Classes wishing to be notified of game changes should implement this
 * interface, all methods have a default implementation of no-op.
 * <br>
 * Not required for the coursework.
 */
public interface GameControl extends Observer {

	default void onGameAttach(Model model, ModelProperty configuration,
	                          Consumer<ImmutableSet<Piece>> timeout) {}

	default void onGameDetached() {}

}
