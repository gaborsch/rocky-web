package rockstar.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.StringJoiner;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class HealthHandler extends AbstractHandler {

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		String path = baseRequest.getPathInContext();

		if (path.startsWith("/health")) {
			response.setContentType("text/plain");
			response.setStatus(HttpServletResponse.SC_OK);
			baseRequest.setHandled(true);
			PrintWriter writer = response.getWriter();
			writer.println("OK\n");
			writer.println("Host IPs: " + getIpAddresses());
		}
	}
	
	public static String getIpAddresses ()  {
		Enumeration<NetworkInterface> ifs;
		try {
			StringJoiner sj = new StringJoiner(", ");
			ifs = NetworkInterface.getNetworkInterfaces();
			ifs.asIterator().forEachRemaining(ni -> {
				ni.getInetAddresses().asIterator().forEachRemaining(ia -> {
					if (!ia.isLoopbackAddress() && ia.isSiteLocalAddress())
					sj.add(ia.getHostAddress());
				});
			});
			return sj.toString();
		} catch (SocketException e) {
			return e.getMessage();
		}

	}
	
}
