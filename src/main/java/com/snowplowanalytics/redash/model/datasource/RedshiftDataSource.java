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

        public RedshiftDataSourceBuilder dbname(String dbName) {
            this.dbName = dbName;
            return this;
        }

        public RedshiftDataSource build() {
            return new RedshiftDataSource(this);
        }
    }

}
