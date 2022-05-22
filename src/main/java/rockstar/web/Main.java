package rockstar.web;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class Main {
	
	public static final String JKS_FILE_NAME = "rockyrockstar.org.jks";

	public static void main(String[] args) throws Exception {
		System.out.println("Starting Rocky Web");		
		new Main().run();
	}

	private void run() throws Exception {
		// Create and configure a ThreadPool.
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setName("server");

		// Configure TCP/IP parameters.
		// Create a Server instance.
		Server server = new Server(threadPool);

		// The address to bind to.
		String host = getStringProperty("host", "127.0.0.1");
		int port = getIntProperty("port", 8080);

		// create HTTP connector and add it to the Server
		if (port > 0) {
			server.addConnector(createHttpConnector(server, host, port));
		}

		int sslport = getIntProperty("sslport", 8443);

		// create HTTPS connector and add it to the Server
		if (sslport > 0) {
			server.addConnector(createHttpsConnector(server, host, sslport));
		}

		// Set Handlers to handle requests/responses.
		HandlerCollection sequence = new HandlerCollection();
		server.setHandler(sequence);

		HandlerList chain = new HandlerList();
//		DebugHandler logger = new DebugHandler();
//		logger.setHandler(new DefaultHandler());

		sequence.addHandler(chain);
//		sequence.addHandler(logger);

		RedirectHandler redirectHandler = new RedirectHandler();
		RockstarHandler processor = new RockstarHandler();
		HealthHandler healthHandler = new HealthHandler();
		ResourceHandler staticHandler = createStaticHandler();
		
		chain.addHandler(healthHandler);
		chain.addHandler(redirectHandler);
		chain.addHandler(processor);
		chain.addHandler(staticHandler);
		chain.addHandler(new DefaultHandler());

		// Start the Server so it starts accepting connections from clients.
		server.start();
		server.join();
	}

	private ServerConnector createHttpConnector(Server server, String host, int port) {
		// Create a ServerConnector to accept connections from clients.
		ServerConnector connector = new ServerConnector(server);

		// The port to listen to.
		connector.setPort(port);
		connector.setHost(host);
		System.out.println("Starting HTTP on host " + host + ":" + port);

		return connector;
	}

	private ServerConnector createHttpsConnector(Server server, String host, int sslport) {
		// Create a ServerConnector to accept connections from clients.
		System.out.println("Starting HTTPS on host " + host + ":" + sslport);

		HttpConfiguration https = new HttpConfiguration();
		https.addCustomizer(new SecureRequestCustomizer());

		SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
		sslContextFactory.setKeyStorePath(getJKSResource().toExternalForm());
		sslContextFactory.setKeyStorePassword("123456");
		sslContextFactory.setKeyManagerPassword("123456");

		ServerConnector sslConnector = new ServerConnector(server,
				new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
				new HttpConnectionFactory(https));

		sslConnector.setPort(sslport);
		return sslConnector;
	}

	private URL getJKSResource() {
		String jkspath = getStringProperty("jkspath", null);
		if (jkspath == null) {
			return Main.class.getResource("/" + JKS_FILE_NAME);
		}
		try {
			System.out.println("Using JKS from path: " + jkspath);
			return new URL("file://"+jkspath);
		} catch (MalformedURLException e) {
			throw new RuntimeException("Invalid jkspath: " + jkspath, e);
		}
	}

	private ResourceHandler createStaticHandler() {
		ResourceHandler staticHandler = new ResourceHandler();

		String baseStr = "/public";
		URL baseUrl = Main.class.getResource(baseStr);
		String basePath = baseUrl.toExternalForm();

		staticHandler.setWelcomeFiles(new String[] { "index.html" });
		staticHandler.setResourceBase(basePath);
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
