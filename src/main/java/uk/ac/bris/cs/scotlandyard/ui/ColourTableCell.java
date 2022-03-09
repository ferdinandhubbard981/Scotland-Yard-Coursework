package uk.ac.bris.cs.scotlandyard.ui;

import javafx.scene.control.TableCell;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import uk.ac.bris.cs.scotlandyard.model.Piece;

public class ColourTableCell<S> extends TableCell<S, Piece> {

	@Override
	protected void updateItem(Piece item, boolean empty) {
		if (!empty) {
			Rectangle rectangle = new Rectangle(40, 20);
			rectangle.setFill(Color.web(item.webColour()));
			rectangle.setStroke(Color.LIGHTGRAY);
			rectangle.setStrokeWidth(1);
			setGraphic(rectangle);
		}
		super.updateItem(item, empty);
	}
}
