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

public class RedshiftDataSource extends DataSource {

    private RedshiftDataSource(RedshiftDataSourceBuilder builder) {
        super(builder.name, builder.host, builder.port, builder.user, builder.password, builder.dbName, "redshift");
    }

    public final static class RedshiftDataSourceBuilder {
        private String name;
        private String host;
        private int port;
        private String user;
        private String password;
        private String dbName;

        public RedshiftDataSourceBuilder(String name) {
            this.name = name;
        }

        public RedshiftDataSourceBuilder host(String host) {
            this.host = host;
            return this;
        }

        public RedshiftDataSourceBuilder port(int port) {
            this.port = port;
            return this;
        }

        public RedshiftDataSourceBuilder user(String user) {
            this.user = user;
            return this;
        }

        public RedshiftDataSourceBuilder password(String password) {
            this.password = password;
            return this;
        }

        public RedshiftDataSourceBuilder dbName(String dbName) {
            this.dbName = dbName;
            return this;
        }

        public RedshiftDataSource build() {
            return new RedshiftDataSource(this);
        }
    }
}
