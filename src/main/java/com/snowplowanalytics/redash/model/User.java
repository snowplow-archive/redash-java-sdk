package com.snowplowanalytics.redash.model;

import java.util.List;
import java.util.Objects;

public class User extends BaseEntity {
    private List<Integer> groups;

    public User(String name) {
        super(name);
    }

    public User(String name, int id) {
        super(name, id);
    }

    public List<Integer> getGroups() {
        return groups;
    }

    public void setGroups(List<Integer> groups) {
        this.groups = groups;
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
