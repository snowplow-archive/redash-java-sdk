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

import com.snowplowanalytics.redash.model.User;
import com.snowplowanalytics.redash.model.UserGroup;
import com.snowplowanalytics.redash.model.datasource.DataSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.snowplowanalytics.redash.RedashClientDataSourceTest.dataSourceMatcher;
import static com.snowplowanalytics.redash.RedashClientDataSourceTest.simpleDatasourceMatcher;

public class UserScenariosTest extends RedashClientUserAndUserGroupTest {

    @Before
    public void isDataSourcesListEmpty() throws IOException {
        wipeDataSources();
        Assert.assertTrue(redashClient.getDataSources().isEmpty());
    }

    /*  UA-1
        I need to add a new user-group to redash. Once added I need to add a specific already created
        user to this group and then attach a data-source to this group.
        After sometime has passed I need to remove this data-source from this group.
    */
    @Test
    public void firstScenarioTest() throws IOException {
        UserGroup userGroup = new UserGroup("test group");
        int userGroupId = redashClient.createUserGroup(userGroup);
        int dataSourceId = redashClient.createDataSource(rds);
        User user = redashClient.getUser(defaultUser.getName());
        Assert.assertTrue(redashClient.addUserToGroup(user.getId(), userGroupId));
        DataSource dataSource = redashClient.getDataSource(rds.getName());
        Assert.assertTrue(dataSourceMatcher(dataSource, rds));
        Assert.assertTrue(redashClient.addDataSourceToGroup(dataSourceId, userGroupId));
        UserGroup groupFromDb = redashClient.getWithUsersAndDataSources(userGroupId);
        Assert.assertTrue(groupFromDb.getUsers().size() == 1);
        Assert.assertTrue(groupFromDb.getDataSources().size() == 1);
        Assert.assertTrue(groupFromDb.getUsers().contains(defaultUser));
        Assert.assertTrue(simpleDatasourceMatcher(groupFromDb.getDataSources().get(0)));
        Assert.assertTrue(redashClient.removeDataSourceFromGroup(dataSource.getId(), userGroupId));
        groupFromDb = redashClient.getWithUsersAndDataSources(userGroupId);
        Assert.assertTrue(groupFromDb.getDataSources().isEmpty());
    }

    /*  UA - 2
        I need to add a new Redshift data-source to redash.
        Once added I need to attach this data-source to all created groups.
    * */
    @Test
    public void secondScenarioTest() throws IOException {
        int dataSourceId = redashClient.createDataSource(rds);
        UserGroup userGroup = new UserGroup("test group");
        redashClient.createUserGroup(userGroup);
        List<UserGroup> groups = redashClient.getUserGroups();
        for (UserGroup ug : groups) {
            if (ug.getId() != defaultGroup.getId()) {
                Assert.assertTrue(redashClient.addDataSourceToGroup(dataSourceId, ug.getId()));
            }
        }
        groups = redashClient.getUserGroups();
        for (UserGroup ug : groups) {
            if (ug.getId() != defaultGroup.getId()) {
                Assert.assertTrue(simpleDatasourceMatcher(redashClient.getWithUsersAndDataSources(ug.getId()).getDataSources().get(0)));
            }
        }
    }

    private void wipeDataSources() throws IOException {
        List<DataSource> dataSources = redashClient.getDataSources();
        for (DataSource ds : dataSources) {
            redashClient.deleteDataSource(ds.getId());
        }
    }
}
