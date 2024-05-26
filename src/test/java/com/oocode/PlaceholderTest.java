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
    private static LocalDateTime twoDaysAgoDateTime;
    private static String validDataFilePath;
    private static String emptyDataFilePath;
    private static String mapDataFilePath;
    private static String dayOfWeek;

    @BeforeClass
    public static void setUpClass() throws IOException {
        today = LocalDate.now();
        twoDaysAgo = today.minusDays(2);
        twoDaysAgoDateTime = twoDaysAgo.atStartOfDay();
        dayOfWeek = twoDaysAgoDateTime.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        validDataFilePath = "src/test/resources/data_three_days_ago.csv";
        emptyDataFilePath = "src/test/resources/empty_data.csv";
        mapDataFilePath = "src/test/resources/map_data.csv";

        // valid CSV test data with the correct date two days ago
        List<String[]> validTestData = Arrays.asList(
                new String[]{"Site", "SiteNumber", "Seconds", "DateTime", "Latitude", "Longitude", "Hsig", "Hmax"},
                new String[]{"Caloundra", "12345", String.valueOf(twoDaysAgoDateTime.toEpochSecond(ZoneOffset.ofHours(10))), twoDaysAgo.toString(), "-26.7987", "153.1330", "1.960", "2.500"}
        );

        // create valid and map data CSV files
        createCsvFile(validDataFilePath, validTestData);
        createCsvFile(mapDataFilePath, validTestData);

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

        // path of CSV test data (valid data)
        String filePath = "src/test/resources/data_three_days_ago.csv";
        String url = "file://" + new File(filePath).getAbsolutePath();

        // call create page method with invalid date (older than three days)
        LocalDate testDate = LocalDate.of(2024, 5, 9);

        try {
            Main.createPage(url, testDate);
            fail("Expected RuntimeException but no exception was thrown");
        } catch (RuntimeException e) {
            assertEquals("Data is not available for the past three days", e.getMessage());
        }
    }

    @Test
    public void testCreatePageWithEmptyData() throws Exception {
        // Path of CSV test data (invalid data)
        String filePath = "src/test/resources/empty_data.csv";
        String url = "file://" + new File(filePath).getAbsolutePath();

        // call create page method with invalid date (future date)
        LocalDate testDate = LocalDate.of(2024, 5, 12);

        // check RuntimeException is thrown
        try {
            Main.createPage(url, testDate);
            fail("Expected RuntimeException but no exception was thrown");
        } catch (RuntimeException e) {
            assertTrue(e instanceof RuntimeException);
        }
    }

    @Test
    public void testGoogleMapsLinkInIndexHtml() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate twoDaysAgo = today.minusDays(2);
        LocalDateTime threeDaysAgoDateTime = twoDaysAgo.atStartOfDay();

        // Generate CSV test data with the correct date three days ago
        List<String[]> testData = Arrays.asList(
                new String[]{"Site", "SiteNumber", "Seconds", "DateTime", "Latitude", "Longitude", "Hsig", "Hmax"},
                new String[]{"Caloundra", "12345", String.valueOf(threeDaysAgoDateTime.toEpochSecond(ZoneOffset.ofHours(10))), twoDaysAgo.toString(), "-26.7987", "153.1330", "1.960", "2.500"}
        );

        // Path of CSV test data
        String filePath = "src/test/resources/map_data.csv";
        createCsvFile(filePath, testData);

        // data with map link
        String url = "file://" + new File(filePath).getAbsolutePath();

        // call createPage method with valid data and date
        Main.createPage(url, today);

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
                new String[]{"Location", "Date", "Time", "Latitude", "Longitude", "Wave Height"},
                new String[]{"Hay Point TriAxys", "2024-05-21", "1716608400", "-21.27760", "149.32260", "0.800"}
        );

        // create mock HttpClient and  HTTP response
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockHttpResponse = new MockHttpResponse(testData);
        CompletableFuture<HttpResponse<String>> mockFuture = CompletableFuture.completedFuture(mockHttpResponse);
        when(mockHttpClient.sendAsync(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockFuture);

        // call mock Main instance with mock HttpClient
        Main main = new Main(mockHttpClient);

        // call createPage method with mock data
        String filePath = "src/test/resources/mock_data.csv";
        String url = "file://" + new File(filePath).getAbsolutePath();
        main.createPage(url,  LocalDate.of(2024, 5, 20));
        // check content of the generated index.html file
        Path indexPath = Paths.get("index.html");
        assertTrue("index.html file does not exist", Files.exists(indexPath));

        List<String> lines = Files.readAllLines(indexPath);
        String content = String.join("\n", lines);

        // check if the HTML content contains the expected Google Maps link for each location
        String[] testDataEntry = testData.get(1);
        String latitude = testDataEntry[3];
        String longitude = testDataEntry[4];
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
