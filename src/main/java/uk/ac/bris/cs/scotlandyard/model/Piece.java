package uk.ac.bris.cs.scotlandyard.model;

import java.io.Serializable;

import javax.annotation.Nonnull;


/**
 * Represent a colored counter in the ScotlandYard game
 */
public interface Piece extends Serializable {
	/**
	 * @return the actual HTML colour hex of this piece as defined by the ScotlandYard game
	 */
	@Nonnull String webColour();
	/**
	 * @return true if this is a detective piece, false otherwise
	 */
	boolean isDetective();
	/**
	 * @return inverted {@link #isDetective()}
	 */
	default boolean isMrX() { return !isDetective();}

	/**
	 * Game-defined detective colour pieces.
	 */
	enum Detective implements Piece {
		RED("#f00"),
		GREEN("#0f0"),
		BLUE("#00f"),
		WHITE("#fff"),
		YELLOW("#ff0");
		private final String colour;
		Detective(String colour) {this.colour = colour;}
		@Nonnull @Override public String webColour() { return colour; }
		@Override public boolean isDetective() { return true; }
	}

	/**
	 * Game-defined MrX colour pieces.
	 */
	enum MrX implements Piece {
		MRX;
		@Override public boolean isDetective() { return false; }
		@Nonnull @Override public String webColour() { return "#000"; }
	}

}
