package japath3.service;

import static japath3.util.Basics.toInt;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.json.JSONObject;

import japath3.cli.Commands;
import japath3.util.JoeUtil;
import japath3.wrapper.NodeFactory;

public class HttpService {
	
	public static Logger logger = Logger.getLogger("japath");

	static String helpJo = "General apath operation <_op> that consumes input (via <_body>), ckecks it against constraints/schema <constraints>,\n"
			+ "evaluates the expression <apathExpr>, and (optionally) get the value of variable <var>. Let R be the result then\n"
			+ "- R will be outputted (_op=select)\n"
			+ "- a schema of R will be generated (_op=schemaGen)\n"
			+ "- a select expression of R will be generated (_op=selectGen)\n"
			+ "\n"
			+ "Json properties:\n"
			+ "   _op: command (see above, default: select), \n"
			+ "   type: source language type (json|xml, default: json), \n"
			+ "   _body: input, \n"
			+ "   apathExpr: apath expression. if missing the input itself will be processed, \n"
			+ "   var: optional variable name. if empty string all variables are retrieved, \n"
			+ "   constraints: optional apath constraints/schema \n"
			+ "   salient: checking selector usage (true/false) \n"
			+ "   pretty: pretty output (true/false) \n"
			+ "   asArray: output as an array ([...]) (true/false) \n"
			+ "   schemaGenSwitches: optional comma-separated switches (w/o whitespace) for schema generation (default: all off). " 
			+ "'opt': all selectors (properties) are optional\n"
//			+ "'selectorRestriction': only selectors of input allowed \n"
			+ "\n"
			+ "Json example 1:\n"
			+ "{\r\n"
			+ "   \"_body\": {\"x\":{\"y\":1,\"z\":\"hi\"}},,\r\n"
			+ "   \"apathExpr\": \"x.y\"\r\n"
			+ "}\n"
			+ "\n"
			+ "Result:\n"
			+ "1\n"
			+ "\n"
			+ "Json example 2:\n"
			+ "{\r\n"
			+ "   \"_op\": \"select\",\r\n"
			+ "   \"_body\": {\"x\":{\"y\":1,\"z\":\"hi\"}},\r\n"
			+ "   \"apathExpr\": \"x$v.y\",\r\n"
			+ "   \"var\": \"v\",\r\n"
			+ "   \"constraints\": \"and(x.and(y.type(Number),z.type(String)))\",\r\n"
			+ "   \"pretty\": true,\r\n"
			+ "   \"asArray\": true\r\n"
			+ "}\n"
			+ "\n"
			+ "Result:\n"
			+ "[{\r\n"
			+ "   \"y\": 1,\r\n"
			+ "   \"z\": \"hi\"\r\n"
			+ "}]\n"
			;

	public static HttpServer server;

	public static void main(String[] args) throws Exception {

		SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(15000).setTcpNoDelay(true).build();
		
		int port = args.length == 0 ? 8082 : toInt(args[0]);
		server = ServerBootstrap.bootstrap()
				.setListenerPort(port)
				.setServerInfo("Test/1.1")
				.setSocketConfig(socketConfig)
				// .setExceptionLogger(new StdErrorExceptionLogger())
				.registerHandler("*", new HttpRequestHandler() {

					@Override public void handle(HttpRequest request, HttpResponse response, HttpContext context)
							throws HttpException,
							IOException {

						String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);
						if (!method.equals("POST")) {
							throw new MethodNotSupportedException(method + " method not supported");
						}
						String uri = request.getRequestLine().getUri();
						if (!uri.equals("/apath/eval")) {
							if (uri.equals("/apath/help")) {
								response.setEntity(new StringEntity(helpJo, "utf-8"));
								return;
							}
							throw new MethodNotSupportedException("'http://<host>:<port>/apath[/(eval|help)]' expected");
						}

						try {
							if (request instanceof HttpEntityEnclosingRequest) {
								JSONObject joReq = JoeUtil.createJoe(
										IOUtils.toString(((HttpEntityEnclosingRequest) request).getEntity().getContent(), "utf-8"));
								response.setEntity(new StringEntity(Commands.exec( NodeFactory.w_(joReq)), "utf-8"));
							} else {
								throw new MethodNotSupportedException("no input given");
							}

						} catch (Exception e) {
//							e.printStackTrace();
							response.setStatusCode(400);
							response.setEntity(new StringEntity(e.getMessage(), "utf-8"));
						}
					}
				})
				.create();

		logger.info("server started at port: " + port);
		server.start();
		server.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override public void run() { server.shutdown(5, TimeUnit.SECONDS); }
		});
	}
}
