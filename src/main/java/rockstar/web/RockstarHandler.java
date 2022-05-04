package rockstar.web;

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.io.WriterOutputStream;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import rockstar.RockstarApi;
import rockstar.parser.ParseException;
import rockstar.runtime.Environment;

public final class RockstarHandler extends AbstractHandler {
	private static final String PARAMETER_INP = "inp";
	private static final String PARAMETER_SRC = "src";
	private static final String PROGRAM_NAME_ROCKSTAR = "Rockstar";
	private static final String CONTENT_TYPE_TEXT_HTML_CHARSET_UTF_8 = "text/html;charset=utf-8";

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {

		String path = baseRequest.getPathInContext();
		
		//log("Request path: "+path);

		try {
			if (path.startsWith("/execute")) {
				response.setContentType(CONTENT_TYPE_TEXT_HTML_CHARSET_UTF_8);
				response.setStatus(HttpServletResponse.SC_OK);
				baseRequest.setHandled(true);
				String source = request.getParameter(PARAMETER_SRC);
				String input = request.getParameter(PARAMETER_INP);
				String output = run(source, input != null ? input : "");
				PrintWriter writer = response.getWriter();
				writer.println(output);
			} else if (path.startsWith("/list")) {
				response.setContentType(CONTENT_TYPE_TEXT_HTML_CHARSET_UTF_8);
				response.setStatus(HttpServletResponse.SC_OK);
				baseRequest.setHandled(true);
				String source = request.getParameter(PARAMETER_SRC);
				String output = list(source);
				PrintWriter writer = response.getWriter();
				writer.println(output);
			} else if (path.startsWith("/explain")) {
				response.setContentType(CONTENT_TYPE_TEXT_HTML_CHARSET_UTF_8);
				response.setStatus(HttpServletResponse.SC_OK);
				baseRequest.setHandled(true);
				String source = request.getParameter(PARAMETER_SRC);
				String output = explain(source);
				PrintWriter writer = response.getWriter();
				writer.println(output);
			}
		} catch (ParseException e) {
			response.setContentType(CONTENT_TYPE_TEXT_HTML_CHARSET_UTF_8);
			response.setStatus(HttpServletResponse.SC_OK);
			baseRequest.setHandled(true);
			PrintWriter writer = response.getWriter();
			writer.println(e.getMessage());
		} catch (Exception e) {
			response.setContentType(CONTENT_TYPE_TEXT_HTML_CHARSET_UTF_8);
			response.setStatus(HttpServletResponse.SC_OK);
			baseRequest.setHandled(true);
			PrintWriter writer = response.getWriter();
			writer.println("Internal error");
			log(e);			
		}

	}

	private String run(String source, String input) throws IOException {
		RockstarApi api = new RockstarApi();

		CharArrayWriter outWriter = new CharArrayWriter();
		api.setEnv(createEnvironment(input, outWriter));

		// run Rocky
		api.run(PROGRAM_NAME_ROCKSTAR, source);

		return outWriter.toString();
	}

	public String list(String fileContent) {
		RockstarApi api = new RockstarApi();

		CharArrayWriter outWriter = new CharArrayWriter();
		api.setEnv(createEnvironment(outWriter));

		return api.list(PROGRAM_NAME_ROCKSTAR, fileContent);
	}

	public String explain(String fileContent) {
		RockstarApi api = new RockstarApi();

		CharArrayWriter outWriter = new CharArrayWriter();
		api.setEnv(createEnvironment(outWriter));

		return api.explain(PROGRAM_NAME_ROCKSTAR, fileContent);
	}

	private Environment createEnvironment(Writer output) {
		OutputStream outStream = new WriterOutputStream(output, StandardCharsets.UTF_8.name());
		// no input, error is redirected to output
		return createEnvironment("", outStream, outStream);
	}

	private Environment createEnvironment(String input, Writer output) {
		OutputStream outStream = new WriterOutputStream(output, StandardCharsets.UTF_8.name());
		// error is redirected to output
		return createEnvironment(input, outStream, outStream);
	}

	private Environment createEnvironment(String input, OutputStream output, OutputStream error) {
		InputStream is = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
		PrintStream opw = new PrintStream(output, true, StandardCharsets.UTF_8);
		PrintStream epw = new PrintStream(error, true, StandardCharsets.UTF_8);
		Map<String, String> options = new HashMap<>();
		options.put("--disable-native-java", "--disable-native-java");
		return Environment.create(is, opw, epw, options);
	}
	
	private void log(String message) {
		System.out.println(message);
	}

	private void log(Exception e) {
		System.out.println(e.getMessage());
		e.printStackTrace(System.out);
	}
}