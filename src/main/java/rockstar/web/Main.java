package rockstar.web;

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
		connector.setPort(8080);

		// The address to bind to.
		connector.setHost("127.0.0.1");

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
		
		ResourceHandler staticHandler = new ResourceHandler();
		staticHandler.setResourceBase("./public");
		
		chain.addHandler(processor);
		chain.addHandler(staticHandler);
		chain.addHandler(new DefaultHandler());
		

		// Start the Server so it starts accepting connections from clients.
		server.start();	
		server.join();
	}

}
