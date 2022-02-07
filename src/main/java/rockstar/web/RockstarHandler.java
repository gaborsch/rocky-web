package rockstar.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.io.WriterOutputStream;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import rockstar.Rockstar;
import rockstar.RockstarApi;
import rockstar.parser.Parser;
import rockstar.runtime.Environment;
import rockstar.statement.Program;

public final class RockstarHandler extends AbstractHandler {
	private static final String PARAMETER_INP = "inp";
	private static final String PARAMETER_SRC = "src";
	private static final String PROGRAM_NAME_ROCKSTAR = "Rockstar";
	private static final String CONTENT_TYPE_TEXT_HTML_CHARSET_UTF_8 = "text/html;charset=utf-8";

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {

		String path = baseRequest.getPathInContext();

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
}