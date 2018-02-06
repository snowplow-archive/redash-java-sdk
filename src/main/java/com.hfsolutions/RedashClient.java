package com.hfsolutions;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hfsolutions.model.datasource.DataSource;
import com.hfsolutions.model.datasource.RedshiftDataSource;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class RedashClient {

    private static final String APIKEY_STRING = "?api_key=";
    private static final String API_STRING = "/api";
    private static final String DATA_SOURCES = "/data_sources";
    private static final String GROUPS = "/groups";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private OkHttpClient client;
    private String baseUrl;
    private String apiKey;
    private Headers headers;

    public RedashClient(String schema, String host, int port, String apiKey) {
        this.client = new OkHttpClient();
        this.apiKey = apiKey;
        this.setBaseUrl(host, port, schema);
        this.setHeaders();
    }

    public int createDataSource(RedshiftDataSource rds) throws IOException, IllegalArgumentException {
        if (isDataSourceAlreadyExists(rds.getName()))
            throw new IllegalArgumentException("Data-source with this name already exists");
        String url = this.baseUrl + DATA_SOURCES + APIKEY_STRING + apiKey;
        String returnValue = post(url, new Gson().toJson(rds));
        return this.getIdFromJson(returnValue);
    }

    public List<DataSource> getDataSources() throws IOException {
        String url = this.baseUrl + DATA_SOURCES + APIKEY_STRING + apiKey;
        Type listType = new TypeToken<ArrayList<DataSource>>(){}.getType();
        return new Gson().fromJson(get(url), listType);
    }

    public boolean deleteDataSource(int id) throws IOException {
        String url = this.baseUrl + DATA_SOURCES + "/" + id + APIKEY_STRING + apiKey;
        String response = delete(url);
        return response.isEmpty();
    }

    private void setHeaders() {
        this.headers = new Headers.Builder()
                .add("Accept", "application/json, text/plain, */*")
                .add("Content-Type", "application/json;charset=UTF-8")
                .build();
    }

    private void setBaseUrl(String host, int port, String schema) {
        this.baseUrl = schema + "://" + host + ":" + port + API_STRING;
    }

    private boolean isDataSourceAlreadyExists(String name) throws IOException {
        return this.getDataSources().stream().anyMatch(e -> name.equals(e.getName()));
    }

    private int getIdFromJson(String json) throws JSONException {
        return new JSONObject(json).getInt("id");
    }

    private String post(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .headers(this.headers)
                .url(url)
                .post(body)
                .build();
        return this.performCall(request);
    }

    private String get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return this.performCall(request);
    }

    private String delete(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();
        return this.performCall(request);
    }

    private String performCall(Request request) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException(response.message());
            return response.body().string();
        }
    }
}
