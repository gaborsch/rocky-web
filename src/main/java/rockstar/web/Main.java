package rockstar.web;

import java.net.URL;

import org.eclipse.jetty.http.HttpContent.ContentFactory;
import org.eclipse.jetty.server.ResourceContentFactory;
import org.eclipse.jetty.server.ResourceService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DebugHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class Main {

	public static void main(String[] args) throws Exception {
		System.out.println("Starting Rocky Web");
		new Main().run();
	}

	private void run() throws Exception {
		// Create and configure a ThreadPool.
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setName("server");

		// Create a Server instance.
		Server server = new Server(threadPool);

		// Create a ServerConnector to accept connections from clients.
		ServerConnector connector = new ServerConnector(server);

		// Configure TCP/IP parameters.
		// The port to listen to.
		int port = getIntProperty("port", 8080);
		connector.setPort(port);

		// The address to bind to.
		String host = getStringProperty("host", "127.0.0.1");
		connector.setHost(host);
		System.out.println("Starting on host " + host + ":" + port);

		// Add the Connector to the Server
		server.addConnector(connector);

		// Set Handlers to handle requests/responses.
		HandlerCollection sequence = new HandlerCollection();
		server.setHandler(sequence);

		HandlerList chain = new HandlerList();
//		DebugHandler logger = new DebugHandler();
//		logger.setHandler(new DefaultHandler());

		sequence.addHandler(chain);
//		sequence.addHandler(logger);

		RockstarHandler processor = new RockstarHandler();
		HealthHandler healthHandler = new HealthHandler();
		ResourceHandler staticHandler = createStaticHandler();

		chain.addHandler(processor);
		chain.addHandler(staticHandler);
		chain.addHandler(healthHandler);
		chain.addHandler(new DefaultHandler());

		// Start the Server so it starts accepting connections from clients.
		server.start();
		server.join();
	}

	private ResourceHandler createStaticHandler() {
		ResourceHandler staticHandler = new ResourceHandler();

	    String  baseStr  = "/public";
	    URL     baseUrl  = Main.class.getResource( baseStr ); 
	    String  basePath = baseUrl.toExternalForm();

	    staticHandler.setWelcomeFiles(new String[]{ "index.html" });
	    staticHandler.setResourceBase( basePath );
	    System.out.println("serving: " + staticHandler.getBaseResource());		
		return staticHandler;
	}

	private int getIntProperty(String name, int defaultValue) {
		String strValue = getStringProperty(name, null);
		int value = defaultValue;
		if (strValue != null) {
			System.out.println("Requested " + name + ": " + strValue);
			try {
				value = Integer.parseInt(strValue);
			} catch (NumberFormatException e) {
				System.out.println("Invalid " + name + ": " + strValue);
			}
		}
		return value;
	}

	private String getStringProperty(String name, String defaultValue) {
		String strValue = System.getenv(name);
		if (strValue == null) {
			strValue = System.getProperty(name);
		}
		if (strValue == null) {
			strValue = defaultValue;
		}
		return strValue;
	}

}
