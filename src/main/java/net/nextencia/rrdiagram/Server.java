// Copyright (c) YugaByte, Inc.

package net.nextencia.rrdiagram;

import net.nextencia.rrdiagram.grammar.model.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

import java.io.IOException;
import java.io.OutputStream;
import java.io.File;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Handler;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.net.URLDecoder;
import java.net.URI;

interface HttpCodes {
  int OK = 200;
  int BAD_REQUEST = 400;
  int NOT_FOUND = 404;
}

class Helper {
  public static boolean writeHTTPResponse(HttpExchange httpExchange, int responseCode, String content,
      String contentType) {
    try {
      OutputStream outputStream = httpExchange.getResponseBody();
      httpExchange.getResponseHeaders().set("Content-Type", contentType);
      httpExchange.sendResponseHeaders(responseCode, content.length());
      outputStream.write(content.getBytes());
      outputStream.flush();
      outputStream.close();
      return true;
    } catch (java.io.IOException e) {
      e.printStackTrace(System.out);
      return false;
    }
  }

  public static boolean writeHTTPResponse(HttpExchange httpExchange, int responseCode, String content) {
    return writeHTTPResponse(httpExchange, responseCode, content, "text/plain; charset=us-ascii");
  }
}

class ServerException
    extends Exception {
  int httpErrorCode;

  public ServerException(int httpErrorCode, String errorMessage) {
    super(errorMessage);
    this.httpErrorCode = httpErrorCode;
  }
}

class BadRequest
    extends ServerException {
  public BadRequest(String errorMessage) {
    super(HttpCodes.BAD_REQUEST, errorMessage);
  }
}

class BNFHandler implements HttpHandler {

  Logger logger = Logger.getLogger(BNFHandler.class.getName());

  @Override
  public void handle(HttpExchange httpExchange) throws IOException {
    if ("GET".equals(httpExchange.getRequestMethod())) {
      handleRequest(httpExchange);
    }
  }

  public Map<String, String> parseParameters(HttpExchange exchange) {
    URI requestedUri = exchange.getRequestURI();
    String query = requestedUri.getRawQuery();
    Map<String, String> parameters = new HashMap<String, String>();
    if (query != null) {
      String pairs[] = query.split("[&]");

      for (String pair : pairs) {
        String param[] = pair.split("=");

        String key = null;
        String value = null;
        if (param.length > 1) {
          try {
            key = URLDecoder.decode(param[0], System.getProperty("file.encoding"));
            value = URLDecoder.decode(param[1], System.getProperty("file.encoding"));
          } catch (java.io.UnsupportedEncodingException ue) {
            ue.printStackTrace();
          }
        }

        if (key != null && value != null) {
          parameters.put(key, value);
        }
      }
    }
    return parameters;
  }

  public String cleanString(String input) {
    // whitespaces
    input = input.replaceAll("\\s*", "");
    // leading ,
    input = input.replaceAll("^,+", "");
    // trailing ,
    input = input.replaceAll(",+$", "");
    // multiple ,
    input = input.replaceAll(",+", ",");
    return input;
  }

  public void handleRequest(HttpExchange httpExchange) {
    int responseCode = HttpCodes.OK;
    try {
      // parse params
      Map<String, String> params = parseParameters(httpExchange);

      BNFProcessor bnfprocessor = BNFProcessor.get(params.get("api"), params.get("version"));
      String content = "";

      if (bnfprocessor == null) {
        // write the response
        throw new ServerException(HttpCodes.NOT_FOUND, "Unable to locate grammar file");
      }

      // fetch the data
      if (!params.containsKey("mode")) {
        throw new BadRequest("no mode specified");
      }
      if (params.get("mode").equals("grammar") || params.get("mode").equals("diagram")) {
        String strrules[] = null;
        String localrefs[] = null;
        if (params.containsKey("rules")) {
          strrules = params.get("rules").split(",");
        }

        if (params.containsKey("local")) {
          localrefs = cleanString(params.get("local")).split(",");
        }

        int depth = 0;
        if (params.containsKey("depth")) {
          try {
            depth = Integer.parseInt(params.get("depth"));
          } catch (NumberFormatException nfe) {
            throw new BadRequest("invalid depth:" + params.get("depth"));
          }
        }

        if (depth == 0) {
          throw new BadRequest("depth should be > 0");
        }

        if (strrules == null || strrules.length == 0) {
          throw new BadRequest("No rules specified");
        }

        List<Rule> rules = bnfprocessor.getTargetRules(strrules);
        List<Rule> localrules = bnfprocessor.getTargetRules(localrefs);
        if (params.get("mode").equals("diagram")) {
          content = bnfprocessor.getDiagram(rules, localrules, depth);
        } else {
          content = bnfprocessor.getGrammar(rules);
        }
      } else if (params.get("mode").equals("reference")) {
        content = bnfprocessor.getReferenceFile();
      } else {
        throw new BadRequest("invalid mode specified - " + params.get("mode"));
      }

      // write the response
      Helper.writeHTTPResponse(httpExchange, responseCode, content);
    } catch (ServerException e) {
      Helper.writeHTTPResponse(httpExchange, e.httpErrorCode, e.getMessage());
    } catch (Exception e) {
      e.printStackTrace(System.out);
    }
  }
}

class Server {
  static {
    System.setProperty("java.util.logging.SimpleFormatter.format", "Diagrams: [%4$s] %5$s %n");
  }

  // TODO: make this configurable
  int port = 1314; /* port at which the server listens */
  String host = "localhost";
  ThreadPoolExecutor threadPoolExecutor;
  HttpServer server;
  Logger logger = Logger.getLogger(Server.class.getName());

  public Server(String[] args) {
    this.parseArgs(args);
    this.start();
  }

  public void setDebugLog() {
    Logger rootLogger = LogManager.getLogManager().getLogger("");
    rootLogger.setLevel(Level.FINE);
    for (Handler h : rootLogger.getHandlers()) {
      h.setLevel(Level.FINE);
    }
  }

  public void parseArgs(String[] args) {
    if (args.length == 0 || !args[0].equals("--server")) {
      logger.severe("First argument must be --server");
      System.exit(1);
    }
    for (int i = 1; i < args.length; i++) {
      switch (args[i]) {
        case "--ebnf":
          i++;
          String bnffile = args[i];
          File f = new File(bnffile);
          if (f.exists() && !f.isDirectory()) {
            BNFProcessor.setDefault(new BNFProcessor(bnffile, "ysql", "preview"));
          } else {
            logger.severe("Unable to locate file: " + bnffile);
            System.exit(1);
          }
          break;
        case "--debug":
          setDebugLog();
          break;
        default:
          logger.severe("unsupported argument: " + args[i]);
          System.exit(1);
      }
    }
  }

  public void start() {
    threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
    try {
      server = HttpServer.create(new InetSocketAddress(host, port), 0);
    } catch (java.io.IOException e) {
      e.printStackTrace(System.out);
      return;
    }
    server.createContext("/ebnf", new BNFHandler());
    server.createContext("/shutdown", new HttpHandler() {
      @Override
      public void handle(final HttpExchange httpExchange) throws IOException {
        logger.warning("Diagrams server shutting down.");
        Helper.writeHTTPResponse(httpExchange, HttpCodes.OK, "Diagrams server will shutdown\n");
        server.stop(1);
        threadPoolExecutor.shutdownNow();
        logger.info("Diagrams server is shutdown.");
      }
    });

    server.setExecutor(threadPoolExecutor);
    server.start();
    logger.info("Diagrams Server started @ [" + host + ":" + port + "]");
  }
}
