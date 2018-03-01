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

package com.snowplowanalytics.redash.model.datasource;

import com.snowplowanalytics.redash.model.BaseEntity;

public class DataSource extends BaseEntity{

    private final String type;
    private final Options options;

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

    public Options getOptions() {
        return options;
    }

    @Override
    public String toString() {
        return "DataSource{" +
                "type='" + type + '\'' +
                ", options=" + options +
                ", name='" + getName() + '\'' +
                ", id=" + getId() +
                "} ";
    }
}
