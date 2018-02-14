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

import static com.snowplowanalytics.redash.RedashClientDataSourceTest.dataSourceMatcher;

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
        Assert.assertTrue(redashClient.getDataSource(rds.getName()).getGroups().containsKey(userGroupId));
        Assert.assertTrue(redashClient.removeDataSourceFromGroup(dataSource.getId(), userGroupId));
        Assert.assertFalse(redashClient.getDataSource(dataSource.getName()).getGroups().containsKey(userGroupId));
    }

    /*  UA - 2
        I need to add a new Redshift data-source to redash.
        Once added I need to attach this data-source to all created groups.
    * */
    @Test
    public void secondScenarioTest() throws IOException {
        int dataSourceId = redashClient.createDataSource(rds);
        UserGroup userGroup = new UserGroup("test group");
        int userGroupId = redashClient.createUserGroup(userGroup);
        redashClient.getUserGroups()
                .stream()
                .filter(ug -> ug.getId() != defaultUser.getId())
                .forEach(ug -> {
                    try {
                        Assert.assertTrue(
                                redashClient.addDataSourceToGroup(dataSourceId, ug.getId())
                        );
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        Assert.assertTrue(redashClient
                .getDataSource(rds.getName()).getGroups().keySet()
                .containsAll(Arrays.asList(defaultUser.getId(), adminUser.getId(), userGroupId)));
    }

    private void wipeDataSources() throws IOException {
        redashClient.getDataSources().forEach(dataSource -> {
            try {
                redashClient.deleteDataSource(dataSource.getId());
            } catch (IOException e) {
            }
        });
    }
}
