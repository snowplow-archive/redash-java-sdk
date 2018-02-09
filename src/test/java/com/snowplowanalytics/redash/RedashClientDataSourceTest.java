package com.snowplowanalytics.redash;

import com.snowplowanalytics.redash.model.datasource.DataSource;
import com.snowplowanalytics.redash.model.datasource.RedshiftDataSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.List;

import static com.snowplowanalytics.redash.RedashClient.*;
import static org.hamcrest.core.Is.is;

/*
    All tests should be performed on clear database without any data sources.
    For that purpose there's implemented wipeDataSources() method.
    IT WILL DROP DATA SOURCES FROM REDASH SERVER
    You may uncomment it if you had some troubles in tests and there's a trash data which you want to delete.
* */
@RunWith(MockitoJUnitRunner.class)

public class RedashClientDataSourceTest extends AbstractRedashClientTest {

    @Before
    public void isDataSourcesListEmpty() throws IOException {
        wipeDataSources();
        Assert.assertTrue(redashClient.getDataSources().isEmpty());
    }

    @Test
    public void createRetrieveDeleteTest() throws IOException {
        int id = redashClient.createDataSource(rds);
        List<DataSource> dataSourceList = redashClient.getDataSources();
        Assert.assertTrue(dataSourceList.size() == 1);
        Assert.assertTrue(simpleDatasourceMatcher(dataSourceList.get(0)));

        redashClient.deleteDataSource(id);
        dataSourceList = redashClient.getDataSources();
        Assert.assertTrue(dataSourceList.isEmpty());
    }

    @Test
    public void createRetrieveDeleteWithExceptionTest() throws IOException {
        int id = redashClient.createDataSource(rds);
        try {
            redashClient.createDataSource(rds);
        } catch (Exception e) {
            Assert.assertTrue(e.getClass().equals(IllegalArgumentException.class));
            Assert.assertThat(e.getMessage(), is(DATA_SOURCE_ALREADY_EXISTS));
        }
        redashClient.deleteDataSource(id);
        List<DataSource> dataSourceList = redashClient.getDataSources();
        Assert.assertTrue(dataSourceList.isEmpty());

    }

    @Test
    public void successfulCreateDataSourceTest() throws IOException {
        int id = redashClient.createDataSource(rds);
        DataSource fromDB = redashClient.getDataSource(rds.getName());
        Assert.assertTrue(dataSourceMatcher(fromDB, rds));
        redashClient.deleteDataSource(id);
    }

    @Test
    public void unsuccessfulWithExistingNameCreateDataSourceTest() throws IOException {
        int id = redashClient.createDataSource(rds);
        try {
            redashClient.createDataSource(rds);
        } catch (Exception e) {
            Assert.assertTrue(e.getClass().equals(IllegalArgumentException.class));
            Assert.assertTrue(e.getMessage().equals(DATA_SOURCE_ALREADY_EXISTS));
        } finally {
            redashClient.deleteDataSource(id);
        }
    }

    @Test
    public void unsuccessfulWithIOExceptionCreateDataSourceTest() throws IOException {
        int id = redashClient.createDataSource(rds);
        try {
            wrongClient.createDataSource(rds);
        } catch (Exception e) {
            Assert.assertTrue(e.getClass().equals(IOException.class));
            Assert.assertTrue(e.getMessage().equals("NOT FOUND"));
        } finally {
            redashClient.deleteDataSource(id);
        }
    }

    @Test
    public void updateDataSourceTest() throws IOException {
        int id = redashClient.createDataSource(rds);
        RedshiftDataSource created = new RedshiftDataSource.RedshiftDataSourceBuilder("name")
                .host("updatedHost")
                .port(15439)
                .user("updatedUser")
                .password("updatedPassword")
                .dbname("updatedDatabase")
                .build();
        Assert.assertTrue(redashClient.updateDataSource(created));
        DataSource updatedFromDB = redashClient.getDataSourceById(id);
        Assert.assertTrue(dataSourceMatcher(updatedFromDB, created));
        Assert.assertFalse(redashClient.updateDataSource(created));
        redashClient.deleteDataSource(id);
    }

    @Test
    public void updateDataSourceWithExceptionTest() throws IOException {
        int id = redashClient.createDataSource(rds);
        try {
            wrongClient.updateDataSource(rds);
        } catch (Exception e) {
        } finally {
            redashClient.deleteDataSource(id);
        }
    }

    @Test
    public void getDataSourceTest() throws IOException {
        int id = redashClient.createDataSource(rds);
        DataSource fromDB = redashClient.getDataSource(rds.getName());
        Assert.assertTrue(dataSourceMatcher(fromDB, rds));
        redashClient.deleteDataSource(id);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDataSourceWhichNotExistsTest() throws IOException {
        redashClient.getDataSource("wrongName");
    }

    @Test
    public void getDataSourceWithIOExceptionTest() throws IOException {
        int id = redashClient.createDataSource(rds);
        DataSource fromDB = redashClient.getDataSource(rds.getName());
        Assert.assertTrue(dataSourceMatcher(fromDB, rds));
        try {
            wrongClient.getDataSource(rds.getName());
        } catch (Exception e) {
            Assert.assertTrue(e.getClass().equals(IOException.class));
        } finally {
            redashClient.deleteUserGroup(id);
        }
    }

    @Test
    public void getDataSourceByIdTest() throws IOException {
        int id = redashClient.createDataSource(rds);
        DataSource fromDb = redashClient.getDataSourceById(id);
        Assert.assertTrue(dataSourceMatcher(fromDb, rds));
        redashClient.deleteDataSource(id);
    }

    private boolean dataSourceMatcher(DataSource first, DataSource second) {
        if (first.getName() == null || first.getType() == null) return false;
        return first.getName().equals(second.getName()) &&
                first.getType().equals(second.getType()) &&
                first.getHost().equals(second.getHost()) &&
                first.getPort() == second.getPort() &&
                first.getDbName().equals(second.getDbName()) &&
                first.getUser().equals(second.getUser());
    }

    private boolean simpleDatasourceMatcher(DataSource dataSource) {
        if (dataSource.getName() == null || dataSource.getType() == null) return false;
        return dataSource.getName().equals(rds.getName()) && dataSource.getType().equals(rds.getType());
    }

    private void wipeDataSources() throws IOException {
        redashClient.getDataSources().forEach(dataSource -> {
            try {
                redashClient.deleteDataSource(dataSource.getId());
            } catch (IOException e){}
        });
    }
}
