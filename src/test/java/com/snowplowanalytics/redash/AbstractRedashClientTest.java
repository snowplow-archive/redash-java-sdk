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

package com.snowplowanalytics.redash;

import com.snowplowanalytics.redash.model.Group;
import com.snowplowanalytics.redash.model.User;
import com.snowplowanalytics.redash.model.datasource.RedshiftDataSource;
import org.junit.BeforeClass;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AbstractRedashClientTest {

    public static RedashClient redashClient, wrongClient;
    public static RedshiftDataSource rds = new RedshiftDataSource.RedshiftDataSourceBuilder("name")
            .host("host")
            .port(5439)
            .user("user")
            .password("password")
            .dbName("dbName")
            .build();
    public static User adminUser, defaultUser;
    public static String invalidUserName;
    public static Group adminGroup, defaultGroup;

    @BeforeClass
    public static void onlyOnce() throws IOException {
        Properties prop_1 = new Properties();
        Properties prop_2 = new Properties();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        InputStream stream_1 = loader.getResourceAsStream("redash_client.properties");
        prop_1.load(stream_1);

        InputStream stream_2 = loader.getResourceAsStream("redash_dynamic.properties");
        prop_2.load(stream_2);
        
        redashClient = new RedashClient(prop_1.getProperty("redash_schema"), prop_1.getProperty("redash_host"),
                Integer.parseInt(prop_1.getProperty("redash_port")),
                prop_2.getProperty("admin_api_key"));

        // Client which has wrong apikey for test case with IOException
        wrongClient = new RedashClient(prop_1.getProperty("redash_schema"), prop_1.getProperty("redash_host"),
                Integer.parseInt(prop_1.getProperty("redash_port")), "wrong");
    }

    @BeforeClass
    public static void loadUser() throws IOException {
        Properties prop = new Properties();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream stream = loader.getResourceAsStream("user_group.properties");
        prop.load(stream);
        adminUser = new User(prop.getProperty("admin_username"), 1);
        defaultUser = new User(prop.getProperty("default_username"), 2);
        invalidUserName = prop.getProperty("non_existent_username");
        adminGroup = new Group(prop.getProperty("admin_group"), 1);
        defaultGroup = new Group(prop.getProperty("default_group"), 2);
    }
}
