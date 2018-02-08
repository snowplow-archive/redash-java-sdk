package com.snowplowanalytics.redash.model;

import java.util.Objects;

public class UserGroup extends BaseEntity{

    public UserGroup(String name) {
        super(name);
    }

    public UserGroup(String name, int id) {
        super(name, id);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof UserGroup)) {
            return false;
        }
        UserGroup o1 = (UserGroup) o;
        return getId() == o1.getId() &&
                Objects.equals(getName(), o1.getName());
    }

}
