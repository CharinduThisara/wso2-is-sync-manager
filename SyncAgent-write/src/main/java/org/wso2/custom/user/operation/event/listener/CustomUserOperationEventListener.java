package org.wso2.custom.user.operation.event.listener;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.util.Arrays;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.*;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;

import io.github.cdimascio.dotenv.Dotenv;

public class CustomUserOperationEventListener extends AbstractUserOperationEventListener {

        private static final Log log = LogFactory.getLog(CustomUserOperationEventListener.class);


    private static final String DEFAULT_KEYSPACE = "my_keyspace";
    private static final String DEFAULT_DATA_CENTER = "datacenter1";
    private static final String DEFAULT_TABLE = "my_table";
    private static final String DEFAULT_NODE = "127.0.0.1";
    private static final int DEFAULT_PORT = 9042;
    
    private String cassandraRoleTable = "roles";
    private static String COSMOS_CONFIG_PATH;
    private CqlSession session;
    private String cassandraKeyspace;
    private String cassandraUserTable;   
    private String region;
    
        
    public CustomUserOperationEventListener() {

        super();

        // Initialize the Cassandra connection
        initializeCassandra();
    }

    public void initializeCassandra() {
        
        // Connect to Cosmos
        connectCosmos();
        log.info("Connected to Cosmos");

        // Create keyspace
        createKeySpace();
        log.info("Keyspace Created");

        // Create table
        createTable();
        log.info("Table Created");
    }

    public void connectToLocalCassandra() {
        
        File file = new File(COSMOS_CONFIG_PATH);

        DriverConfigLoader loader = DriverConfigLoader.fromFile(file);
        CqlSessionBuilder builder = CqlSession.builder();
        builder.addContactPoint(new InetSocketAddress(DEFAULT_NODE, DEFAULT_PORT));
        builder.withLocalDatacenter(DEFAULT_DATA_CENTER);
        builder.withConfigLoader(loader);
        
        this.session = builder.build();

        System.out.println("Session Created : " + session.getName());
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
    
    public void close() {
        this.session.close();
    }

    @Override
    public int getExecutionOrderId() {
        return 9000;
    }

    @Override
    public boolean doPreDeleteUser
    (String s, UserStoreManager userStoreManager) throws UserStoreException {
        
        System.out.println("User deleted successfully");
        System.out.println("User Name: " + s);
        System.out.println("User Store Manager: " + userStoreManager);

        return true;
    }

    @Override
    public boolean doPreDeleteUserWithID
            (String s, UserStoreManager userStoreManager) throws UserStoreException {
            System.out.println("doPreDeleteUserWithID");
        
            return true;
    }

    @Override
    public boolean doPostAddGroup(String groupName, List<String> userIDs, UserStoreManager userStoreManager)
            throws UserStoreException {

        System.out.println("Group added successfully " + groupName);

        return true;
    }

    @Override
    public boolean doPostUpdateUserListOfRoleWithID(String roleName, String[] deletedUsers, String[] newUsers,
            UserStoreManager userStoreManager) throws UserStoreException {
                System.out.println("doPostUpdateUserListOfRoleWithID");
                System.out.println("Role Name: " + roleName);
                System.out.println("Deleted Users: ");
                for (String userId : deletedUsers) {
                    System.out.println(userId);
                }
                System.out.println("New Users: ");
                for (String userId : newUsers) {
                    System.out.println(userId);
                }

                // get first user id
                String userId = newUsers[0];
                final String INSERT_ROLE_QUERY = "INSERT INTO sync.roles (role_name, user_id, central_us, east_us) VALUES (?, ?, ?, ?)";

                try {
                // Writing data to the user_data table
                PreparedStatement preparedStatement = session.prepare(INSERT_ROLE_QUERY);
                boolean central_us = region.equals("Central US");
                boolean east_us = !central_us;
                BoundStatement boundStatement = preparedStatement.bind(
                    roleName,                // role_name
                    userId,             // user_id
                    central_us,               // central_us
                    east_us);             // east_us
                session.execute(boundStatement);
    
                System.out.println("Data written to roles table successfully.");
               
            } catch (Exception e) {
                System.err.println("Error: " + e);
            }
                    
                return true;
    }

    
    @Override
    public boolean doPostAddUser(String userName, Object credential, String[] roleList, Map<String, String> claims, String profile, UserStoreManager userStoreManager) throws UserStoreException {

        System.out.println("User added successfully");
        System.out.printf("User Name: %s\n", userName);
        System.out.printf("User Credential: %s\n", credential);
        System.out.printf("User Store Manager: %s\n", userStoreManager);
        System.out.println("User Claims:");
        printMap(claims);
        System.out.println("User Roles:");
        printArray(roleList);
        System.out.printf("User Profile: %s\n", profile);

        final String INSERT_USER_QUERY = "INSERT INTO "+ cassandraKeyspace +"."+cassandraUserTable+" (user_id, username, credential, role_list, claims, profile, central_us, east_us) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try{

            // Prepare the insert statement
            PreparedStatement preparedStatement = session.prepare(INSERT_USER_QUERY);

            String userId = claims.get("http://wso2.org/claims/userid");
            Set<String> roleSet = new HashSet<>(Arrays.asList(roleList));
            boolean central_us;

            central_us = region.equals("Central US");
            
            // Execute the insert statement
            session.execute(preparedStatement.bind(
                    userId,                
                    userName,
                    credential.toString(),
                    roleSet,              
                    claims,               
                    profile,
                    central_us,
                    !central_us
                    ));

        }
        catch(Exception e){
            System.out.println("Error: " + e);
            e.printStackTrace();
        }
   
        return true;
    }

    private void printMap(Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            System.out.printf("%s: %s\n", entry.getKey(), entry.getValue());
        }
    }
    
    private void printArray(String[] array) {
        for (String item : array) {
            System.out.println(item);
        }
    }
}

