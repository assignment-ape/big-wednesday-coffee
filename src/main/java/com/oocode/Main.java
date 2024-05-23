package com.oocode;

import com.opencsv.CSVReader;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.FileWriter;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import static java.lang.Integer.parseInt;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

public class Main {
  public static void main(String[] args) throws Exception {
    if (args.length == 2) {
      createPage(args[0], LocalDate.parse(args[1]));
    } else {
      createPage("https://apps.des.qld.gov.au/data-sets/waves/wave-7dayopdata.csv", LocalDate.now());
    }
  }

  public static void createPage(String url, LocalDate today) throws Exception {
    try (Response r = new OkHttpClient().newCall(new Request.Builder().url(url).build()).execute()) {
      if (r.isSuccessful()) {
        try (ResponseBody rb = r.body()) {
          List<String[]> result;
          try (CSVReader reader = new CSVReader(new StringReader(rb.string()))) {
            result = reader.readAll().stream().skip(2).collect(toList());
          }
          String[] strings = result.stream().max(comparing(o -> Double.valueOf(o[7]))).orElse(null);
          try (FileWriter myWriter = new FileWriter("index.html")) {
            LocalDateTime foo = LocalDateTime.ofEpochSecond(parseInt(strings[2]), 0, ZoneOffset.ofHours(10));
            myWriter.write(
                String.format("<html><body>You should have been at %s on %s - it was gnarly - waves up to %sm!</body></html>",
                    strings[0], foo.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH), strings[7]));
          }
        }
      }
    }
  }
}
