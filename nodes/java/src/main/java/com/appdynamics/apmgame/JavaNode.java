package com.appdynamics.apmgame;

import net.sf.ehcache.Element;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;

import javax.json.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.net.URL;
import java.net.HttpURLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.HashSet;

import com.appdynamics.apm.appagent.api.AgentDelegate;
import com.appdynamics.apm.appagent.api.DataScope;
import com.appdynamics.apm.appagent.api.IMetricAndEventReporter;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class JavaNode {

protected static Cache cache;
private static IMetricAndEventReporter metricAndEventReporter;

private static final Logger logger = LoggerFactory.getLogger(JavaNode.class);

private static HashSet<DataScope> allScopes;

public static void main(String[] args) throws Exception {
        int port = 8080;
        CacheManager cacheManager;

        if (args.length > 0) {
                port = Integer.parseInt(args[0]);
        }

        String envAppConfig = System.getenv("APP_CONFIG");
        String envApmConfig = System.getenv("APM_CONFIG");

        if(envAppConfig == null) {
                envAppConfig = "{\"endpoints\":{\"http\":{}}}";
        }

        if(envApmConfig == null) {
                envApmConfig = "{}";
        }

        JsonReader jsonReader = Json.createReader(new StringReader(envAppConfig));
        JsonObject config = jsonReader.readObject();

        jsonReader = Json.createReader(new StringReader(envApmConfig));
        JsonObject apmConfig = jsonReader.readObject();

        CacheManager cm = CacheManager.getInstance();

        cm.addCache("cache1");

        cache = cm.getCache("cache1");

        Server server = new Server(port);
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);

        NodeServlet.setConfig(config, apmConfig);

        handler.addServletWithMapping(NodeServlet.class, "/*");

        server.start();
        server.join();
}

@SuppressWarnings("serial")
public static class NodeServlet extends HttpServlet {

        protected static JsonObject config;
        protected static JsonObject apmConfig;
        protected static JsonObject endpoints;

        public static void setConfig(JsonObject config, JsonObject apmConfig) {
                NodeServlet.config = config;
                NodeServlet.apmConfig = apmConfig;
                NodeServlet.endpoints = config.getJsonObject("endpoints").getJsonObject("http");
        }

        public static boolean hasMetricAndEventReporter() {
                // Depending on the setup this might throw a ClassNotFoundException or a java.lang.ExceptionInInitializerError
                // This solution is not perfect, but makes the code robust for running without agent, with dynamic attach, etc.
                try {
                        metricAndEventReporter = AgentDelegate.getMetricAndEventPublisher();
                        allScopes = new HashSet<DataScope>();
                        allScopes.add(DataScope.SNAPSHOTS);
                        allScopes.add(DataScope.ANALYTICS);
                        return true;
                } catch ( Throwable t ) {
                        logger.debug("Could not get hold of metricAndEventReporter: " + t.getMessage());
                        return false;
                }
        }

        protected String buildResponse(int timeout) {
                long start = System.currentTimeMillis();
                long finish = start;
                String response = "";
                while(finish - start < timeout) {
                        response += " ";
                        finish = System.currentTimeMillis();
                }
                return response.length() + "slow response";
        }

        protected String loadFromCache(int timeout) {
                long start = System.currentTimeMillis();
                long finish = start;
                int i = 0;
                Integer element = new Integer(0);
                while(finish - start < timeout) {
                        i++;
                        element = new Integer(i);
                        cache.putIfAbsent(new Element(element, i));
                        finish = System.currentTimeMillis();
                }
                return "Cache result: " + cache.get(element).toString();
        }

        protected String queryDatabase(String call, boolean catchExceptions, int remoteTimeout) throws IOException {
                try {
                        String url = "jdbc:my" + call.split("\\?")[0] + "?useSSL=false&allowMultiQueries=true";
                        Connection connection = DriverManager.getConnection(url, "root", "root");

                        PreparedStatement stmt = connection.prepareStatement(call.split("\\?query=")[1]);
                        stmt.execute();

                        connection.close();
                } catch (SQLException e) {
                        if(catchExceptions) {
                                return e.getMessage();
                        }
                        throw new IOException(e.getMessage());
                }
                return "Database query executed: " + call;
        }

        protected String callRemote(String call, boolean catchExceptions, int remoteTimeout, String uniqueSessionId) throws IOException {
                try {
                        URL url = new URL(call + "?unique_session_id=" + uniqueSessionId);
                        // return new Scanner( url.openStream() ).useDelimiter( "\\Z" ).next();
                        HttpURLConnection con = (HttpURLConnection) url.openConnection();

                        con.setConnectTimeout(remoteTimeout);
                        con.setReadTimeout(remoteTimeout);
                        con.setRequestProperty("Content-Type", "application/json");

                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(con.getInputStream()));
                        String inputLine;
                        StringBuffer response = new StringBuffer();

                        while ((inputLine = in.readLine()) != null) {
                                response.append(inputLine);
                        }
                        in.close();

                        return response.toString();

                } catch (IOException e) {
                        if(catchExceptions) {
                                return e.getMessage();
                        }
                        throw e;
                }
        }

        protected String logMessage(String level, String msg) {
                switch(level) {
                case "warn":
                        logger.warn(msg);
                        break;
                case "error":
                        logger.error(msg);
                        break;
                case "debug":
                        logger.debug(msg);
                        break;
                case "trace":
                        logger.trace(msg);
                        break;
                default:
                        logger.info(msg);
                }
                return "Logged (" + level + "): " + msg;
        }

        protected String processCall(String call, boolean catchExceptions, int remoteTimeout, String uniqueSessionId) throws IOException {
                logger.info("Processing call: {}", call);
                if (call.startsWith("sleep")) {
                        int timeout = Integer.parseInt(call.split(",")[1]);
                        try {
                                Thread.sleep(timeout);
                        } catch (InterruptedException e) {

                        }
                        return "Slept for " + timeout;
                }

                if(call.startsWith("log")) {
                        String[] log = call.split(",");
                        if(log.length > 2) {
                                return this.logMessage(log[1], log[2]);
                        }
                        return this.logMessage("info", log[1]);
                }

                if (call.startsWith("slow")) {
                        int timeout = Integer.parseInt(call.split(",")[1]);
                        return this.buildResponse(timeout);
                }

                if (call.startsWith("cache")) {
                        int timeout = Integer.parseInt(call.split(",")[1]);
                        return this.loadFromCache(timeout);
                }

                if (call.startsWith("http://")) {
                        return this.callRemote(call, catchExceptions, remoteTimeout, uniqueSessionId);
                }
                if (call.startsWith("sql://")) {
                        return this.queryDatabase(call, catchExceptions, remoteTimeout);
                }
                if (call.startsWith("error")) {
                        throw new HttpException(500, "error");
                }
                if (call.startsWith("image")) {
                        String src = call.split(",")[1];
                        return "<img src='"+src+"' />";
                }
                if (call.startsWith("script")) {
                        String src = call.split(",")[1];
                        return "<script src='"+src+"?output=javascript'></script>";
                }
                if (call.startsWith("ajax")) {
                        String src = call.split(",")[1];
                        return "<script>var o = new XMLHttpRequest();o.open('GET', '"+src+"');o.send();</script>";
                }
                return ":" + call + " is not supported";
        }

        protected String processIntData(String id, long value) {
                return metricAndEventReporter.addSnapshotData(id, value, allScopes) ? id + " data added" : id + " data not added";
        }

        protected String processDoubleData(String id, double value) {
                return metricAndEventReporter.addSnapshotData(id, value, allScopes) ? id + " data added" : id + " data not added";
        }

        protected String processStringData(String id, String value) {
                return metricAndEventReporter.addSnapshotData(id, value, allScopes) ? id + " data added" : id + " data not added";
        }

        protected String processData(JsonObject data) {
                if(!hasMetricAndEventReporter()) {
                        return "Data not processed: no agent installed.";
                }

                if(!data.containsKey("id")) {
                        return "Data not processed: No id provided";
                }

                String id = data.getString("id");
                String type = data.containsKey("type")  ? data.getString("type").toLowerCase() : "string";
                String chance = data.containsKey("chance") ? data.getString("chance").toLowerCase() : "";

                String value = "";
                
                if ( !chance.equals("")) {
                        String[] chanceSplit = chance.split(",");

                        switch(chanceSplit[0]){
                            case "guid":
                                value = UUID.randomUUID().toString();
                                break;
                            case "integer":
                                int min = Integer.parseInt(chanceSplit[1].split(":")[1]);
                                int max = Integer.parseInt(chanceSplit[2].split(":")[1]);
                                value = String.valueOf(ThreadLocalRandom.current().nextInt(min, max + 1));
                                break;
                        }
                        switch(type) {
                                case "int":
                                        return processIntData(id, Integer.parseInt(value));
                                case "double":
                                        return processDoubleData(id, Double.parseDouble(value));
                                default:
                                        return processStringData(id, value);
                        }

                } else {
                        if(!data.containsKey("value")) {
                                return "Data not processed: No value provided";
                        }
                        JsonValue valueJson = data.get("value");
                        if (valueJson.getValueType() == JsonValue.ValueType.ARRAY) {
                                JsonArray arr = (JsonArray) valueJson;
                                int index = ThreadLocalRandom.current().nextInt(arr.size());
                                valueJson = arr.get(index);
                        }
                        switch(type) {
                                case "int":
                                        return processIntData(id, ((JsonNumber)valueJson).longValue());
                                case "double":
                                        return processDoubleData(id, ((JsonNumber)valueJson).doubleValue());
                                default:
                                        return processStringData(id, ((JsonString)valueJson).getString());
                                }                        
                }

        }

        protected String preProcessCall(JsonValue call, String uniqueSessionId) throws IOException {

                boolean catchExceptions = true;
                int remoteTimeout = Integer.MAX_VALUE;

                if (call.getValueType() == JsonValue.ValueType.ARRAY) {
                        JsonArray arr = (JsonArray) call;
                        int index = ThreadLocalRandom.current().nextInt(arr.size());
                        call = arr.get(index);
                }
                if (call.getValueType() == JsonValue.ValueType.OBJECT) {
                        JsonObject obj = (JsonObject) call;
                        call = obj.getJsonString("call");

                        if(((JsonString) call).getString().startsWith("data")) {
                                return this.processData(obj);
                        }

                        if(obj.containsKey("probability")) {
                                double probability = obj.getJsonNumber("probability").doubleValue();
                                if (probability * 100 < ThreadLocalRandom.current().nextInt(100)) {
                                        return call + " was not probable";
                                }
                        }
                        if(obj.containsKey("catchExceptions")) {
                                catchExceptions = obj.getBoolean("catchExceptions");
                        }
                        if(obj.containsKey("remoteTimeout")) {
                                remoteTimeout = obj.getInt("remoteTimeout");
                        }
                }
                return this.processCall(((JsonString) call).getString(), catchExceptions, remoteTimeout, uniqueSessionId);
        }

        public void handleEndpoint(HttpServletRequest request, HttpServletResponse response, JsonArray endpoint, boolean withEum, String uniqueSessionId) throws IOException {
                response.setStatus(HttpServletResponse.SC_OK);

                StringBuilder result = new StringBuilder();

                for (JsonValue entry : endpoint) {
                        result.append(this.preProcessCall(entry, uniqueSessionId));
                }

                if(withEum) {

                        String adrum = "";
                        String locationCustom = "";
                        String adrumCustom = "";

                        logger.debug("city: {}", request.getParameter("city"));
                        logger.debug("region: {}", request.getParameter("region"));
                        logger.debug("country: {}", request.getParameter("country"));
                        
                        if(request.getParameter("city") != null && request.getParameter("region") != null && request.getParameter("country") != null) {
                                locationCustom = "<script type='text/javascript' charset='UTF-8'>window['adrum-config'] = (function(config) { config.geo = { localIP: '192.168.1.100', city: '" + request.getParameter("city") + "', region: '" + request.getParameter("region") + "', country: '" + request.getParameter("country") + "' }; return config;})(window['adrum-config'] || {});</script>";
                                logger.debug("Location: {}", locationCustom);
                        }

                        try  {
                                if ( NodeServlet.config.getString("adrumCustom") != null ) {
                                        String fileName = System.getenv("CUSTOM_CODE_DIR") + "/" + NodeServlet.config.getString("adrumCustom");
                                        logger.info("adrumCustom fileName: {}", fileName);
                                        adrumCustom = new String(Files.readAllBytes(Paths.get(fileName)));
                                        adrumCustom = "<script type='text/javascript' charset='UTF-8'>" + adrumCustom + "</script>";
                                        logger.debug("adrumCustom content: {}", adrumCustom);

                                }
                        } catch(Exception e) {
                                // e.printStackTrace();
                        }

                        adrum = "<script type='text/javascript' charset='UTF-8'>window['adrum-start-time'] = new Date().getTime();window['adrum-config'] = " + NodeServlet.apmConfig.getJsonObject("eum") + "</script>"+ locationCustom + adrumCustom + "<script src='//cdn.appdynamics.com/adrum/adrum-latest.js' type='text/javascript' charset='UTF-8'></script>";

                        response.getWriter().println("<!doctype html><html lang=\"en\"><head><title>" + NodeServlet.config.getString("name") + "</title>" + adrum + "<body>" + result);

                } else {
                        response.getWriter().println(result);
                }
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

                String uniqueSessionId = "";
                if(request.getParameter("unique_session_id") != null) {
                        uniqueSessionId = request.getParameter("unique_session_id");
                        response.addHeader("uniqueSessionId", uniqueSessionId);
                }

                String endpoint = request.getRequestURI();
                logger.info("Endpoint: {}", endpoint);

                boolean withEum = NodeServlet.apmConfig.containsKey("eum");

                String contentType = request.getContentType();

                if("application/json".equals(contentType) || "javascript".equals(request.getParameter("output"))) {
                        response.setContentType(contentType);
                        withEum = false;
                } else if (contentType == null) {
                        response.setContentType("text/html;charset=utf-8");
                }

                response.addHeader("Access-Control-Allow-Origin", "*");
                response.addHeader("Access-Control-Allow-Methods","GET, POST, OPTIONS, PUT, PATCH, DELETE");
                response.addHeader("Access-Control-Allow-Credentials", "true");
                response.addHeader("Timing-Allow-Origin", "*");
                
                if ( !uniqueSessionId.equals("") && hasMetricAndEventReporter()) {
                        metricAndEventReporter.addSnapshotData("uniqueSessionId", uniqueSessionId, allScopes);
                }

                try {
                        if (NodeServlet.endpoints.containsKey(endpoint)) {
                                this.handleEndpoint(request, response, NodeServlet.endpoints.getJsonArray(endpoint), withEum, uniqueSessionId);
                        } else if (NodeServlet.endpoints.containsKey(endpoint.substring(1))) {
                                this.handleEndpoint(request, response, NodeServlet.endpoints.getJsonArray(endpoint.substring(1)), withEum, uniqueSessionId);
                        } else {
                                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                                response.getWriter().println(404);
                        }
                } catch (HttpException e) {
                        response.setStatus(e.getCode());
                        response.getWriter().println(e.getMessage());
                } catch (IOException e) {
                        response.setStatus(500);
                        response.getWriter().println(e.getMessage());
                }
        }
        }
}
