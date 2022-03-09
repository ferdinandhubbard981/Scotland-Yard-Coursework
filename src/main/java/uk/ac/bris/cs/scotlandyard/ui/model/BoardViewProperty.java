package uk.ac.bris.cs.scotlandyard.ui.model;

import net.kurobako.gesturefx.GesturePane.ScrollMode;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

public class BoardViewProperty {

	private final ObjectProperty<ScrollMode> scrollMode =
			new SimpleObjectProperty<>(ScrollMode.PAN);
	private final BooleanProperty animation = new SimpleBooleanProperty(true);
	private final BooleanProperty focusPlayer = new SimpleBooleanProperty(false);
	private final BooleanProperty history = new SimpleBooleanProperty(false);

	public ScrollMode getScrollMode() {
		return scrollMode.get();
	}

	public ObjectProperty<ScrollMode> scrollModeProperty() {
		return scrollMode;
	}

	public BooleanProperty animationProperty() {
		return animation;
	}

	public BooleanProperty focusPlayerProperty() {
		return focusPlayer;
	}

	public BooleanProperty historyProperty() {
		return history;
	}

}
