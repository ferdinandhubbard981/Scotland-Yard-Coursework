package uk.ac.bris.cs.scotlandyard.ui.controller;

import com.google.common.io.Resources;

import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import uk.ac.bris.cs.fxkit.BindFXML;
import uk.ac.bris.cs.fxkit.Controller;
import uk.ac.bris.cs.scotlandyard.ui.Utils;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Controller for the user manual pages.<br> Not required for the coursework.
 */
@BindFXML("layout/Manual.fxml") final class ManualController implements Controller {

	@FXML private VBox root;
	@FXML private WebView content;

	ManualController(Stage stage) {
		Controller.bind(this);
		try {
			Parser parser = Parser.builder().build();
			Node document = parser
					.parse(Resources.toString(getResource("manual/MANUAL.md"), UTF_8));
			HtmlRenderer renderer = HtmlRenderer.builder().build();
			String index = Resources.toString(getResource("manual/index.html"), UTF_8);
			String rendered = index.replace("$content$", renderer.render(document));
			content.getEngine().loadContent(rendered);
		} catch (IOException e) {
			Utils.handleNonFatalException(e, "Unable to show manual");
			stage.close();
		}
	}

	@Override
	public Parent root() {
		return root;
	}
}
