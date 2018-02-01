/*
 * Copyright (c) 2018 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

package com.snowplowanalytics.redash;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.snowplowanalytics.redash.model.BaseEntity;
import com.snowplowanalytics.redash.model.Group;
import com.snowplowanalytics.redash.model.User;
import com.snowplowanalytics.redash.model.datasource.DataSource;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class RedashClient {

    private static final String API_KEY_URL_PARAM = "?api_key=";
    private static final String API_PREFIX = "/api";
    private static final String DATA_SOURCES_URL_PREFIX = "/data_sources";
    private static final String GROUPS_URL_PREFIX = "/groups";
    private static final String USERS_URL_PREFIX = "/users";
    private static final String MEMBERS_URL_PREFIX = "/members";

    private static final String ID = "id";
    private static final String USER_ID = "user_id";
    private static final String DATA_SOURCE_ID = "data_source_id";
    private static final String AUTH_TYPE = "auth_type";
    private static final String CREATED_AT = "created_at";
    private static final String MESSAGE = "message";
    private static final String NULL = "null";
    private static final String MESSAGE_URL_NOT_FOUND = "The requested URL was not found on the server.  " +
            "If you entered the URL manually please check your spelling and try again.";
    private static final String MESSAGE_INTERNAL_SERVER_ERROR = "Internal Server Error";
    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";

    private static final MediaType JSON = MediaType.parse(JSON_CONTENT_TYPE);

    public static final String DATA_SOURCE_ALREADY_EXISTS = "Data-source with this name already exists.";
    public static final String USER_GROUP_ALREADY_EXISTS = "User group with this name already exists.";
    public static final String USER_DOES_NOT_EXIST = "User with such name does not exist.";
    public static final String DATA_SOURCE_DOES_NOT_EXIST = "Data-source with such name does not exist.";

    private final OkHttpClient client;
    private final String baseUrl;
    private final String apiKey;
    private final Headers headers;

    public RedashClient(String schema, String host, int port, String apiKey) {
        this.client = new OkHttpClient();
        this.apiKey = apiKey;
        this.baseUrl = schema + "://" + host + ":" + port + API_PREFIX;
        this.headers = new Headers.Builder()
                .add("Accept", "application/json, text/plain, */*")
                .add("Content-Type", JSON_CONTENT_TYPE)
                .build();
    }

    /**
     * Creates a new data-source.
     *
     * @param dataSource A data-source object which should contain all necessary information.
     * @return int Id of the successfully created data-source. In this case object that was transferred as argument
     * receives that id.
     * @throws IOException If the server is unavailable due to a connection error or if API key is invalid or 
     *         if {@code url} is not a valid HTTP or HTTPS URL.
     * @throws IllegalArgumentException If a data-source with the same name already exists.
     */
    public int createDataSource(DataSource dataSource) throws IOException, IllegalArgumentException {
        if (isEntityAlreadyExists(getDataSources(), dataSource.getName())) {
            throw new IllegalArgumentException(DATA_SOURCE_ALREADY_EXISTS);
        }
        String url = baseUrl + DATA_SOURCES_URL_PREFIX + API_KEY_URL_PARAM + apiKey;
        String response = post(url, new Gson().toJson(dataSource), CheckResponseStatus.YES);
        int id = getIdFromJson(response);
        dataSource.setId(id);
        return id;
    }

    /**
     * Updates an already existing data-source. Please note that all the argument's fields must be present and have correct values.  
     * This function uses the data-source name to discover what needs to be updated.
     *
     * @param dataSource A data-source object which should contain all necessary information.
     * @return boolean False if any of the argument's fields have null or empty value and True if entity successfully updated.
     * @throws IOException If a data-source with this name could not be found or if the server is unavailable due to a connection error 
     *         or if API key is invalid or if {@code url} is not a valid HTTP or HTTPS URL.
     */
    public boolean updateDataSource(DataSource dataSource) throws IOException {
        DataSource fromDataBase;
        try {
            fromDataBase = getDataSource(dataSource.getName());
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage());
        }
        if (dataSourceIsInValid(dataSource)) {
            return false;
        }
        String url = baseUrl + DATA_SOURCES_URL_PREFIX + "/" + fromDataBase.getId() + API_KEY_URL_PARAM + apiKey;
        post(url, new Gson().toJson(dataSource), CheckResponseStatus.YES);
        return true;
    }

    /**
     * @return The list of all existing data-sources on the server. If there are no data-sources then the list will be empty.
     * @throws IOException If the server is unavailable due to a connection error or if API key is invalid or 
     *         if {@code url} is not a valid HTTP or HTTPS URL.
     */
    public List<DataSource> getDataSources() throws IOException {
        String url = baseUrl + DATA_SOURCES_URL_PREFIX + API_KEY_URL_PARAM + apiKey;
        Type listType = new TypeToken<ArrayList<DataSource>>() {}.getType();
        return new Gson().fromJson(get(url, CheckResponseStatus.YES), listType);
    }

    /**
     * Removes an existing data-source.
     *
     * @param dataSourceId Id of the data-source to delete.
     * @return boolean False if a data-source with the provided id doesn't exist, if it does then the data-source will be
     * deleted and True will be returned.
     * @throws IOException If the server is unavailable due to a connection error or if API key is invalid or 
     *         if {@code url} is not a valid HTTP or HTTPS URL.
     */
    public boolean deleteDataSource(int dataSourceId) throws IOException {
        String url = baseUrl + DATA_SOURCES_URL_PREFIX + "/" + dataSourceId + API_KEY_URL_PARAM + apiKey;
        try {
            delete(url, CheckResponseStatus.YES);
        } catch (IOException e) {
            if ((MESSAGE_INTERNAL_SERVER_ERROR.toLowerCase()).equals(e.getMessage().toLowerCase())) {
                return false;
            }
            throw new IOException(e.getMessage());
        }
        return true;
    }

    /**
     * Creates a new user-group.
     *
     * @param group A group object which should contain all necessary information.
     * @return int Id of the successfully created group. In this case object that was transferred as argument
     * receives that id.
     * @throws IllegalArgumentException If a user-group with the provided name already exists.
     * @throws IOException If the server is unavailable due to a connection error or if API key is invalid or 
     *         if {@code url} is not a valid HTTP or HTTPS URL.
     */
    public int createUserGroup(Group group) throws IOException {
        if (isEntityAlreadyExists(getUserGroups(), group.getName())) {
            throw new IllegalArgumentException(USER_GROUP_ALREADY_EXISTS);
        }
        String url = baseUrl + GROUPS_URL_PREFIX + API_KEY_URL_PARAM + apiKey;
        String returnValue = post(url, new Gson().toJson(group), CheckResponseStatus.YES);
        int id = getIdFromJson(returnValue);
        group.setId(id);
        return id;
    }

    /**
     * Adds a user to a user-group; both specified by their ids.
     *
     * @param userId The id of the the user to add to the group.
     * @param groupId The id of the group to add the user to.
     * @return boolean False if specified user-group already contains specified user, True if not and user was added.
     * @throws IllegalArgumentException If either the user or user-group does not exist.
     * @throws IOException If the server is unavailable due to a connection error or if API key is invalid or 
     *         if {@code url} is not a valid HTTP or HTTPS URL.
     */
    public boolean addUserToGroup(int userId, int groupId) throws IOException {
        checkIfEntityExists(User.class, userId);
        Group group = getWithUsersAndDataSources(groupId);
        if (group.getUsers().stream().anyMatch(u -> u.getId() == userId)) {
            return false;
        }
        String url = baseUrl + GROUPS_URL_PREFIX + "/" + groupId + MEMBERS_URL_PREFIX + API_KEY_URL_PARAM + apiKey;
        post(url, new JSONObject().put(USER_ID, userId).toString(), CheckResponseStatus.YES);
        return true;
    }

    /**
     * Adds a data-source to a user-group; both specified by their ids.
     *
     * @param dataSourceId The id of the data-source to add to the group.
     * @param groupId The id of the group to add the data-source to.
     * @return boolean False if the data-source is already attached to the group, True if not and data-source was added.
     * @throws IllegalArgumentException If either the data-source or user-group does not exist.
     * @throws IOException If the server is unavailable due to a connection error or if API key is invalid or 
     *         if {@code url} is not a valid HTTP or HTTPS URL.
     */
    public boolean addDataSourceToGroup(int dataSourceId, int groupId) throws IOException {
        checkIfEntityExists(DataSource.class, dataSourceId);
        Group group = getWithUsersAndDataSources(groupId);
        if (group.getDataSources().stream().anyMatch(ds -> ds.getId() == dataSourceId)) {
            return false;
        }
        String url = baseUrl + GROUPS_URL_PREFIX + "/" + groupId + DATA_SOURCES_URL_PREFIX + API_KEY_URL_PARAM + apiKey;
        post(url, new JSONObject().put(DATA_SOURCE_ID, dataSourceId).toString(), CheckResponseStatus.YES);
        return true;
    }

    /**
     * Removes a user from a user-group; both specified by their ids.
     *
     * @param userId The id of the user to be removed.
     * @param groupId The id of the group to remove the user from.
     * @return boolean False if user is not a member of the group, True if it is and was removed.
     * @throws IllegalArgumentException If either the user or user-group does not exist.
     * @throws IOException If the server is unavailable due to a connection error or if API key is invalid or 
     *         if {@code url} is not a valid HTTP or HTTPS URL.
     */
    public boolean removeUserFromGroup(int userId, int groupId) throws IOException {
        checkIfEntityExists(User.class, userId);
        Group group = getWithUsersAndDataSources(groupId);
        if (group.getUsers().stream().noneMatch(u -> u.getId() == userId)) {
            return false;
        }
        String url = baseUrl + GROUPS_URL_PREFIX + "/" + groupId + MEMBERS_URL_PREFIX + "/" + userId + API_KEY_URL_PARAM + apiKey;
        delete(url, CheckResponseStatus.YES);
        return true;
    }

    /**
     * Removes a data-source from a user-group; both specified by their ids.
     *
     * @param dataSourceId The id of the data-source to be removed.
     * @param groupId The id of the group to remove the data-source from.
     * @return boolean False if data-source is not attached to the group, True if it is and was removed.
     * @throws IllegalArgumentException If either the data-source or user-group does not exist.
     * @throws IOException If the server is unavailable due to a connection error or if API key is invalid or 
     *         if {@code url} is not a valid HTTP or HTTPS URL.
     */
    public boolean removeDataSourceFromGroup(int dataSourceId, int groupId) throws IOException {
        checkIfEntityExists(DataSource.class, dataSourceId);
        Group group = getWithUsersAndDataSources(groupId);
        if (group.getDataSources().stream().noneMatch(ds -> ds.getId() == dataSourceId)) {
            return false;
        }
        String url = baseUrl + GROUPS_URL_PREFIX + "/" + groupId + DATA_SOURCES_URL_PREFIX + "/" + dataSourceId + API_KEY_URL_PARAM + apiKey;
        delete(url, CheckResponseStatus.YES);
        return true;
    }

    /**
     * Retrieves all available user-groups.
     *
     * @return List of groups which will contain at least two user groups with the names "admin" and "default".
     * @throws IOException If the server is unavailable due to a connection error or if API key is invalid or 
     *         if {@code url} is not a valid HTTP or HTTPS URL.
     */
    public List<Group> getUserGroups() throws IOException {
        String url = baseUrl + GROUPS_URL_PREFIX + API_KEY_URL_PARAM + apiKey;
        Type listType = new TypeToken<ArrayList<Group>>() {}.getType();
        return new Gson().fromJson(get(url, CheckResponseStatus.YES), listType);
    }

    /**
     * Removes a user-group with the specified id.
     *
     * @param userGroupId The id of the group to delete.
     * @return boolean True if user group was found and deleted, False if the user-group did not exist.
     * @throws IOException If the server is unavailable due to a connection error or if API key is invalid or 
     *         if {@code url} is not a valid HTTP or HTTPS URL.
     */
    public boolean deleteUserGroup(int userGroupId) throws IOException {
        String url = baseUrl + GROUPS_URL_PREFIX + "/" + userGroupId + API_KEY_URL_PARAM + apiKey;
        String response = delete(url, CheckResponseStatus.NO);
        if (!NULL.equals(response) && !MESSAGE_INTERNAL_SERVER_ERROR.equals(new JSONObject(response).getString(MESSAGE))) {
            throw new IOException(response);
        }
        return NULL.equals(response);
    }

    /**
     * Retrieves all available users.
     *
     * @return List of users.
     * @throws IOException If the server is unavailable due to a connection error or if API key is invalid or 
     *         if {@code url} is not a valid HTTP or HTTPS URL.
     */
    public List<User> getUsers() throws IOException {
        String url = baseUrl + USERS_URL_PREFIX + API_KEY_URL_PARAM + apiKey;
        Type listType = new TypeToken<ArrayList<User>>() {}.getType();
        return new Gson().fromJson(get(url, CheckResponseStatus.YES), listType);
    }

    /**
     * Attempts to retrieve a single user specified by their username.
     *
     * @param userName The name of the user to return.
     * @return The user object that matches the name provided.
     * @throws IllegalArgumentException If user with provided name does not exist.
     * @throws IOException If the server is unavailable due to a connection error or if API key is invalid or 
     *         if {@code url} is not a valid HTTP or HTTPS URL.
     */
    public User getUser(String userName) throws IOException {
        Optional<User> result = getUsers().stream().filter(e -> userName.equals(e.getName())).findFirst();
        if (!result.isPresent()) {
            throw new IllegalArgumentException(USER_DOES_NOT_EXIST);
        }
        return result.get();
    }

    /**
     * Attempts to retrieve a single data-source specified by name.
     *
     * @param dataSourceName The name of the data-source to return.
     * @return The data-source object that matches the name provided.
     * @throws IllegalArgumentException If data-source with provided name does not exist.
     * @throws IOException If the server is unavailable due to a connection error or if API key is invalid or 
     *         if {@code url} is not a valid HTTP or HTTPS URL.
     */
    public DataSource getDataSource(String dataSourceName) throws IOException {
        Optional<DataSource> result = getDataSources().stream().filter(e -> dataSourceName.equals(e.getName())).findFirst();
        if (!result.isPresent()) {
            throw new IllegalArgumentException(DATA_SOURCE_DOES_NOT_EXIST);
        }
        return getDataSourceById(result.get().getId());
    }

    /**
     * Attempts to retrieve a single user-group specified by id.
     * 
     * @param userGroupId The id of the user-group to return.
     * @return The Group object that matches the id provided.
     * @throws IllegalArgumentException If user-group with provided id does not exist.
     * @throws IOException If the server is unavailable due to a connection error or if API key is invalid or 
     *         if {@code url} is not a valid HTTP or HTTPS URL.
     */
    public Group getGroupById(int userGroupId) throws IOException {
        String url = baseUrl + GROUPS_URL_PREFIX + "/" + userGroupId + API_KEY_URL_PARAM + apiKey;
        String returnValue = get(url, CheckResponseStatus.NO);
        return resultResolver(Group.class, returnValue);
    }

    /**
     * Attempts to retrieve a single user specifed by id.
     * 
     * @param userId The id of the user to return.
     * @return The User object that matches the id provided.
     * @throws IllegalArgumentException If user with provided id does not exist.
     * @throws IOException If the server is unavailable due to a connection error or if API key is invalid or 
     *         if {@code url} is not a valid HTTP or HTTPS URL.
     */
    public User getUserById(int userId) throws IOException {
        String url = baseUrl + USERS_URL_PREFIX + "/" + userId + API_KEY_URL_PARAM + apiKey;
        String returnValue = get(url, CheckResponseStatus.NO);
        return resultResolver(User.class, returnValue);
    }

    /**
     * Attempts to retrieve a single data-source specified by id.
     * 
     * @param id The id of the data-source to return.
     * @return The DataSource object that matches the id provided.
     * @throws IllegalArgumentException If data-source with provided id does not exist.
     * @throws IOException If the server is unavailable due to a connection error or if API key is invalid or 
     *         if {@code url} is not a valid HTTP or HTTPS URL.
     */
    public DataSource getDataSourceById(int id) throws IOException {
        String url = baseUrl + DATA_SOURCES_URL_PREFIX + "/" + id + API_KEY_URL_PARAM + apiKey;
        String returnValue = get(url, CheckResponseStatus.NO);
        JSONObject jsonObject = new JSONObject(returnValue);
        if (jsonObject.has(MESSAGE) && MESSAGE_INTERNAL_SERVER_ERROR.equals(jsonObject.getString(MESSAGE))) {
            throw new IllegalArgumentException(MESSAGE_INTERNAL_SERVER_ERROR);
        }
        return new Gson().fromJson(returnValue, DataSource.class);
    }

    /**
     * Attempts to return a single user-group with all attached users and data-sources attached to the same object.
     *
     * @param userGroupId The id of the user-group to return.
     * @return A Group object with all users and data-sources found and attached.
     * @throws IllegalArgumentException If user-group with provided id does not exist.
     * @throws IOException If the server is unavailable due to a connection error or if API key is invalid or 
     *         if {@code url} is not a valid HTTP or HTTPS URL.
     */
    public Group getWithUsersAndDataSources(int userGroupId) throws IOException {
        Group group = getGroupById(userGroupId);
        String dataSourcesUrl = baseUrl + GROUPS_URL_PREFIX + "/" + userGroupId + DATA_SOURCES_URL_PREFIX + API_KEY_URL_PARAM + apiKey,
                usersUrl = baseUrl + GROUPS_URL_PREFIX + "/" + userGroupId + MEMBERS_URL_PREFIX + API_KEY_URL_PARAM + apiKey;
        Type userListType = new TypeToken<ArrayList<User>>() {}.getType();
        Type dataSourceListType = new TypeToken<ArrayList<DataSource>>() {}.getType();
        String usersReturnValue = get(usersUrl, CheckResponseStatus.YES);
        String dataSourcesReturnValue = get(dataSourcesUrl, CheckResponseStatus.YES);
        group.setUsers(new Gson().fromJson(usersReturnValue, userListType));
        group.setDataSources(new Gson().fromJson(dataSourcesReturnValue, dataSourceListType));
        return group;
    }

    private boolean dataSourceIsInValid(DataSource dataSource) {
        return  dataSource.getName() == null || dataSource.getName().isEmpty()
                || dataSource.getHost() == null || dataSource.getHost().isEmpty()
                || dataSource.getPort() == 0
                || dataSource.getUser() == null || dataSource.getUser().isEmpty()
                || dataSource.getPassword() == null || dataSource.getPassword().isEmpty()
                || dataSource.getDbName() == null || dataSource.getDbName().isEmpty();
    }

    private boolean isEntityAlreadyExists(List<? extends BaseEntity> list, String name) {
        return list.stream().anyMatch(e -> name.equals(e.getName()));
    }

    private int getIdFromJson(String json) throws JSONException {
        return new JSONObject(json).getInt(ID);
    }

    private <T extends BaseEntity> T resultResolver(Class<T> type, String returnValue) throws IOException {
        JSONObject jsonObject = new JSONObject(returnValue);
        if (jsonObject.has(AUTH_TYPE) || jsonObject.has(CREATED_AT) || jsonObject.has("name")) {
            return new Gson().fromJson(returnValue, type);
        }
        if (jsonObject.getString(MESSAGE).startsWith(MESSAGE_URL_NOT_FOUND) ||
                jsonObject.getString(MESSAGE).equals(MESSAGE_INTERNAL_SERVER_ERROR)) {
            throw new IllegalArgumentException(returnValue);
        }
        throw new IOException(returnValue);
    }

    private String post(String url, String json, CheckResponseStatus checkResponseStatus) throws IOException {
        validateURL(url);
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .headers(this.headers)
                .build();
        return this.performCall(request, checkResponseStatus);
    }

    private String get(String url, CheckResponseStatus checkResponseStatus) throws IOException {
        validateURL(url);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .headers(this.headers)
                .build();
        return this.performCall(request, checkResponseStatus);
    }

    private String delete(String url, CheckResponseStatus checkResponseStatus) throws IOException {
        validateURL(url);
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .headers(this.headers)
                .build();
        return this.performCall(request, checkResponseStatus);
    }

    private String performCall(Request request, CheckResponseStatus checkResponseStatus) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            if (checkResponseStatus == CheckResponseStatus.YES && !response.isSuccessful()) {
                throw new IOException(anonymisedUrl(response.message()));
            }
            return response.body().string();
        }
    }

    private void validateURL(String url) throws IOException {
        if (HttpUrl.parse(url) == null) throw new IOException("Incorrect URL. Please check it and try again");
    }

    private String anonymisedUrl(String message) {
        return message.contains(apiKey) ? message.replace(apiKey, "XXX") : message;
    }

    private void checkIfEntityExists(Class<? extends BaseEntity> clazz, int id) throws IOException {
        if (User.class.equals(clazz)) {
            getUserById(id);
        } else if (DataSource.class.equals(clazz)) {
            getDataSourceById(id);
        }
    }
}
