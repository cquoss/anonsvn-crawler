package de.unioninvestment.md.dp.example.anonsvncrawler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnonsvnCrawler {

    private static final Logger LOGGER = LogManager.getLogger();

    private Path directory;

    void run(final String [] args) throws Exception {
        if (args.length != 2) {
            throw new RuntimeException("USAGE: java "+ AnonsvnCrawler.class.getCanonicalName() + " uri directory");
        }
        directory = Paths.get(args[1]);
        if (Files.exists(directory)) {
            if (Files.isDirectory(directory)) {
                // ok
            } else {

            }
        }
        recurse(args[0], Paths.get(""));
    }

    void recurse(final String address, final Path p) throws Exception {
        String methodName = "recurse(URI, Path)";
        List<String> lines = matchLines(getPage(address, p));
        LOGGER.debug("{} [lines={}]", methodName, lines);
        for (String line : lines) {
            if ("../". equals(line) || ".cvsignore".equals(line)) {
                // do nothing
            } else if (line.endsWith("/")) {
                recurse(address, createDirectory(p, line.substring(0, line.length() - 1)));
            } else {
                throw new RuntimeException("Unsupported entry: " + line);
            }
        }
    }

    Path createDirectory(final Path p, final String s) throws Exception {
        Path p2 = Paths.get(p.toString(), s);
        Path p3 = Paths.get(directory.toString(), p2.toString());
        if (Files.exists(p3)) {
            if (Files.isDirectory(p3)) {
                LOGGER.info("Directory {} already exists.", p3.toAbsolutePath());
            } else {
                throw new RuntimeException("Entry " + p3.toAbsolutePath() + " exists and is no directory as it should be.");
            }
        } else {
            Files.createDirectory(p3);
            LOGGER.info("Directory {} created.", p3.toAbsolutePath());
        }
        return p2;
    }

    List<String> matchLines(final String s) throws Exception {
        String methodName = "matchLines(String)";
        LOGGER.trace("{} start [s={}]", methodName, s);
        Pattern p = Pattern.compile("<li><a href=\"(.*)\">.*</a></li>");
        Matcher m = p.matcher(s);
        List<String> l = new LinkedList<>();
        while (m.find()) {
            String group = m.group(1);
            LOGGER.debug("{} [group={}]", methodName, group);
            l.add(group);
        }
        LOGGER.trace("{} end [l={}]", methodName, l);
        return l;
    }

    String getPage(final String address, final Path p) throws Exception {
        String methodName = "getPage(String, Path)";
        String s = p.toString();
        LOGGER.debug("{} [s={}]", methodName, s);
        final URI uri = new URI(address + "/" + s);
        final HttpClient client = HttpClient
                .newBuilder()
                .proxy(ProxySelector.of(new InetSocketAddress("localhost", 8080)))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        final HttpRequest request = HttpRequest
                .newBuilder(uri)
                .GET()
                .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String result = response.body();
        LOGGER.trace("{} end [result={}]", methodName, result);
        return result;
    }

    public static void main(final String[] args) throws Exception {

        new AnonsvnCrawler().run(args);

    }

}
