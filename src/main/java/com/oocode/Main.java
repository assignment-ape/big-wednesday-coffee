package com.oocode;

import com.opencsv.CSVReader;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

public class Main {
  private final HttpClient httpClient;

  public Main(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  public static void main(String[] args) throws Exception {
    String url = args.length == 2 ? args[0] : "https://apps.des.qld.gov.au/data-sets/waves/wave-7dayopdata.csv";
    LocalDate today = args.length == 2 ? LocalDate.parse(args[1]) : LocalDate.now();

    createPage(url, today);
    startServer();
  }

  public static void createPage(String url, LocalDate today) throws Exception {
    List<String[]> result;
    if (url == null || url.isEmpty()) {
      throw new IllegalArgumentException("URL cannot be null or empty");
    } else if (url.startsWith("file:")) {
      // if its local file path
      try (CSVReader reader = new CSVReader(new FileReader(url.substring(7)))) {
        result = reader.readAll().stream().skip(1).collect(toList());
      }
    } else if (url.startsWith("http:") || url.startsWith("https:")) {
      // if its remote URL
      try (Response r = new OkHttpClient().newCall(new Request.Builder().url(url).build()).execute()) {
        if (r.isSuccessful()) {
          try (ResponseBody rb = r.body()) {
            try (CSVReader reader = new CSVReader(new StringReader(rb.string()))) {
              result = reader.readAll().stream().skip(1).collect(toList());
            }
          }
        } else {
          throw new RuntimeException("Failed to fetch CSV data from URL: " + url);
        }
      }
    } else {
      throw new IllegalArgumentException("Unsupported URL scheme: " + url);
    }

    // Find three days ago at the start of the day
    LocalDateTime startOfDay = today.atStartOfDay();
    LocalDateTime threeDaysAgoOfStart = startOfDay.minusDays(3);

    // Check if the parsed dateTime is after threeDaysAgoOfStart and before startOfDay
    List<String[]> filterDate = result.stream()
            .filter(o -> {
              long seconds = Long.parseLong(o[2]);
              LocalDateTime dateTime = LocalDateTime.ofEpochSecond(seconds, 0, ZoneOffset.ofHours(10));
              System.out.println("DateTime: " + dateTime + ", threeDaysAgoOfStart: " + threeDaysAgoOfStart + ", startOfDay: " + startOfDay);
              return dateTime.isAfter(threeDaysAgoOfStart) && dateTime.isBefore(startOfDay);
            })
            .collect(toList());

    if (filterDate.isEmpty()) {
      // if no data is available for the past three days, display a message in the HTML
      String message = "Data is not available for the past three days";
      generateHtmlWithMessage(message);
      return;
    }

    String[] strings = filterDate.stream().max(comparing(o -> Double.valueOf(o[6]))).orElse(null);

    // Define latitude and longitude
    String latitude = strings[4];
    String longitude = strings[5];
    String mapTitle = strings[0];
    LocalDateTime waveDateTime = LocalDateTime.ofEpochSecond(Long.parseLong(strings[2]), 0, ZoneOffset.ofHours(10));

    // Create Google Maps link
    String googleMapsLink = String.format(
            "<a style=\"color: #007bff; text-decoration: none; font-weight: bold;\" class=\"map-link\" target=\"_blank\" " +
                    "href=\"https://www.google.com/maps/search/?api=1&query=%s,%s\">Open Map of %s</a>",
            latitude, longitude, mapTitle);

    // create HTML content
    String htmlContent = String.format("<html><body>You should have been at %s on %s - it was gnarly - waves up to %sm! %s</body></html>",
            mapTitle, waveDateTime.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH), strings[6], googleMapsLink);

    // Write HTML content to file
    try (FileWriter writer = new FileWriter("index.html")) {
      writer.write(htmlContent);
    }
  }

  public static void startServer() throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
    server.createContext("/", new HttpHandler() {
      @Override
      public void handle(HttpExchange exchange) throws IOException {
        byte[] response = Files.readAllBytes(Paths.get("index.html"));
        exchange.sendResponseHeaders(200, response.length);
        OutputStream os = exchange.getResponseBody();
        os.write(response);
        os.close();
      }
    });
    server.setExecutor(null);
    server.start();
    System.out.println("Server is listening on port 8080");
  }

  private static void generateHtmlWithMessage(String message) throws IOException {
    String htmlContent = String.format("<html><body>%s</body></html>", message);
    try (FileWriter writer = new FileWriter("index.html")) {
      writer.write(htmlContent);
    }
  }
}
