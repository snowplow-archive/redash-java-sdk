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
    public static final String MESSAGE_INTERNAL_SERVER_ERROR = "{\"message\": \"Internal Server Error\"}";

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

    /**
     * Creates new data-source instance on the Redash server.
     *
     * @param dataSource data transfer object which should contain all necessary information.
     * @return int id of successfully (if so) created instance. In this case object that was transferred as argument
     * receives that id.
     * @throws IOException              if server is unable due to connection error or API key is invalid.
     * @throws IllegalArgumentException if there is already presents data-source on Redash server
     *                                  with such name
     *                                  Uses Gson library to serialize DataSource instance into String value.
     */
    //#1
    public int createDataSource(DataSource dataSource) throws IOException, IllegalArgumentException {
        if (isEntityAlreadyExists(getDataSources(), dataSource.getName())) {
            throw new IllegalArgumentException(DATA_SOURCE_ALREADY_EXISTS);
        }
        String url = baseUrl + DATA_SOURCES + APIKEY_STRING + apiKey;
        String response = post(url, new Gson().toJson(dataSource));
        int id = getIdFromJson(response);
        dataSource.setId(id);
        return id;
    }

    /**
     * Updates data-source on Redash server with provided as argument DataSource instance. Please note that password field
     * doesn't take a part in making decision of necessity to update data-source as the server does not return it (password field).
     *
     * @param dataSource Argument's name field is used to find specific data-source on the server which (if found) then
     *                   will be updated by it's fields, if it is necessary.
     * @return false, if all the fields except password are equals, and there is nothing to update.
     * True if one or more fields needs to be updated.
     * @throws IOException with the message that the server response in the case if data-source with provided name
     *                     couldn't be found, or wrong API key, or there is any connection error.
     */
    //#2
    public boolean updateDataSource(DataSource dataSource) throws IOException {
        DataSource fromDataBase;
        try {
            fromDataBase = getDataSource(dataSource.getName());
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage());
        }
        if (isDataSourceAlreadyUpToDate(fromDataBase, dataSource)) {
            return false;
        }
        String url = baseUrl + DATA_SOURCES + "/" + fromDataBase.getId() + APIKEY_STRING + apiKey;
        post(url, new Gson().toJson(dataSource));
        return true;
    }

    /**
     * @return ArrayList<DataSource> of all existing data-sources on the server. If there are no data-sources then the list
     * will be empty.
     * @throws IOException if server is not available due to connection error or API key is invalid.
     */
    //#3
    public List<DataSource> getDataSources() throws IOException {
        String url = baseUrl + DATA_SOURCES + APIKEY_STRING + apiKey;
        Type listType = new TypeToken<ArrayList<DataSource>>() {
        }.getType();
        return new Gson().fromJson(get(url), listType);
    }

    /**
     * Removes data-source
     *
     * @param dataSourceId id of data-source which will be deleted if such presents.
     * @return false if data-source with provided id doesn't exist on the server, if it does then data-source will be
     * deleted and true will be returned.
     * @throws IOException if server is not available due to connection error or API key is invalid.
     */
    //#4
    public boolean deleteDataSource(int dataSourceId) throws IOException {
        String url = baseUrl + DATA_SOURCES + "/" + dataSourceId + APIKEY_STRING + apiKey;
        return delete(url).isEmpty();
    }

    /**
     * Creates user-group onto Redash server
     *
     * @param userGroup should contain correct value name field, as it will be used in creation.
     * @return id of newly created group, if there wasn't with such name, and in this case argument's id field will
     * be set by value that was generated and returned by server in the response body.
     * @throws IllegalArgumentException if there is already exists user-group with the name provided inside dataSource
     * @throws IOException              if server is not available due to connection error or API key is invalid.
     */
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

    /**
     * Adds user into user-group both specified by their ids.
     *
     * @return false if specified user-group already contains specified user, true if not and user was successfully added.
     * @throws IllegalArgumentException if no user or no user group with provided userId and userGroupId accordingly exist
     *                                  onto Redash server.
     * @throws IOException              if server is not available due to connection error or API key is invalid.
     */
    //#6
    public boolean addUserToGroup(int userId, int groupId) throws IOException {
        User user = getUserById(userId);
        checkIfUserGroupExists(groupId);
        if (user.getGroups().contains(groupId)) {
            return false;
        }
        String url = baseUrl + GROUPS + "/" + groupId + MEMBERS + APIKEY_STRING + apiKey;
        String json = "{\"user_id\":" + userId + "}";
        post(url, json);
        return true;
    }

    /**
     * Adds data-source to user-group which are both specified by their ids in methods arguments.
     *
     * @return false if data-source already attached to group, true if not and has just added successfully to group
     * @throws IllegalArgumentException if data-source or user-group does not exist
     * @throws IOException              if server is not available due to connection error or API key is invalid.
     */
    //#7
    public boolean addDataSourceToGroup(int dataSourceId, int groupId) throws IOException {
        DataSource dataSource = getDataSourceById(dataSourceId);
        checkIfUserGroupExists(groupId);
        if (dataSource.getGroups().keySet().contains(groupId)) {
            return false;
        }
        String url = baseUrl + GROUPS + "/" + groupId + DATA_SOURCES + APIKEY_STRING + apiKey;
        String json = "{\"data_source_id\":" + dataSourceId + "}";
        post(url, json);
        return true;
    }

    /**
     * Removes user from user-group which are both specified by their ids in methods arguments.
     *
     * @return false if user is not a member of group, true if it is and then it will be successfully removed.
     * @throws IllegalArgumentException if either user or user-group does not exist.
     * @throws IOException              if server is not available due to connection error or API key is invalid.
     */
    //#8
    public boolean removeUserFromGroup(int userId, int groupId) throws IOException {
        User user = getUserById(userId);
        checkIfUserGroupExists(groupId);
        if (!user.getGroups().contains(groupId)) {
            return false;
        }
        String url = baseUrl + GROUPS + "/" + groupId + MEMBERS + "/" + userId + APIKEY_STRING + apiKey;
        delete(url, false);
        return true;
    }

    /**
     * Removes data-source from user-group which are both specified by their ids in methods arguments.
     *
     * @return false if data-source is not attached to group, true if it is and then it will be successfully removed.
     * @throws IllegalArgumentException if either data-source or user-group does not exist.
     * @throws IOException              if server is not available due to connection error or API key is invalid.
     */
    //#9
    public boolean removeDataSourceFromGroup(int dataSourceId, int groupId) throws IOException {
        DataSource dataSource = getDataSourceById(dataSourceId);
        checkIfUserGroupExists(groupId);
        if (!dataSource.getGroups().keySet().contains(groupId)) {
            return false;
        }
        String url = baseUrl + GROUPS + "/" + groupId + DATA_SOURCES + "/" + dataSourceId + APIKEY_STRING + apiKey;
        delete(url, false);
        return true;
    }

    /**
     * Retrieves all the user-groups from Redash server and maps them to UserGroup class.
     *
     * @return List<UserGroup> as result which will contain at least two user groups with names "admin" and "default".
     * @throws IOException if server is not available due to connection error or API key is invalid.
     */
    //#10
    public List<UserGroup> getUserGroups() throws IOException {
        String url = baseUrl + GROUPS + APIKEY_STRING + apiKey;
        Type listType = new TypeToken<ArrayList<UserGroup>>() {
        }.getType();
        return new Gson().fromJson(get(url), listType);
    }

    /**
     * Removes user-group from redash server by provided id as method's argument.
     *
     * @return true if user group with specified id was found on server and successfully deleted, false if the user-group
     * wasn't found and so couldn't be deleted.
     * @throws IOException if server is not available due to connection error or API key is invalid.
     */
    //#11
    public boolean deleteUserGroup(int userGroupId) throws IOException {
        String url = baseUrl + GROUPS + "/" + userGroupId + APIKEY_STRING + apiKey;
        String response = delete(url);
        if (!MESSAGE_INTERNAL_SERVER_ERROR.equals(response) && !"null".equals(response)) {
            throw new IOException(response);
        }
        return "null".equals(response);
    }

    /**
     * Retrieves all the existing users server from redash server, maps them onto User class and
     * returns instances as List.
     *
     * @throws IOException if server is not available due to connection error or API key is invalid.
     */
    //#12
    public List<User> getUsers() throws IOException {
        String url = baseUrl + USERS + APIKEY_STRING + apiKey;
        Type listType = new TypeToken<ArrayList<User>>() {
        }.getType();
        return new Gson().fromJson(get(url), listType);
    }

    /**
     * Calls the public List<User> getUsers() method, then filters obtained list using Stream API.
     *
     * @param userName the name of user which will be returned as result if successfully found.
     * @throws IllegalArgumentException if user with provided name wasn't found between existing on the Redash server.
     * @throws IOException              if server is not available due to connection error or API key is invalid.
     */
    //#13
    public User getUser(String userName) throws IOException {
        Optional<User> result = getUsers().stream().filter(e -> userName.equals(e.getName())).findFirst();
        if (!result.isPresent()) {
            throw new IllegalArgumentException(USER_DOES_NOT_EXIST);
        }
        return result.get();
    }

    /**
     * Calls the public List<DataSource> getDataSources() method, then filters obtained List<DataSource> using Stream API.
     *
     * @param dataSourceName the name of data-source which will be returned as result if successfully found.
     * @throws IllegalArgumentException if data-source with provided name wasn't found between existing on the Redash server.
     * @throws IOException              if server is not available due to connection error or API key is invalid.
     */
    //#14
    public DataSource getDataSource(String dataSourceName) throws IOException {
        Optional<DataSource> result = getDataSources().stream().filter(e -> dataSourceName.equals(e.getName())).findFirst();
        if (!result.isPresent()) {
            throw new IllegalArgumentException(DATA_SOURCE_DOES_NOT_EXIST);
        }
        return getDataSourceById(result.get().getId());
    }

    /**
     * Provides opportunity to retrieve the user-group from redash server by it's id.
     * Implemented as util method and also takes a part in several test methods in the RedashClientUserAndUserGroupTest class.
     * Only for this reason it has a public access. So after tests are passed access modifier could be changed to a private.
     *
     * @return UserGroup instance.
     * @throws IllegalArgumentException if there is no user-group with such id.
     * @throws IOException              if server is not available due to connection error or API key is invalid.
     */
    public UserGroup getUserGroupById(int userGroupId) throws IOException {
        String url = baseUrl + GROUPS + "/" + userGroupId + APIKEY_STRING + apiKey;
        String returnValue = get(url, false);
        return resultResolver(UserGroup.class, returnValue);
    }

    /**
     * Provides opportunity to retrieve the user from redash server by it's id.
     * Implemented as util method and also takes a part in several test methods in the RedashClientUserAndUserGroupTest class.
     * Only for this reason it has a public access. So after tests are passed access modifier could be changed to a private.
     *
     * @return User instance.
     * @throws IllegalArgumentException if there is no user with such id.
     * @throws IOException              if server is not available due to connection error or API key is invalid.
     */
    public User getUserById(int userId) throws IOException {
        String url = baseUrl + USERS + "/" + userId + APIKEY_STRING + apiKey;
        String returnValue = get(url, false);
        return resultResolver(User.class, returnValue);
    }

    /**
     * Provides opportunity to retrieve the data-source from redash server by it's id.
     * Implemented as util method and also takes a part in several test methods in the RedashClientDataSourceTest class.
     * Only for this reason it has a public access. So after tests are passed access modifier could be changed to a private.
     *
     * @return UserGroup instance
     * @throws IllegalArgumentException if there is no user-group with such id
     * @throws IOException              if server is not available due to connection error or API key is invalid.
     */
    public DataSource getDataSourceById(int id) throws IOException {
        String url = baseUrl + DATA_SOURCES + "/" + id + APIKEY_STRING + apiKey;
        String returnValue = get(url, false);
        if (MESSAGE_INTERNAL_SERVER_ERROR.equals(returnValue)) {
            throw new IllegalArgumentException(MESSAGE_INTERNAL_SERVER_ERROR);
        }
        return new Gson().fromJson(returnValue, DataSource.class);
    }

    private boolean isDataSourceAlreadyUpToDate(DataSource fromDataBase, DataSource whichToUpdate) {
        return fromDataBase.getHost().equals(whichToUpdate.getHost())
                && fromDataBase.getPort() == whichToUpdate.getPort()
                && fromDataBase.getUser().equals(whichToUpdate.getUser())
                && fromDataBase.getDbName().equals(whichToUpdate.getDbName());
    }

    private boolean isEntityAlreadyExists(List<? extends BaseEntity> list, String name) throws IOException {
        return list.stream().anyMatch(e -> name.equals(e.getName()));
    }

    private int getIdFromJson(String json) throws JSONException {
        return new JSONObject(json).getInt("id");
    }

    private void checkIfUserGroupExists(int groupId) throws IOException {
        getUserGroupById(groupId);
    }

    private <T extends BaseEntity> T resultResolver(Class<T> type, String returnValue) throws IOException {
        if (returnValue.startsWith(AUTH_TYPE) || returnValue.startsWith(CREATED_AT)) {
            return new Gson().fromJson(returnValue, type);
        }
        if (returnValue.startsWith(MESSAGE_USER_DOES_NOT_EXIST) || returnValue.equals(MESSAGE_INTERNAL_SERVER_ERROR)) {
            throw new IllegalArgumentException(returnValue);
        }
        throw new IOException(returnValue);
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
