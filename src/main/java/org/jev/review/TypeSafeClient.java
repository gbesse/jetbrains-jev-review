package org.jev.review;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

final class TypeSafeClient {
    private TypeSafeClient() {}
    static JsonObject decide(JsonObject request, String apiKey) throws Exception {
        var httpRequest = HttpRequest.newBuilder(URI.create("https://api.typesafe.ai/v1/systemone"))
            .timeout(Duration.ofSeconds(15)).header("Authorization", "Bearer " + apiKey).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(request.toString())).build();
        var response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build().send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) throw new IllegalStateException("TypeSafe request failed with HTTP " + response.statusCode());
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }
}
