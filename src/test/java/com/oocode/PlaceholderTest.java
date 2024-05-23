package com.oocode;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;

public class PlaceholderTest {

    @Test
    public void testCreatePageWithValidData() throws Exception {

        // path of CSV test data (valid data)
        String filePath = "src/test/resources/data_three_days_ago.csv";
        String url = "file://" + new File(filePath).getAbsolutePath();

        // call create page method with valid data and date
        LocalDate testDate = LocalDate.of(2024, 5, 12);
        Main.createPage(url, testDate);

        // read index.html
        List<String> lines = Files.readAllLines(Path.of("index.html"));
        String content = String.join("\n", lines);

        // check HTML content contains the expected string
        String expected_string =  "You should have been at Caloundra on Thursday - it was gnarly - waves up to 1.960m!";
        assertThat(content, containsString(expected_string));
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
}