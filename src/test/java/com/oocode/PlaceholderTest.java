package com.oocode;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import static org.mockito.ArgumentMatchers.any;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlaceholderTest {

    private static LocalDate today;
    private static LocalDate twoDaysAgo;
    private static LocalDate fourDaysAgo;
    private static LocalDateTime fourDaysAgoDateTime;
    private static LocalDateTime twoDaysAgoDateTime;
    private static String validDataFilePath;
    private static String inValidDataFilePath;
    private static String emptyDataFilePath;
    private static String mapDataFilePath;
    private static String dayOfWeek;
    private static String mockDataFilePath;
    @BeforeClass
    public static void setUpClass() throws IOException {
        today = LocalDate.now();
        twoDaysAgo = today.minusDays(2);
        fourDaysAgo = today.minusDays(4);
        twoDaysAgoDateTime = today.minusDays(2).atStartOfDay();
        fourDaysAgoDateTime = today.minusDays(4).atStartOfDay();
        dayOfWeek = twoDaysAgoDateTime.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        validDataFilePath = "src/test/resources/data_two_days_ago.csv";
        inValidDataFilePath = "src/test/resources/data_older_three_days.csv";
        emptyDataFilePath = "src/test/resources/empty_data.csv";
        mapDataFilePath = "src/test/resources/map_data.csv";
        mockDataFilePath = "src/test/resources/mock_data.csv";

        // valid CSV test data with the correct date two days ago
        List<String[]> validTestData = Arrays.asList(
                new String[]{"Site", "SiteNumber", "Seconds", "DateTime", "Latitude", "Longitude", "Hsig", "Hmax"},
                new String[]{"Caloundra", "12345", String.valueOf(twoDaysAgoDateTime.toEpochSecond(ZoneOffset.ofHours(10))), twoDaysAgo.toString(), "-26.7987", "153.1330", "1.960", "2.500"}
        );

        // invalid CSV test data with the correct date two days ago
        List<String[]> inValidTestData = Arrays.asList(
                new String[]{"Site", "SiteNumber", "Seconds", "DateTime", "Latitude", "Longitude", "Hsig", "Hmax"},
                new String[]{"Noosa", "4567", String.valueOf(fourDaysAgoDateTime.toEpochSecond(ZoneOffset.ofHours(10))), fourDaysAgo.toString(), "-26.34492", "153.11801", "1.270", "1.720"}
        );

        // create valid and map data CSV files
        createCsvFile(validDataFilePath, validTestData);
        createCsvFile(mapDataFilePath, validTestData);
        createCsvFile(inValidDataFilePath, inValidTestData);

        // create empty CSV file
        List<String[]> emptyTestData = new ArrayList<>();
        createCsvFile(emptyDataFilePath, emptyTestData);
    }

    @Test
    public void testCreatePageWithValidData() throws Exception {

        // path of CSV test data (valid data)
        String validDataUrl = "file://" + new File(validDataFilePath).getAbsolutePath();

        // call create page method with valid data and date
        Main.createPage(validDataUrl, today);

        // read index.html
        List<String> lines = Files.readAllLines(Path.of("index.html"));
        String content = String.join("\n", lines);

        // check HTML content contains the expected string
        String expectedString = String.format("You should have been at Caloundra on %s - it was gnarly - waves up to 1.960m!", dayOfWeek);
        assertThat(content, containsString(expectedString));
    }

    @Test
    public void testCreatePageWithInvalidData() throws Exception {

        // path of CSV test data (invalid data)
        String inValidDataUrl = "file://" + new File(inValidDataFilePath).getAbsolutePath();

        // call create page method with valid data and date
        Main.createPage(inValidDataUrl, today);

        // read index.html
        List<String> lines = Files.readAllLines(Path.of("index.html"));
        String content = String.join("\n", lines);

        // check HTML content contains the expected string (invalid data)
        String expectedString =  "Data is not available for the past three days";
        assertThat(content, containsString(expectedString));
    }

    @Test
    public void testCreatePageWithEmptyData() throws Exception {

        // call create page method with invalid date
        String emptyDataUrl = "file://" + new File(emptyDataFilePath).getAbsolutePath();

        // check RuntimeException is thrown
        try {
            Main.createPage(emptyDataUrl, today);
            fail("Expected RuntimeException but no exception was thrown");
        } catch (RuntimeException e) {
            assertTrue(e instanceof RuntimeException);
        }
    }

    @Test
    public void testGoogleMapsLinkInIndexHtml() throws Exception {

        // path of CSV test data (valid data)
        String mapDataUrl = "file://" + new File(mapDataFilePath).getAbsolutePath();

        // call create page method with valid data and date
        Main.createPage(mapDataUrl, today);

        // read index.html
        List<String> lines = Files.readAllLines(Path.of("index.html"));
        String content = String.join("\n", lines);

        // check if HTML contains the expected Google Maps link
        String expectedLink = "href=\"https://www.google.com/maps/search/?api=1&query=-26.7987,153.1330\"";
        assertThat(content, containsString(expectedLink));
    }

    @Test
    public void testCreatePageWithMockData() throws Exception {
        List<String[]> testData = Arrays.asList(
                new String[]{"Site", "SiteNumber", "Seconds", "DateTime", "Latitude", "Longitude","Hsig", "Hmax"},
                new String[]{"Hay Point TriAxys", "4567", String.valueOf(twoDaysAgoDateTime.toEpochSecond(ZoneOffset.ofHours(10))),
                        twoDaysAgo.toString(), "-21.27760", "149.32260", "0.800", "1.040"}
        );
        createCsvFile(mockDataFilePath, testData);

        // create mock HttpClient and  HTTP response
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockHttpResponse = new MockHttpResponse(testData);
        CompletableFuture<HttpResponse<String>> mockFuture = CompletableFuture.completedFuture(mockHttpResponse);
        when(mockHttpClient.sendAsync(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockFuture);

        // call mock Main instance with mock HttpClient
        Main main = new Main(mockHttpClient);

        // path of CSV test data (valid data)
        String mockDataUrl = "file://" + new File(mockDataFilePath).getAbsolutePath();

        // call create page method with valid data and date
        Main.createPage(mockDataUrl, today);

        // check content of the generated index.html file
        Path indexPath = Paths.get("index.html");
        assertTrue("index.html file does not exist", Files.exists(indexPath));

        List<String> lines = Files.readAllLines(indexPath);
        String content = String.join("\n", lines);

        // check if the HTML content contains the expected Google Maps link for each location
        String[] testDataEntry = testData.get(1);
        String latitude = testDataEntry[4];
        String longitude = testDataEntry[5];

        String expectedLink = String.format("href=\"https://www.google.com/maps/search/?api=1&query=%s,%s\"", latitude, longitude);
        assertTrue("Google Maps link not found in index.html", content.contains(expectedLink));
    }

    private static void createCsvFile(String filePath, List<String[]> data) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath))) {
            for (String[] line : data) {
                writer.write(String.join(",", line));
                writer.newLine();
            }
        }
    }
}
