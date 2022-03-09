package uk.ac.bris.cs.scotlandyard.ui.controller;

import com.google.common.collect.ImmutableSet;

import java.util.function.Consumer;

import javax.annotation.Nonnull;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToolBar;
import uk.ac.bris.cs.fxkit.BindFXML;
import uk.ac.bris.cs.fxkit.Controller;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Model;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.ui.GameControl;
import uk.ac.bris.cs.scotlandyard.ui.model.ModelProperty;

/**
 * Controller for status bar.<br> Not required for the coursework.
 */
@BindFXML("layout/Status.fxml") final class StatusController implements Controller, GameControl {

	@FXML private ToolBar root;
	@FXML private Label round;
	@FXML private Label player;
	@FXML private Label time;
	@FXML private Label status;
	@FXML private Slider volume;

	StatusController() { Controller.bind(this); }

	@Override
	public void onGameAttach(Model board, ModelProperty configuration,
	                         Consumer<ImmutableSet<Piece>> timeout) { bindView(board.getCurrentBoard()); }

	@Override
	public void onModelChanged(@Nonnull Board board, @Nonnull Event event) { bindView(board); }
	@Override public void onGameDetached() { status.setText("Game finished"); }
	private void bindView(Board board) {
		int round = board.getMrXTravelLog().size();
		this.round.setText((round == 0 ? 1 : round) + " of " + board.getSetup().moves.size());
		if (!board.getWinner().isEmpty()) {
			status.setText("Game completed, winning player:" + board.getWinner());
		} else {
			var pending = board.getAvailableMoves().stream()
					.map(Move::commencedBy)
					.collect(ImmutableSet.toImmutableSet());
			status.setText("Waiting for move: " + pending);
		}
	}

	@Override public Parent root() { return root; }

	public void setPlayer(String player) {
		this.player.setText(player);
	}

	public void setStatus(String status) {
		this.status.setText(status);
	}

}
