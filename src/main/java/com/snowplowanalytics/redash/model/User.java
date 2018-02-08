package com.snowplowanalytics.redash.model;

import java.util.Objects;

public class User extends BaseEntity {

    public User(String name) {
        super(name);
    }

    public User(String name, int id) {
        super(name, id);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof User)) {
            return false;
        }
        User user = (User) o;
        return getId() == user.getId() &&
                Objects.equals(getName(), user.getName());
    }

}
