/**
 *  Program to provide a simple web UI to send an arbitrary uploaded image to
 *  a Zebra ZPL printer connected via USB.
 *
 *  Run this via "java -jar zebra-printer.jar" or similar.
**/
package org.riverbots.zebra;

import java.awt.image.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.*;
import io.undertow.*;
import io.undertow.Undertow.*;
import io.undertow.server.*;
import io.undertow.server.handlers.form.*;
import io.undertow.util.*;
import org.xnio.channels.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name="printer", mixinStandardHelpOptions = true, version = "zebra-printer 1.0",
         description="Run the app server to print images to a Zebra printer.")
public class Main implements Callable<Integer>, HttpHandler {
    @Option(names = { "--address" }, description = "Address on which to listen.")
    public String address = "0.0.0.0";
    @Option(names = { "--port" }, description = "Port number on which to listen.")
    public Integer port = 8080;

    Undertow server;

    public String getForm() {
        return "Hello!";
    }

    private class PostHandler implements HttpHandler {
        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            // Form data is stored here
            FormData formData = exchange.getAttachment(FormDataParser.FORM_DATA);
            // Iterate through form data
            for (String data : formData) {
                for (FormData.FormValue formValue : formData.get(data)) {
                    if (formValue.isFileItem()) {
                        // Process file here
                        File uploadedFile = formValue.getFileItem().getFile().toFile();
                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                        try {
                            BufferedImage img = ImageIO.read(uploadedFile);
                            ZPL zpl = new ZPL(img);
                            exchange.getResponseSender().send(zpl.toString());
                        } catch(IOException e) {
                            exchange.setResponseCode(500);
                            exchange.getResponseSender().send("Couldn't open file!");
                            return;
                        }
                        uploadedFile.delete();
                    }
                }
            }
        }
    }

    public void handleRequest(HttpServerExchange exchange) {
        Map<String,Deque<String>> req = exchange.getQueryParameters();
        StringWriter res = new StringWriter();
        PrintWriter ret = new PrintWriter(res);
        HeaderMap headers = exchange.getResponseHeaders();
        String method = exchange.getRequestMethod().toString();
        switch(method) {
        case "GET":
            headers.put(Headers.CONTENT_TYPE, "text/html");
            exchange.getResponseSender().send(getForm());
            break;
        case "POST":
            long length = exchange.getRequestContentLength();
            if(length <= 0 || length > 10485760) {
                exchange.setResponseCode(403);
                exchange.getResponseSender().send("Can't handle a POST of size " + length);
                return;
            }
            exchange.dispatch(new HttpHandler() {
                @Override
                public void handleRequest(HttpServerExchange exchange) throws Exception {
                    // Parses HTTP POST form data and passes it to a handler asynchronously
                    FormDataParser parser = FormParserFactory.builder().build().createParser(exchange);
                    PostHandler handler = new PostHandler();
                    parser.parse(handler);
                }
            });
            break;
        default:
            exchange.setResponseCode(403);
            exchange.getResponseSender().send("Can't handle a " + method + " request.");
            return;
        }
    }

    @Override
    public Integer call() throws Exception {
        Builder builder = Undertow.builder();
        builder = builder.addHttpListener(port, address);
        builder = builder.setHandler(this);
        server = builder.build();

        // Note to the peanut gallery.
        System.out.printf("Starting server on %s:%d\n", address, port);

        // Start the server.
        server.start();

        // Block forever.
        while(true) {
            Thread.sleep(86400*1000);
        }
    }

// Now for the main routine.
    public static void main(String... args) {
        Main m = new Main();
        int exitCode = new CommandLine(m).execute(args);
        System.exit(exitCode);
    }
}
