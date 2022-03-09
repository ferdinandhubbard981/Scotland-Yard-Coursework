package uk.ac.bris.cs.scotlandyard.ui.controller;

import com.google.common.io.Resources;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import uk.ac.bris.cs.fxkit.BindFXML;
import uk.ac.bris.cs.fxkit.Controller;

/**
 * Controller for the license dialog.<br> Not required for the coursework.
 */
@BindFXML("layout/License.fxml") final class LicenseController implements Controller {

	@FXML private VBox root;
	@FXML private TextArea content;
	@FXML private Button dismiss;

	LicenseController(Stage stage) {
		Controller.bind(this);
		try {
			String license = Resources.toString(getClass().getResource("/LICENSE.txt"),
					StandardCharsets.UTF_8);
			content.setText(license);

			dismiss.setOnAction(e -> stage.close());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Parent root() {
		return root;
	}
}
