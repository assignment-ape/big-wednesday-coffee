package com.oocode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.net.ssl.SSLSession;

public class MockHttpResponse implements HttpResponse<String> {
    private final String body;

    public MockHttpResponse(List<String[]> bodyData) {
        StringBuilder csvBuilder = new StringBuilder();
        for (String[] row : bodyData) {
            csvBuilder.append(String.join(",", row)).append("\n");
        }
        this.body = csvBuilder.toString();
    }

    @Override
    public int statusCode() {
        return 200;
    }

    @Override
    public HttpRequest request() {
        return null;
    }

    @Override
    public Optional<HttpResponse<String>> previousResponse() {
        return Optional.empty();
    }

    @Override
    public HttpHeaders headers() {
        return HttpHeaders.of(Collections.emptyMap(), (s1, s2) -> true);
    }

    @Override
    public String body() {
        return body;
    }

    @Override
    public Optional<SSLSession> sslSession() {
        return Optional.empty();
    }

    @Override
    public URI uri() {
        return null;
    }

    @Override
    public HttpClient.Version version() {
        return null;
    }
}
