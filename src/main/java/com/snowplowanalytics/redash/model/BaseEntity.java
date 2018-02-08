package com.snowplowanalytics.redash.model;

import java.util.Objects;

public class BaseEntity {
    private String name;
    private int id;

    public BaseEntity(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public BaseEntity(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName());
    }
}
