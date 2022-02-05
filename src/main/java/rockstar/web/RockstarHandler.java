package rockstar.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;

import org.eclipse.jetty.io.WriterOutputStream;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import rockstar.RockstarApi;
import rockstar.runtime.Environment;

public final class RockstarHandler extends AbstractHandler {
	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {

		String path = baseRequest.getPathInContext();

		if (path.startsWith("/execute")) {
			System.out.println(
					"Processor captured " + baseRequest.getPathInContext() + "?" + baseRequest.getQueryString());

			response.setContentType("text/html;charset=utf-8");
			response.setStatus(HttpServletResponse.SC_OK);
			baseRequest.setHandled(true);

			String source = request.getParameter("src");
			String input = request.getParameter("inp");
			String output = run(source, input != null ? input : "");

			PrintWriter writer = response.getWriter();
//			writer.println("<h1>Output:</h1>\n<hr />\n<code>");
//			writer.println(output.replaceAll("\r?\n", "\n<br>\n"));
//			writer.println("</code>");
//			writer.println("<hr />\nDate: " + new Date());

			writer.println(output);
			writer.println("(Date: " + new Date() + ")");
		} else {
			System.out.println(
					"Processor passed " + baseRequest.getPathInContext() + ", ?: " + baseRequest.getQueryString());
		}
	}

	private String run(String source, String input) throws IOException {
		RockstarApi api = new RockstarApi();

		CharArrayWriter outWriter = new CharArrayWriter();
		OutputStream output = new WriterOutputStream(outWriter, StandardCharsets.UTF_8.name());
		// error is redirected to output
		api.setEnv(createEnvironment(input, output, output));

		// run Rocky
		api.run("Rockstar", source);

		return outWriter.toString();
	}

	private Environment createEnvironment(String input, OutputStream output, OutputStream error) {
		InputStream is = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
		PrintStream opw = new PrintStream(output, true, StandardCharsets.UTF_8);
		PrintStream epw = new PrintStream(error, true, StandardCharsets.UTF_8);
		return Environment.create(is, opw, epw, new HashMap<String, String>());
	}

}