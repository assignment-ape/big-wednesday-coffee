package com.oocode;

import com.opencsv.CSVReader;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import java.io.FileReader;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static java.lang.Integer.parseInt;
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
    List<String[]> result;
    if (url.startsWith("file:")) {
      // add URL as file path
      try (CSVReader reader = new CSVReader(new FileReader(url.substring(5)))) {
        result = reader.readAll().stream().skip(2).collect(toList());
      }
    } else {
      // If the URL is a remote URL
      try (Response r = new OkHttpClient().newCall(new Request.Builder().url(url).build()).execute()) {
        if (r.isSuccessful()) {
          try (ResponseBody rb = r.body()) {
            try (CSVReader reader = new CSVReader(new StringReader(rb.string()))) {
              result = reader.readAll().stream().skip(2).collect(toList());
            }
          }
        } else {
          throw new RuntimeException("Failed to fetch CSV data from URL: " + url);
        }
      }
    }

    // find three days ago at the start of the day
    LocalDateTime startOfDay = today.atStartOfDay();
    LocalDateTime threeDaysAgoOfStart = startOfDay.minusDays(3);

    // check if the parsed dateTime is after threeDaysAgoOfStart and before startOfDay
    List<String[]> filterDate = result.stream()
            .filter(o -> {
              LocalDateTime dateTime = LocalDateTime.ofEpochSecond(parseInt(o[2]), 0, ZoneOffset.ofHours(10));
              return dateTime.isAfter(threeDaysAgoOfStart) && dateTime.isBefore(startOfDay);
            })
            .collect(toList());

    if (filterDate.isEmpty()) {
      throw new RuntimeException("Data is not available for the past three days");
    }
  }
}
