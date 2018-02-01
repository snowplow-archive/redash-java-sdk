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

package com.snowplowanalytics.redash.model;

import com.snowplowanalytics.redash.model.datasource.DataSource;

import java.util.List;
import java.util.Objects;

public class Group extends BaseEntity {

    private List<User> users;
    private List<DataSource> dataSources;

    public Group(String name) {
        super(name);
    }

    public Group(String name, int id) {
        super(name, id);
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public List<DataSource> getDataSources() {
        return dataSources;
    }

    public void setDataSources(List<DataSource> dataSources) {
        this.dataSources = dataSources;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Group)) {
            return false;
        }
        Group o1 = (Group) o;
        return getId() == o1.getId() &&
                Objects.equals(getName(), o1.getName());
    }

    @Override
    public String toString() {
        return "Group{" +
                "name='" + getName() + '\'' +
                ", id=" + getId() +
                '}';
    }
}
