package com.snowplowanalytics.redash;

import com.snowplowanalytics.redash.model.datasource.RedshiftDataSource;
import org.junit.BeforeClass;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AbstractRedashClientTest {
    public static RedashClient redashClient, wrongClient;
    public RedshiftDataSource rds = new RedshiftDataSource.RedshiftDataSourceBuilder("name")
            .host("host")
            .port(5439)
            .user("user")
            .password("password")
            .dbname("dbName")
            .build();

    @BeforeClass
    public static void onlyOnce() throws IOException {
        Properties prop = new Properties();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream stream = loader.getResourceAsStream("redashclient.properties");
        prop.load(stream);
        redashClient = new RedashClient(prop.getProperty("redash_schema"), prop.getProperty("redash_host"),
                Integer.parseInt(prop.getProperty("redash_port")),
                prop.getProperty("redash_apikey"));
        //client which has wrong apikey for test case with IOException
        wrongClient = new RedashClient(prop.getProperty("redash_schema"), prop.getProperty("redash_host"),
                Integer.parseInt(prop.getProperty("redash_port")), "wrong");
    }

}
