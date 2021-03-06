package com.azuriom.azauth;

import com.azuriom.azauth.gson.ColorSerializer;
import com.azuriom.azauth.gson.InstantSerializer;
import com.azuriom.azauth.model.PlayerProfile;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;

public class AzAuthenticator {

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(Color.class, new ColorSerializer())
            .registerTypeAdapter(Instant.class, new InstantSerializer())
            .create();

    private final String url;

    /**
     * Construct a new AzAuthenticator instance.
     *
     * @param url The website url
     */
    public AzAuthenticator(String url) {
        this.url = Objects.requireNonNull(url, "url");

        if (url.startsWith("http://")) {
            System.err.println("[AzLink] The url use HTTP, this is not secure, please consider to upgrade to HTTPS !");
        }
    }

    /**
     * Gets the website url.
     *
     * @return the website url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Try to authenticate the player on the website and get his profile.
     *
     * @param email    the player email
     * @param password the player password
     * @return the player profile
     * @throws AuthenticationException if credentials are not valid
     * @throws IOException             if an IO exception occurs
     */
    public PlayerProfile authenticate(String email, String password) throws AuthenticationException, IOException {
        return this.authenticate(email, password, PlayerProfile.class);
    }

    /**
     * Try to authenticate the player on the website and get his profile with a given response type.
     *
     * @param email        the player email
     * @param password     the player password
     * @param responseType the type of the response
     * @return the player profile
     * @throws AuthenticationException if credentials are not valid
     * @throws IOException             if an IO exception occurs
     */
    public <T> T authenticate(String email, String password, Class<T> responseType) throws AuthenticationException, IOException {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);

        return this.post("authenticate", body, responseType);
    }

    /**
     * Verify an access token and get the associated profile.
     *
     * @param accessToken the player access token
     * @return the player profile
     * @throws AuthenticationException if credentials are not valid
     * @throws IOException             if an IO exception occurs
     */
    public PlayerProfile verify(String accessToken) throws AuthenticationException, IOException {
        return this.verify(accessToken, PlayerProfile.class);
    }

    /**
     * Verify an access token and get the associated profile with a given response type .
     *
     * @param accessToken  the player access token
     * @param responseType the type of the response
     * @return the player profile
     * @throws AuthenticationException if credentials are not valid
     * @throws IOException             if an IO exception occurs
     */
    public <T> T verify(String accessToken, Class<T> responseType) throws AuthenticationException, IOException {
        JsonObject body = new JsonObject();
        body.addProperty("access_token", accessToken);

        return this.post("verify", body, responseType);
    }

    /**
     * Invalidate the given access token.
     * To get a new valid access token you need to {@link #authenticate(String, String)} again.
     *
     * @param accessToken the player access token
     * @throws AuthenticationException if credentials are not valid
     * @throws IOException             if an IO exception occurs
     */
    public void logout(String accessToken) throws AuthenticationException, IOException {
        JsonObject body = new JsonObject();
        body.addProperty("access_token", accessToken);

        this.post("logout", body, null);
    }

    private <T> T post(String endPoint, JsonObject body, Class<T> responseType) throws AuthenticationException, IOException {
        URL url = new URL(this.url + "/api/auth/" + endPoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.addRequestProperty("User-Agent", "AzAuth authenticator v1");
        connection.addRequestProperty("Content-Type", "application/json; charset=utf-8");

        try (OutputStream out = connection.getOutputStream()) {
            out.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        if (connection.getResponseCode() == 422) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                AuthResponse status = GSON.fromJson(reader, AuthResponse.class);

                throw new AuthenticationException(status.getMessage());
            }
        }

        if (responseType == null) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            return GSON.fromJson(reader, responseType);
        }
    }
}
