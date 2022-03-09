package uk.ac.bris.cs.scotlandyard;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import io.github.fastclasspathscanner.FastClasspathScanner;
import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Transport;

import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * Manager for static resources such as game map and graph
 */
public final class ResourceManager {

	public enum ImageResource {ICON, MAP, UOB_LOGO}

	private Map<ImageResource, Image> imageResources;
	private Map<Ticket, Image> ticketResources;
	private Map<Integer, Entry<Integer, Integer>> mapCoordinates;
	private ImmutableValueGraph<Integer, ImmutableSet<Transport>> graph;

	/**
	 * Loads all resources into memory <br>
	 * This should be called before any resources are required
	 *
	 * @throws IOException if any of the resources cannot be found
	 */
	public void loadAllResources() throws IOException {

		// shared images
		imageResources = ImmutableMap.of(
				ImageResource.MAP, new Image(ScotlandYard.pngMapAsStream(), -1, -1, true, true),
				ImageResource.UOB_LOGO, loadImage("/uob_logo.png"),
				ImageResource.ICON, loadImage("/icon.png"));

		ticketResources = ImmutableMap.copyOf(Stream.of(Ticket.values()).collect(toMap(
				identity(),
				ticket -> loadImage(format("/tickets/%s.png", ticket.name().toLowerCase())))));

		mapCoordinates = ImmutableMap.copyOf(ScotlandYard.pngMapPositionEntries());
		graph = ScotlandYard.standardGraph();
	}

	private static Image loadImage(String path) {
		return new Image(path, -1, -1, true, true, false);
	}

	public Image getImage(ImageResource resource) { return imageResources.get(resource); }

	@Nonnull public Image getMap() { return getImage(ImageResource.MAP); }

	@Nonnull public Image getTicket(Ticket ticket) { return ticketResources.get(ticket); }

	@Nonnull
	public ImmutableValueGraph<Integer, ImmutableSet<Transport>> getGraph() { return graph; }

	@javax.annotation.Nullable public Point2D coordinateAtNode(int node) {
		var entry = mapCoordinates.get(node);
		return new Point2D(entry.getKey(), entry.getValue());
	}

	public static Ai instantiateAi(Class<Ai> cls) {
		try {
			return cls.getConstructor().newInstance();
		} catch (InstantiationException
				| IllegalAccessException
				| InvocationTargetException
				| NoSuchMethodException e) {
			throw new RuntimeException("Unable to create Ai instance of class " + cls, e);
		}
	}

	@SuppressWarnings("unchecked") public static ImmutableList<Ai> scanAis() {
		var found = new FastClasspathScanner()
				.enableAllInfo()
				.enableExternalClasses()
				.scan()
				.getClassesImplementing(Ai.class.getName());
		return found.stream().map(c -> {
			try {
				Class<Ai> clazz = (Class<Ai>) c.loadClass();
				if (!Ai.class.isAssignableFrom(clazz))
					throw new IllegalArgumentException(c + " does not implement " + Ai.class);
				return instantiateAi(clazz);
			} catch (Exception e) { throw new RuntimeException(e); }
		}).collect(ImmutableList.toImmutableList());
	}

}
