package com.hfsolutions.model.datasource;

import com.google.gson.annotations.SerializedName;

public class Options {
    private String host;
    private int port;
    private String user;
    private String password;
    @SerializedName("dbname")
    private String dbName;

    public Options(String host, int port, String user, String password, String dbName) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.dbName = dbName;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getDbName() {
        return dbName;
    }

}
