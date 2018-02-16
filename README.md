for the# redash-java-sdk
Before launching `RedashClientDataSourceTest`, `RedashClientUserAndUserGroupTest` & `UserScenariosTest` test classes please do the following:
 - please create new user onto redash instance, it's name should be then filled into `user_usergroup.properties` file.     
 - please read comments inside all those classes;
 - please put your Redash server properties (schema, hostname, port, api key) into `redashclient.properties` file, all neccessary user & user group credentials (usernames of admin and default user, names of admin and default group) into 
`user_usergroup.properties`. They are both lying in the `resources` direcory inside test package.
  
After each test method if it successfully passed redash server stays in the same state as before tests.
