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
     * Creates new data-source instance on the Redash server.
     *
     * @param dataSource data transfer object which should contain all necessary information.
     * @return int id of successfully (if so) created instance. In this case object that was transferred as argument
     * receives that id.
     * @throws IOException              if server is unavailable due to connection error or if {@code url} is not a valid HTTP or HTTPS URL.
     * @throws IllegalArgumentException if there is already a data-source with this name on the Redash server.
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
     * Updates data-source on Redash server with provided as argument DataSource instance. Please note that all the argument's
     * fields should be present and have correct values.
     * @param dataSource Argument's name field is used to find specific data-source on the server which (if found) then
     *                   will be updated by it's fields.
     * @return false, if any of the argument's fields have null or empty value.
     * True if entity successfully updated.
     * @throws IOException with the message that the server response in the case if data-source with provided name
     *         couldn't be found, or if {@code url} is not a valid HTTP or HTTPS URL, or there is any connection error.
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
     * @return The list of all existing data-sources on the server. If there are no data-sources then the list
     * will be empty.
     * @throws IOException if server is not available due to connection error or API key is invalid, or
     * if {@code url} is not a valid HTTP or HTTPS URL.
     */
    public List<DataSource> getDataSources() throws IOException {
        String url = baseUrl + DATA_SOURCES_URL_PREFIX + API_KEY_URL_PARAM + apiKey;
        Type listType = new TypeToken<ArrayList<DataSource>>() {}.getType();
        return new Gson().fromJson(get(url, CheckResponseStatus.YES), listType);
    }

    /**
     * Removes data-source
     *
     * @param dataSourceId id of data-source which will be deleted if it exists.
     * @return false if data-source with provided id doesn't exist on the server, if it does then data-source will be
     * deleted and true will be returned.
     * @throws IOException if server is not available due to connection error or API key is invalid, or
     * if {@code url} is not a valid HTTP or HTTPS URL.
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
     * Creates user-group onto Redash server
     *
     * @param group should contain correct value name field, as it will be used in creation.
     * @return id of newly created group, if there wasn't with such name, and in this case argument's id field will
     * be set by value that was generated and returned by server in the response body.
     * @throws IllegalArgumentException if there is already exists user-group with the name provided inside dataSource
     * @throws IOException              if server is not available due to connection error or API key is invalid, or
     * if {@code url} is not a valid HTTP or HTTPS URL.
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
     * Adds user into user-group both specified by their ids.
     *
     * @param userId the user to add to a group
     * @param groupId the group to add the user to
     * @return false if specified user-group already contains specified user, true if not and user was successfully added.
     * @throws IllegalArgumentException if no user or no user group with provided userId and userGroupId accordingly exist
     *                                  onto Redash server.
     * @throws IOException              if server is not available due to connection error or API key is invalid, or
     * if {@code url} is not a valid HTTP or HTTPS URL.
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
     * Adds data-source to user-group which are both specified by their ids in methods arguments.
     *
     * @param dataSourceId the data-source to add to a group
     * @param groupId the group to add the data-source to
     * @return false if data-source already attached to group, true if not and has just added successfully to group
     * @throws IllegalArgumentException if data-source or user-group does not exist
     * @throws IOException              if server is not available due to connection error or API key is invalid, or
     * if {@code url} is not a valid HTTP or HTTPS URL.
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
     * Removes user from user-group which are both specified by their ids in methods arguments.
     *
     * @param userId the user that needs to be removed
     * @param groupId the group to remove the user from
     * @return false if user is not a member of group, true if it is and then it will be successfully removed.
     * @throws IllegalArgumentException if either user or user-group does not exist.
     * @throws IOException              if server is not available due to connection error or API key is invalid, or
     * if {@code url} is not a valid HTTP or HTTPS URL.
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
     * Removes data-source from user-group which are both specified by their ids in methods arguments.
     *
     * @param dataSourceId the data-source that needs to be removed
     * @param groupId the group to remove the data-source from
     * @return false if data-source is not attached to group, true if it is and then it will be successfully removed.
     * @throws IllegalArgumentException if either data-source or user-group does not exist.
     * @throws IOException              if server is not available due to connection error or API key is invalid, or
     * if {@code url} is not a valid HTTP or HTTPS URL.
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
     * Retrieves all the user-groups from Redash server and maps them to Group class.
     *
     * @return List of groups as result which will contain at least two user groups with names "admin" and "default".
     * @throws IOException if server is not available due to connection error or API key is invalid, or
     * if {@code url} is not a valid HTTP or HTTPS URL.
     */
    public List<Group> getUserGroups() throws IOException {
        String url = baseUrl + GROUPS_URL_PREFIX + API_KEY_URL_PARAM + apiKey;
        Type listType = new TypeToken<ArrayList<Group>>() {}.getType();
        return new Gson().fromJson(get(url, CheckResponseStatus.YES), listType);
    }

    /**
     * Removes user-group from redash server by provided id as method's argument.
     *
     * @param userGroupId the group to delete
     * @return true if user group with specified id was found on server and successfully deleted, false if the user-group
     * wasn't found and so couldn't be deleted.
     * @throws IOException if server is not available due to connection error or API key is invalid, or
     * if {@code url} is not a valid HTTP or HTTPS URL.
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
     * Retrieves all the existing users server from redash server, maps them onto User class and
     * returns instances as List.
     *
     * @return List of users that have been found on the server
     * @throws IOException if server is not available due to connection error or API key is invalid, or
     * if {@code url} is not a valid HTTP or HTTPS URL.
     */
    public List<User> getUsers() throws IOException {
        String url = baseUrl + USERS_URL_PREFIX + API_KEY_URL_PARAM + apiKey;
        Type listType = new TypeToken<ArrayList<User>>() {}.getType();
        return new Gson().fromJson(get(url, CheckResponseStatus.YES), listType);
    }

    /**
     * Calls the public getUsers() method, then filters obtained list using Stream API.
     *
     * @param userName the name of user which will be returned as result if successfully found
     * @return the user object that matches the name provided
     * @throws IllegalArgumentException if user with provided name wasn't found between existing on the Redash server.
     * @throws IOException              if server is not available due to connection error or API key is invalid, or
     * if {@code url} is not a valid HTTP or HTTPS URL.
     */
    public User getUser(String userName) throws IOException {
        Optional<User> result = getUsers().stream().filter(e -> userName.equals(e.getName())).findFirst();
        if (!result.isPresent()) {
            throw new IllegalArgumentException(USER_DOES_NOT_EXIST);
        }
        return result.get();
    }

    /**
     * Calls the public getDataSources() method, then filters obtained data-sources using Stream API.
     *
     * @param dataSourceName the name of data-source which will be returned as result if successfully found.
     * @return the data-source that matches the name provided
     * @throws IllegalArgumentException if data-source with provided name wasn't found between existing on the Redash server.
     * @throws IOException              if server is not available due to connection error or API key is invalid, or
     * if {@code url} is not a valid HTTP or HTTPS URL.
     */
    public DataSource getDataSource(String dataSourceName) throws IOException {
        Optional<DataSource> result = getDataSources().stream().filter(e -> dataSourceName.equals(e.getName())).findFirst();
        if (!result.isPresent()) {
            throw new IllegalArgumentException(DATA_SOURCE_DOES_NOT_EXIST);
        }
        return getDataSourceById(result.get().getId());
    }

    /**
     * Provides opportunity to retrieve the user-group from redash server by it's id. Implemented as util method.
     *
     * @param userGroupId the id of the group to fetch
     * @return Group instance.
     * @throws IllegalArgumentException if there is no user-group with such id.
     * @throws IOException              if server is not available due to connection error or API key is invalid, or
     * if {@code url} is not a valid HTTP or HTTPS URL.
     */
    public Group getGroupById(int userGroupId) throws IOException {
        String url = baseUrl + GROUPS_URL_PREFIX + "/" + userGroupId + API_KEY_URL_PARAM + apiKey;
        String returnValue = get(url, CheckResponseStatus.NO);
        return resultResolver(Group.class, returnValue);
    }

    /**
     * Provides opportunity to retrieve the user-group from redash server by it's id. Implemented as util method.
     *
     * @param userId the id of the user to fetch
     * @return User instance.
     * @throws IllegalArgumentException if there is no user with such id.
     * @throws IOException              if server is not available due to connection error or API key is invalid, or
     * if {@code url} is not a valid HTTP or HTTPS URL.
     */
    public User getUserById(int userId) throws IOException {
        String url = baseUrl + USERS_URL_PREFIX + "/" + userId + API_KEY_URL_PARAM + apiKey;
        String returnValue = get(url, CheckResponseStatus.NO);
        return resultResolver(User.class, returnValue);
    }

    /**
     * Provides opportunity to retrieve the user-group from Redash server by it's id. Implemented as util method.
     *
     * @param id the id of the data-source to fetch
     * @return DataSource instance
     * @throws IllegalArgumentException if there is no user-group with such id
     * @throws IOException              if server is not available due to connection error or API key is invalid, or
     * if {@code url} is not a valid HTTP or HTTPS URL.
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
     * Gets specified by id user-group if it existing onto Redash instance. After that making two additional calls
     * for getting lists of users and data-sources which are belong to it and sets by them earlier retrieved
     * user-group entity.
     *
     * @param userGroupId specifies the user-group which should be returned.
     * @return a group with all users and data-sources attached
     * @throws IllegalArgumentException if there is no user-group with such id.
     * @throws IOException              if server is not available due to connection error or API key is invalid, or
     * if {@code url} is not a valid HTTP or HTTPS URL.
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
