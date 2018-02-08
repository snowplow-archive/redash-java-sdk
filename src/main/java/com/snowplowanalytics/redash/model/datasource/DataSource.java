package com.snowplowanalytics.redash.model.datasource;


import com.snowplowanalytics.redash.model.BaseEntity;

public class DataSource extends BaseEntity{
    private String type;
    private Options options;

    public DataSource(String name, String host, int port, String user, String password, String dbName, String type) {
        super(name);
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

}
