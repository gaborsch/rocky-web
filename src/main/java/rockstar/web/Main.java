package rockstar.web;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
	
	public static final Logger logger = LogManager.getLogger(Main.class);
	public static final String JKS_FILE_NAME = "rockyrockstar.org.jks";

	public static void main(String[] args) throws Exception {
		logger.debug("Starting Rocky Web");		
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
		int sslport = getIntProperty("sslport", 8443);
		String disableRedirect = getStringProperty("disableRedirect", null);

		// create HTTP connector and add it to the Server
		if (port > 0) {
			server.addConnector(createHttpConnector(server, host, port));
		}


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

		HealthHandler healthHandler = new HealthHandler();
		RedirectHandler redirectHandler = new RedirectHandler();
		RockstarHandler processor = new RockstarHandler();
		ResourceHandler staticHandler = createStaticHandler();
		
		chain.addHandler(healthHandler);
		if (disableRedirect == null) {
			chain.addHandler(redirectHandler);
		} else {
			logger.debug("Redirect disabled");
		}
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
		logger.debug("Starting HTTP on host " + host + ":" + port);

		return connector;
	}

	private ServerConnector createHttpsConnector(Server server, String host, int sslport) {
		// Create a ServerConnector to accept connections from clients.
		logger.debug("Starting HTTPS on host " + host + ":" + sslport);

		HttpConfiguration https = new HttpConfiguration();
		https.addCustomizer(new SecureRequestCustomizer());

		SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
		sslContextFactory.setKeyStorePath(getJKSResource().toExternalForm());
		sslContextFactory.setKeyStorePassword("123456");
		sslContextFactory.setKeyManagerPassword("123456");

		logCertificate(sslContextFactory);

		ServerConnector sslConnector = new ServerConnector(server,
				new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
				new HttpConnectionFactory(https));

		sslConnector.setPort(sslport);
		return sslConnector;
	}

	private void logCertificate(SslContextFactory.Server sslContextFactory) {
		try {
			sslContextFactory.start();
			X509Certificate cert = (X509Certificate) sslContextFactory.getKeyStore().getCertificate("rockyrockstar.org");
			if (cert != null) {
				DateFormat df = DateFormat.getDateTimeInstance();
				Date startDate= cert.getNotBefore();
				Date endDate = cert.getNotAfter();
				logger.info("Cert is valid from  " + df.format(startDate) + " until " + df.format(endDate));
				if (endDate.before(new Date())) {
					logger.info("Cert is EXPIRED! Renew at sslforfree.com, install in the load balancer with CA bundle and private key!");
				}
			} else {
				logger.error("Cert not found!");
			}
		} catch (KeyStoreException e) {
			logger.error("Could not extract certificate from sslContextFactory", e);
		} catch (Exception e) {
			logger.error("Could not start sslContextFactory", e);
		} finally {
			if (sslContextFactory.isRunning()) {
				try {
					sslContextFactory.stop();
				} catch (Exception e) {
					logger.error("Could not stop sslContextFactory", e);
				}
			}
		}
	}

	private URL getJKSResource() {
		String jkspath = getStringProperty("jkspath", null);
		if (jkspath == null) {
			return Main.class.getResource("/" + JKS_FILE_NAME);
		}
		try {
			logger.debug("Using JKS from path: " + jkspath);
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
		logger.debug("serving: " + staticHandler.getBaseResource());
		return staticHandler;
	}

	private int getIntProperty(String name, int defaultValue) {
		String strValue = getStringProperty(name, null);
		int value = defaultValue;
		if (strValue != null) {
			logger.debug("Requested " + name + ": " + strValue);
			try {
				value = Integer.parseInt(strValue);
			} catch (NumberFormatException e) {
				logger.debug("Invalid " + name + ": " + strValue);
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
