package com.snowplowanalytics.redash.model.datasource;


public class DataSource {
    private String name;
    private String type;

    public DataSource(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
