package com.snowplowanalytics.redash;

import com.snowplowanalytics.redash.model.User;
import com.snowplowanalytics.redash.model.UserGroup;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

/*  Tests should be performed with only thouse user groups that are created by Redash server
    just after installation and registration process completed.
    For that purpose there's implemented wipeDataSources() method,
    IT WILL DROP USER GROUPS FROM REDASH SERVER EXCEPT ADMIN AND DEFAULT
    You may uncomment it if you had some troubles in tests and there's a trash data which you want to delete.
 */
public class RedashClientUserAndUserGroupTest extends AbstractRedashClientTest {
    private static User adminUser, defaultUser;
    private static String invalidUserName;
    private static UserGroup adminGroup, defaultGroup;

    @BeforeClass
    public static void loadUser() throws IOException {
        Properties prop = new Properties();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream stream = loader.getResourceAsStream("user_usergroup.properties");
        prop.load(stream);
        adminUser = new User(prop.getProperty("admin_username"), 1);
        defaultUser = new User(prop.getProperty("default_username"), 2);
        invalidUserName = prop.getProperty("non_existent_username");
        adminGroup = new UserGroup(prop.getProperty("admin_group"), 1);
        defaultGroup = new UserGroup(prop.getProperty("default_group"), 2);
    }

    @Before
    public void setup() throws IOException {
        wipeAllCreatedUserGroups();
    }

    @Test
    public void successfulCreateUserGroupTest() throws IOException {
        List<UserGroup> userGroups = redashClient.getUserGroups();
        UserGroup created = new UserGroup("name");
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
        } finally {
            System.out.println(redashClient.deleteUserGroup(id));
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
        Assert.assertTrue(users.get(0).equals(defaultUser));
        Assert.assertTrue(users.get(1).equals(adminUser));
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
