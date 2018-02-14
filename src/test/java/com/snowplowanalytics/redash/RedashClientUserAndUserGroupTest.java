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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

/** Tests should be performed with only those user groups that are created by Redash server
    just after installation and registration process completed.
    For that purpose there's implemented wipeDataSources() method,
    IT WILL DROP USER GROUPS FROM REDASH SERVER EXCEPT ADMIN AND DEFAULT
 */
public class RedashClientUserAndUserGroupTest extends AbstractRedashClientTest {

    @Before
    public void setup() throws IOException {
        wipeAllCreatedUserGroups();
    }

    @Test
    public void successfulCreateUserGroupTest() throws IOException {
        List<UserGroup> userGroups = redashClient.getUserGroups();
        UserGroup created = new UserGroup("testGroup");
        Assert.assertTrue(userGroups.size() == 2);
        int id = redashClient.createUserGroup(created);
        userGroups = redashClient.getUserGroups();
        Assert.assertTrue(userGroups.size() == 3);
        Assert.assertTrue(userGroups.contains(created));
        redashClient.deleteUserGroup(id);
    }

    @Test(expected = IOException.class)
    public void getUserGroupsTest() throws IOException {
        wrongClient.getUserGroups();
    }

    @Test
    public void deleteUserGroupTest() throws IOException {
        UserGroup created = new UserGroup("name");
        int id = redashClient.createUserGroup(created);
        Assert.assertTrue(redashClient.getUserGroups().size() == 3);
        Assert.assertFalse(redashClient.deleteUserGroup(id + 1));
        Assert.assertTrue(redashClient.getUserGroups().size() == 3);
        Assert.assertTrue(redashClient.deleteUserGroup(id));
        Assert.assertTrue(redashClient.getUserGroups().size() == 2);
        id = redashClient.createUserGroup(created);
        Assert.assertTrue(redashClient.getUserGroups().size() == 3);
        try {
            wrongClient.deleteUserGroup(id);
        } catch (Exception e) {
            Assert.assertTrue(e.getClass().equals(IOException.class));
        }
    }

    @Test
    public void unsuccessfulWithExistingNameCreateUserGroupTest() throws IOException {
        List<UserGroup> userGroups = redashClient.getUserGroups();
        Assert.assertTrue(userGroups.size() == 2);
        try {
            redashClient.createUserGroup(new UserGroup(defaultGroup.getName()));
        } catch (Exception e) {
            Assert.assertTrue(e.getClass().equals(IllegalArgumentException.class));
        }
        userGroups = redashClient.getUserGroups();
        Assert.assertTrue(userGroups.size() == 2);
    }

    @Test
    public void unsuccessfulWithWrongClientCreateUserGroupTest() throws IOException {
        List<UserGroup> userGroups = redashClient.getUserGroups();
        Assert.assertTrue(userGroups.size() == 2);
        try {
            wrongClient.createUserGroup(new UserGroup(defaultUser.getName()));
        } catch (Exception e) {
            Assert.assertTrue(e.getClass().equals(IOException.class));
        }
        userGroups = redashClient.getUserGroups();
        Assert.assertTrue(userGroups.size() == 2);
    }

    @Test
    public void getUsersTest() throws IOException {
        List<User> users = redashClient.getUsers();
        Assert.assertTrue(users.size() == 2);
        Assert.assertTrue(users.get(0).equals(adminUser));
        Assert.assertTrue(users.get(1).equals(defaultUser));
    }

    @Test(expected = IOException.class)
    public void getUsersWithIOExceptionTest() throws IOException {
        wrongClient.getUsers();
    }

    @Test
    public void getUserTest() throws IOException {
        User user = redashClient.getUser(adminUser.getName());
        Assert.assertTrue(user.equals(adminUser));
    }

    @Test(expected = IllegalArgumentException.class)
    public void unsuccessfulGetUserTest() throws IOException {
        redashClient.getUser(invalidUserName);
    }

    @Test(expected = IOException.class)
    public void getUserWithIOExceptionTest() throws IOException {
        wrongClient.getUser(defaultUser.getName());
    }

    @Test
    public void addUserToUserGroupTest() throws IOException {
        UserGroup createdUserGroup = new UserGroup("createdForTest");
        int createdUserGroupId = redashClient.createUserGroup(createdUserGroup);
        User fromDb = redashClient.getUser(defaultUser.getName());
        Assert.assertFalse(fromDb.getGroups().contains(createdUserGroupId));
        Assert.assertTrue(redashClient.addUserToGroup(fromDb.getId(), createdUserGroupId));
        fromDb = redashClient.getUser(defaultUser.getName());
        Assert.assertTrue(fromDb.getGroups().size() == 1);
        Assert.assertTrue(fromDb.getGroups().contains(createdUserGroupId));
        Assert.assertFalse(redashClient.addUserToGroup(fromDb.getId(), createdUserGroupId));
        Assert.assertTrue(fromDb.getGroups().size() == 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addNonExistingUserToUserGroup() throws IOException {
        redashClient.addUserToGroup(4, defaultGroup.getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void addUserToNonExistingUserGroup() throws IOException {
        redashClient.addUserToGroup(defaultUser.getId(), 3);
    }

    @Test(expected = IOException.class)
    public void addUserToUserGroupTestWithWrongKey() throws IOException {
        wrongClient.addUserToGroup(defaultUser.getId(), defaultGroup.getId());
    }

    @Test
    public void removeUserFromGroupTest() throws IOException {
        User fromDb = redashClient.getUser(defaultUser.getName());
        Assert.assertTrue(fromDb.getGroups().isEmpty());
        Assert.assertTrue(redashClient.addUserToGroup(defaultUser.getId(), defaultGroup.getId()));
        fromDb = redashClient.getUser(defaultUser.getName());
        Assert.assertTrue(fromDb.getGroups().size() == 1);
        Assert.assertTrue(fromDb.getGroups().contains(defaultGroup.getId()));
        Assert.assertTrue(redashClient.removeUserFromGroup(defaultUser.getId(), defaultGroup.getId()));
        fromDb = redashClient.getUser(defaultUser.getName());
        Assert.assertTrue(fromDb.getGroups().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeNonExistingUserFromGroupTest() throws IOException {
        redashClient.removeUserFromGroup(defaultUser.getId() + 1, defaultGroup.getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeUserFromNonExistingGroupTest() throws IOException {
        redashClient.removeUserFromGroup(defaultUser.getId(), defaultGroup.getId() + 1);
    }

    @Test(expected = IOException.class)
    public void removeUserGroupWithWrongClientTest() throws IOException {
        wrongClient.removeUserFromGroup(defaultUser.getId(), defaultGroup.getId());
    }

    private void wipeAllCreatedUserGroups() throws IOException {
        redashClient.getUserGroups().forEach(userGroup -> {
            int id = userGroup.getId();
            try {
                if (id != 1 && id != 2) {
                    redashClient.deleteUserGroup(id);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
