package com.snowplowanalytics.redash;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.snowplowanalytics.redash.model.BaseEntity;
import com.snowplowanalytics.redash.model.User;
import com.snowplowanalytics.redash.model.UserGroup;
import com.snowplowanalytics.redash.model.datasource.DataSource;
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
    private static final String USERS = "/users";
    private static final String MEMBERS = "/members";
    private static final String AUTH_TYPE = "{\"auth_type\":";
    private static final String CREATED_AT = "{\"created_at\":";

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public static final String DATA_SOURCE_ALREADY_EXISTS = "Data-source with this name already exists.";
    public static final String USER_GROUP_ALREADY_EXISTS = "User group with this name already exists.";
    public static final String USER_DOES_NOT_EXIST = "User with such name does not exist.";
    public static final String DATA_SOURCE_DOES_NOT_EXIST = "Data-source with such name does not exist.";
    public static final String MESSAGE_USER_DOES_NOT_EXIST = "{\"message\": \"The requested URL was not found on the server.  " +
            "If you entered the URL manually please check your spelling and try again.";
    public static final String MESSAGE_USER_GROUP_DOES_NOT_EXIST = "{\"message\": \"Internal Server Error\"}";

    private final OkHttpClient client;
    private final String baseUrl;
    private final String apiKey;
    private final Headers headers;

    public RedashClient(String schema, String host, int port, String apiKey) {
        this.client = new OkHttpClient();
        this.apiKey = apiKey;
        this.baseUrl = schema + "://" + host + ":" + port + API_STRING;
        this.headers = new Headers.Builder()
                .add("Accept", "application/json, text/plain, */*")
                .add("Content-Type", "application/json;charset=UTF-8")
                .build();
    }

    //#1
    public int createDataSource(DataSource rds) throws IOException, IllegalArgumentException {
        if (isEntityAlreadyExists(getDataSources(), rds.getName())) {
            throw new IllegalArgumentException(DATA_SOURCE_ALREADY_EXISTS);
        }
        String url = baseUrl + DATA_SOURCES + APIKEY_STRING + apiKey;
        String response = post(url, new Gson().toJson(rds));
        int id = getIdFromJson(response);
        rds.setId(id);
        return id;
    }

    //#2
    public boolean updateDataSource(DataSource whichToUpdate) throws IOException {
        DataSource fromDataBase;
        try {
            fromDataBase = getDataSource(whichToUpdate.getName());
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage());
        }
        if (isDataSourceAlreadyUpToDate(fromDataBase, whichToUpdate)) {
            return false;
        }
        String url = baseUrl + DATA_SOURCES + "/" + fromDataBase.getId() + APIKEY_STRING + apiKey;
        post(url, new Gson().toJson(whichToUpdate));
        return true;
    }

    //#3
    public List<DataSource> getDataSources() throws IOException {
        String url = baseUrl + DATA_SOURCES + APIKEY_STRING + apiKey;
        Type listType = new TypeToken<ArrayList<DataSource>>() {
        }.getType();
        return new Gson().fromJson(get(url), listType);
    }

    //#4
    public boolean deleteDataSource(int dataSourceId) throws IOException {
        String url = baseUrl + DATA_SOURCES + "/" + dataSourceId + APIKEY_STRING + apiKey;
        return delete(url).isEmpty();
    }

    //#5
    public int createUserGroup(UserGroup userGroup) throws IOException {
        if (isEntityAlreadyExists(getUserGroups(), userGroup.getName())) {
            throw new IllegalArgumentException(USER_GROUP_ALREADY_EXISTS);
        }
        String url = baseUrl + GROUPS + APIKEY_STRING + apiKey;
        String returnValue = post(url, new Gson().toJson(userGroup));
        int id = getIdFromJson(returnValue);
        userGroup.setId(id);
        return id;
    }

    //#6
    public boolean addUserToGroup(int userId, int groupId) throws IOException {
        User user = getUserById(userId);
        getUserGroupBuId(groupId);
        if (user.getGroups().contains(groupId)) {
            return false;
        }
        String url = baseUrl + GROUPS + "/" + groupId + MEMBERS + APIKEY_STRING + apiKey;
        String json = "{\"user_id\":" + userId + "}";
        post(url, json);
        return true;
    }

    public UserGroup getUserGroupBuId(int userGroupId) throws IOException {
        String url = baseUrl + GROUPS + "/" + userGroupId + APIKEY_STRING + apiKey;
        String returnValue = get(url, false);
        return resultResolver(UserGroup.class, returnValue);
    }

    public User getUserById(int userId) throws IOException {
        String url = baseUrl + USERS + "/" + userId + APIKEY_STRING + apiKey;
        String returnValue = get(url, false);
        return resultResolver(User.class, returnValue);
    }

    private <T extends BaseEntity> T resultResolver(Class<T> type, String returnValue) throws IOException {
        if (returnValue.startsWith(AUTH_TYPE) || returnValue.startsWith(CREATED_AT)) {
            return new Gson().fromJson(returnValue, type);
        }
        if (returnValue.startsWith(MESSAGE_USER_DOES_NOT_EXIST) || returnValue.equals(MESSAGE_USER_GROUP_DOES_NOT_EXIST)) {
            throw new IllegalArgumentException(returnValue);
        }
        throw new IOException(returnValue);
    }

    //#10
    public List<UserGroup> getUserGroups() throws IOException {
        String url = baseUrl + GROUPS + APIKEY_STRING + apiKey;
        Type listType = new TypeToken<ArrayList<UserGroup>>() {
        }.getType();
        return new Gson().fromJson(get(url), listType);
    }

    //#11
    public boolean deleteUserGroup(int userGroupId) throws IOException {
        String url = baseUrl + GROUPS + "/" + userGroupId + APIKEY_STRING + apiKey;
        String response = delete(url);
        if (!MESSAGE_USER_GROUP_DOES_NOT_EXIST.equals(response) && !"null".equals(response)) {
            throw new IOException(response);
        }
        return "null".equals(response);
    }

    //#12
    public List<User> getUsers() throws IOException {
        String url = baseUrl + USERS + APIKEY_STRING + apiKey;
        Type listType = new TypeToken<ArrayList<User>>() {
        }.getType();
        return new Gson().fromJson(get(url), listType);
    }

    //#13
    public User getUser(String userName) throws IOException {
        Optional<User> result = getUsers().stream().filter(e -> userName.equals(e.getName())).findFirst();
        if (!result.isPresent()) {
            throw new IllegalArgumentException(USER_DOES_NOT_EXIST);
        }
        return result.get();
    }

    //#14
    public DataSource getDataSource(String dataSourceName) throws IOException {
        Optional<DataSource> result = getDataSources().stream().filter(e -> dataSourceName.equals(e.getName())).findFirst();
        if (!result.isPresent()) {
            throw new IllegalArgumentException(DATA_SOURCE_DOES_NOT_EXIST);
        }
        return getDataSourceById(result.get().getId());
    }

    private boolean isDataSourceAlreadyUpToDate(DataSource fromDataBase, DataSource whichToUpdate) {
        return fromDataBase.getHost().equals(whichToUpdate.getHost())
                && fromDataBase.getPort() == whichToUpdate.getPort()
                && fromDataBase.getUser().equals(whichToUpdate.getUser())
                && fromDataBase.getDbName().equals(whichToUpdate.getDbName());
    }

    public DataSource getDataSourceById(int id) throws IOException {
        String url = baseUrl + DATA_SOURCES + "/" + id + APIKEY_STRING + apiKey;
        return new Gson().fromJson(get(url), DataSource.class);
    }

    private boolean isEntityAlreadyExists(List<? extends BaseEntity> list, String name) throws IOException {
        return list.stream().anyMatch(e -> name.equals(e.getName()));
    }

    private int getIdFromJson(String json) throws JSONException {
        return new JSONObject(json).getInt("id");
    }

    private String post(String url, String json) throws IOException {
        return post(url, json, false);
    }

    private String get(String url) throws IOException {
        return get(url, true);
    }

    private String delete(String url) throws IOException {
        return delete(url, false);
    }

    private String post(String url, String json, boolean checkResponseStatus) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .headers(this.headers)
                .url(url)
                .post(body)
                .build();
        return this.performCall(request, checkResponseStatus);
    }

    private String get(String url, boolean checkResponseStatus) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .headers(this.headers)
                .build();
        return this.performCall(request, checkResponseStatus);
    }

    private String delete(String url, boolean checkResponseStatus) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .headers(this.headers)
                .build();
        return this.performCall(request, checkResponseStatus);
    }

    private String performCall(Request request, boolean checkResponseStatus) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            if (checkResponseStatus && !response.isSuccessful()) {
                throw new IOException(response.message());
            }
            return response.body().string();
        }
    }

}
