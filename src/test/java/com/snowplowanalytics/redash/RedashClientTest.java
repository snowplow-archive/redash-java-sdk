package com.snowplowanalytics.redash;

import com.snowplowanalytics.redash.model.datasource.DataSource;
import com.snowplowanalytics.redash.model.datasource.RedshiftDataSource;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.core.Is.is;

@RunWith(MockitoJUnitRunner.class)

public class RedashClientTest {

    private static RedashClient redashClient;
    private RedshiftDataSource rds = new RedshiftDataSource.RedshiftDataSourceBuilder("qw")
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
        redashClient = new RedashClient(prop.getProperty("redash_schema"),prop.getProperty("redash_host"),
                Integer.parseInt(prop.getProperty("redash_port")),
                prop.getProperty("redash_apikey"));
    }

    @Test
    public void createRetrieveDeleteTest() throws IOException {
        List<DataSource> dataSourceList = redashClient.getDataSources();
        Assert.assertTrue(dataSourceList.isEmpty());

        int id = redashClient.createDataSource(rds);
        dataSourceList = redashClient.getDataSources();
        Assert.assertTrue(dataSourceList.size() == 1);
        Assert.assertTrue(singleDataSourceMatcher(dataSourceList.get(0)));

        redashClient.deleteDataSource(id);
        dataSourceList = redashClient.getDataSources();
        Assert.assertTrue(dataSourceList.isEmpty());
    }

    @Test
    public void createRetrieveDeleteWithExceptionTest() throws IOException {
        List<DataSource> dataSourceList = redashClient.getDataSources();
        Assert.assertTrue(dataSourceList.isEmpty());
        int id = redashClient.createDataSource(rds);
        try {
            redashClient.createDataSource(rds);
        } catch (Exception e) {
            Assert.assertTrue(e.getClass().equals(IllegalArgumentException.class));
            Assert.assertThat(e.getMessage(), is("Data-source with this name already exists"));
        }
        redashClient.deleteDataSource(id);
        dataSourceList = redashClient.getDataSources();
        Assert.assertTrue(dataSourceList.isEmpty());

    }

    private boolean singleDataSourceMatcher(DataSource dataSource) {
        if (dataSource.getName() == null || dataSource.getType() == null) return false;
        return dataSource.getName().equals(rds.getName()) && dataSource.getType().equals(rds.getType());
    }

}
