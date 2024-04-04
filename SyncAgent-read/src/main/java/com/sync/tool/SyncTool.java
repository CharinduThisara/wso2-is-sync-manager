package com.sync.tool;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.sync.tool.internal.SyncToolServiceDataHolder;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;

import org.wso2.carbon.user.core.claim.ClaimManager;
import org.wso2.carbon.user.core.claim.inmemory.InMemoryClaimManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.UserRealm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.net.InetSocketAddress;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import io.github.cdimascio.dotenv.Dotenv;

public class SyncTool {
    private static final Log log = LogFactory.getLog(SyncTool.class);
    private static String COSMOS_CONFIG_PATH;
    private CqlSession session;
    private String cassandraKeyspace;
    private String cassandraUserTable;
    private String cassandraRoleTable = "roles";
    private String region;
    private CustomJDBCUserStoreManager jdbcUserStoreManager;
    private RealmService realmService;

    public void init() {

        // Initialize Cassandra
        initializeCassandra();

        // Get the realm service
        realmService = SyncToolServiceDataHolder.getInstance().getRealmService();

        try{

            if (jdbcUserStoreManager == null) {

                // Get the realm configuration
                RealmConfiguration realmConfig = realmService.getTenantUserRealm(-1234).getRealmConfiguration();

                Map<String, Object> properties = new HashMap<String, Object>();

                // Create a claim manager
                ClaimManager claimManager = new InMemoryClaimManager();

                // Get user realm
                UserRealm realm = (UserRealm) realmService.getTenantUserRealm(-1234);
                log.info("User Realm: " + realm.toString());

                // Get tenant ID
                Integer tenantId = new Integer(realmService.getTenantManager().getTenantId("carbon.super"));

                // Create a JDBCUserStoreManager
                this.jdbcUserStoreManager = new CustomJDBCUserStoreManager(realmConfig, properties, claimManager, null, realm, tenantId);

                log.info("JDBCUserStoreManager created successfully" + jdbcUserStoreManager.toString());

            }
        }catch(Exception e){
            log.error("Error creating JDBCUserStoreManager: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeCassandra() {
        
        // Connect to Cosmos
        connectCosmos();
        log.info("Connected to Cosmos");

        // Create keyspace
        createKeySpace();
        log.info("Keyspace Created");

        // Create table
        createTable();
        log.info("Tables Created");
    }

    private void createTable() {

        // Create table query for user table
        String createTableQuery = "CREATE TABLE IF NOT EXISTS " + cassandraKeyspace + "." + cassandraUserTable + " ("
                                + "central_us BOOLEAN, "
                                + "east_us BOOLEAN, "
                                + "user_id TEXT, "
                                + "username TEXT, "
                                + "credential TEXT, "
                                + "role_list SET<TEXT>, "
                                + "claims MAP<TEXT, TEXT>,"
                                + "profile TEXT, "
                                + "PRIMARY KEY ((central_us, east_us), user_id));";

        // Create table query for role table
        String roleTableQuery   = "CREATE TABLE IF NOT EXISTS " + cassandraKeyspace + "."+cassandraRoleTable+" (" 
                                + "role_name TEXT, "
                                + "user_id TEXT, " 
                                + "central_us BOOLEAN, "
                                + "east_us BOOLEAN, " 
                                + "PRIMARY KEY ((central_us, east_us), role_name, user_id));";
        
        // Execute the queries to create the tables
        session.execute(roleTableQuery);
        session.execute(createTableQuery);
  
    }

    private void createKeySpace(){

        // Create keyspace query
        String createKeyspaceQuery = "CREATE KEYSPACE IF NOT EXISTS " + cassandraKeyspace + " WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 2};" ;

        // Execute the query to create the keyspace
        session.execute(createKeyspaceQuery);

    }

    private void connectCosmos() {

        SSLContext sc = null;

        try{

            // Create KeyManagerFactory
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

            // Initialize the KeyManagerFactory
            kmf.init(null, null);

            // Create TrustManagerFactory
            final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

            // Initialize the TrustManagerFactory with null keystore
            tmf.init((KeyStore) null);

            // Create SSLContext
            sc = SSLContext.getInstance("TLSv1.2");

            // Initialize the SSLContext with KeyManagerFactory and TrustManagerFactory
            sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            // Load the environment variables
            Dotenv dotenv = Dotenv.configure().load();

            // Get the environment variables
            COSMOS_CONFIG_PATH = dotenv.get("COSMOS_CONFIG_PATH");
            String cassandraHost = dotenv.get("COSMOS_CONTACT_POINT");
            int cassandraPort = Integer.parseInt(dotenv.get("COSMOS_PORT"));
            String cassandraUsername = dotenv.get("COSMOS_USER_NAME");
            String cassandraPassword = dotenv.get("COSMOS_PASSWORD");
            cassandraKeyspace = dotenv.get("COSMOS_KEYSPACE");
            cassandraUserTable = dotenv.get("COSMOS_TABLE");
            region = dotenv.get("COSMOS_REGION");

            log.info("COSMOS_CONFIG_PATH: "+COSMOS_CONFIG_PATH);
        
            // Load the configuration from the file
            DriverConfigLoader loader = DriverConfigLoader.fromFile(new File(COSMOS_CONFIG_PATH));

            log.info("Connecting to Cosmos "+cassandraHost+":"+cassandraPort+" with keyspace: "+cassandraKeyspace+" and table: "+cassandraUserTable);

            // Create a session with the Cosmos
            this.session = CqlSession.builder().withSslContext(sc)
                                               .addContactPoint(new InetSocketAddress(cassandraHost, cassandraPort))
                                               .withLocalDatacenter(region)
                                               .withConfigLoader(loader)   
                                               .withAuthCredentials(cassandraUsername, cassandraPassword)
                                               .build();
            
        }
        catch (Exception e) {
            log.error("Error connecting to Cosmos: " + e.getMessage());
            e.printStackTrace();
        }

    }

    private void updateRoles(ResultSet resultSet) {

        for (Row row : resultSet) {

            // Get the user details from the row
            String user_id = row.getString("user_id");

            // make role_list from the field role_name, which is a string
            String[] role_list = row.getString("role_name").split(",");

            // empty role list
            String [] empty_role_list = new String[0];

            try {

                // Check if the user exists in the system
                if (jdbcUserStoreManager.doCheckExistingUserWithID(user_id)) {

                    log.info("User exists in the system. Updating roles...");
                    log.info("User ID: " + user_id + ", Role List: " + role_list);

                    // Update the roles of the user
                    jdbcUserStoreManager.doUpdateRoleListOfUserWithID(user_id, empty_role_list, role_list);

                    // Delete the role record from Cosmos
                    deleteRoleRecord(user_id, role_list[0]);
                }
                
            } catch (Exception e) {
                System.out.println("Error updating roles: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void createUsers(ResultSet resultSet) {
    
        for (Row row : resultSet) {
            
            // Get the user details from the row
            String user_id = row.getString("user_id");
            String username = row.getString("username");
            String credential = row.getString("credential");
            String[] role_list = row.getSet("role_list", String.class).toArray(new String[0]);
            Map <String, String> claimsMap = row.getMap("claims", String.class, String.class);
            String claims = row.getMap("claims", String.class, String.class).toString();
            String profile = row.getString("profile");          
            
            try {

                // Check if the user exists in the system
                if (!jdbcUserStoreManager.doCheckExistingUserWithID(user_id)) {

                    log.info("User does not exist in the system. Adding user...");

                    // log the user details
                    log.info("User ID: " + user_id + ", Username: " + username + ", Credential: " + credential + ", Role List: " + role_list + ", Claims: " + claims + ", Profile: " + profile );
                    
                    // Add the user to the Database
                    jdbcUserStoreManager.doAddUserWithCustomID(user_id, username, credential, role_list, claimsMap, profile, false);

                    // Delete the user record from Cosmos
                    deleteUserRecord(user_id);
                }

            } catch (Exception e) {
                log.error("Error adding user: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void deleteRoleRecord(String user_id, String role_name) {

        String deleteRoleQuery = "DELETE FROM " + cassandraKeyspace + "." + cassandraRoleTable + " WHERE role_name = ? AND central_us = ? AND east_us = ? AND user_id = ?;";

        // Check if the data is written from Central US
        boolean isCentral = region.equals("Central US");

        try {
            
            // Prepare the delete query
            PreparedStatement preparedStatement = session.prepare(deleteRoleQuery);

            // Bind the parameters to the query
            BoundStatement boundStatement = preparedStatement.bind(role_name, !isCentral, isCentral, user_id);

            // Delete the user from Cosmos
            session.execute(boundStatement);

        } catch (Exception e) {
            log.error("Error deleting user from Cosmos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void deleteUserRecord(String user_id) {

        String user_query = String.format("DELETE FROM %s.%s WHERE central_us = ? AND east_us = ? AND user_id = ?", cassandraKeyspace, cassandraUserTable);
        boolean isCentral = region.equals("Central US");
        
        try {

            // Prepare the delete query
            PreparedStatement preparedStatement = session.prepare(user_query);

            // Bind the parameters to the query
            BoundStatement boundStatement = preparedStatement.bind(!isCentral, isCentral, user_id);

            // Delete the user from Cosmos
            session.execute(boundStatement);


        } catch (Exception e) {
            log.error("Error deleting user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void read() {
        
        try {

            // Check if the data is written from Central US
            boolean isCentral = region.equals("Central US");
            
            log.info("Keyspace: "+cassandraKeyspace + " Table: "+cassandraUserTable + " Region: "+region);

            String user_query = String.format("SELECT * FROM %s.%s WHERE central_us = %s ALLOW FILTERING;", cassandraKeyspace, cassandraUserTable, !isCentral);
            String role_query = String.format("SELECT * FROM %s.%s WHERE central_us = %s ALLOW FILTERING;", cassandraKeyspace, cassandraRoleTable, !isCentral);

            while (true) {

                // Execute the query to get the data from the users table in Cosmos
                ResultSet resultSet = session.execute(user_query); 

                // Write data to WSO2 IS
                createUsers(resultSet);

                // Execute the query to get the data from the roles table in Cosmos
                ResultSet roleResultSet = session.execute(role_query);

                // Write data to WSO2 IS
                updateRoles(roleResultSet);
                
                // Sleep for 1 second
                Thread.sleep(1000);

            }

        } catch (Exception e) {
            System.err.println("Error: " + e);
        }
    }

    public void close() {
        session.close();
    }  

}