package com.snowplowanalytics.redash.model.datasource;


public class DataSource {
    private String name;
    private String type;
    private int id;
    private Options options;

    public DataSource(String name, String host, int port, String user, String password, String dbName, String type) {
        this.name = name;
        this.type = type;
        this.options = new Options(host, port, user, password, type);

    }
    public String getHost() {
        return this.options.getHost();
    }

    public int getPort() {
        return this.options.getPort();
    }

    public String getUser() {
        return this.options.getUser();
    }

    public String getPassword() {
        return this.options.getPassword();
    }

    public String getDbName() {
        return this.options.getDbName();
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
