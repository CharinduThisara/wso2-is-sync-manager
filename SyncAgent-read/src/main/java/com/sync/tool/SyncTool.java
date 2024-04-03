package com.sync.tool;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.sync.tool.internal.SyncToolServiceDataHolder;

import org.wso2.carbon.user.core.claim.ClaimManager;
import org.wso2.carbon.user.core.claim.inmemory.InMemoryClaimManager;

import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.UserRealm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import com.datastax.oss.driver.api.core.config.DriverConfigLoader;

import io.github.cdimascio.dotenv.Dotenv;

public class SyncTool {
    private static final Log log = LogFactory.getLog(SyncTool.class);
    private static String COSMOS_CONFIG_PATH;
    private CqlSession session;
    private String cassandraKeyspace;
    private String cassandraTable;
    private String region;
    private CustomJDBCUserStoreManager jdbcUserStoreManager;
    RealmService realmService;

    public void init() {
        connectCosmos();
        realmService = SyncToolServiceDataHolder.getInstance().getRealmService();
        try{

            if(jdbcUserStoreManager==null){
                // realmService.getTenantUserRealm(-1234).getRealmConfiguration().getUserStoreProperties().put("dataSource", "jdbc/SHARED_DB");

                RealmConfiguration realmConfig = realmService.getTenantUserRealm(-1234).getRealmConfiguration();
                Map<String, Object> properties = new HashMap<String, Object>();
                ClaimManager claimManager = new InMemoryClaimManager();
                UserRealm realm = (UserRealm) realmService.getTenantUserRealm(-1234);
                Integer tenantId = new Integer(realmService.getTenantManager().getTenantId("carbon.super"));

                this.jdbcUserStoreManager = new CustomJDBCUserStoreManager(realmConfig, properties, claimManager, null, realm, tenantId);
                System.out.println("Realm Service: "+realmService.getTenantManager().getTenantId("carbon.super"));
                System.out.println("Tenant User Realm: "+realmService.getTenantUserRealm(-1234).getRealmConfiguration());
                System.out.println(this.jdbcUserStoreManager.getClaimManager());
            }
        }catch(Exception e){
            System.out.println("Error creating JDBCUserStoreManager: "+e.getMessage());
            e.printStackTrace();
        }   
    }

    private void connectCosmos() {

        SSLContext sc = null;
        try{

            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(null, null);

            final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);

            sc = SSLContext.getInstance("TLSv1.2");
            sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            Dotenv dotenv = Dotenv.configure().load();

            COSMOS_CONFIG_PATH = dotenv.get("COSMOS_CONFIG_PATH");
            String cassandraHost = dotenv.get("COSMOS_CONTACT_POINT");
            int cassandraPort = Integer.parseInt(dotenv.get("COSMOS_PORT"));
            String cassandraUsername = dotenv.get("COSMOS_USER_NAME");
            String cassandraPassword = dotenv.get("COSMOS_PASSWORD");
            cassandraKeyspace = dotenv.get("COSMOS_KEYSPACE");
            cassandraTable = dotenv.get("COSMOS_TABLE");
            region = dotenv.get("COSMOS_REGION");

            System.out.println("COSMOS_CONFIG_PATH: "+COSMOS_CONFIG_PATH);
        
            DriverConfigLoader loader = DriverConfigLoader.fromFile(new File(COSMOS_CONFIG_PATH));

            System.out.println("Connecting to Cosmos "+cassandraHost+":"+cassandraPort+" with keyspace: "+cassandraKeyspace+" and table: "+cassandraTable);

            this.session = CqlSession.builder().withSslContext(sc)
            .addContactPoint(new InetSocketAddress(cassandraHost, cassandraPort)).withLocalDatacenter(region)
            .withConfigLoader(loader)   
            .withAuthCredentials(cassandraUsername, cassandraPassword).build();
            
        }
        catch (Exception e) {
            System.out.println("Error creating session");
            e.printStackTrace();
        }

    }

    public void updateRoles(ResultSet resultSet) {
        for (Row row : resultSet) {
            String user_id = row.getString("user_id");
            // make role_list from the field role_name, which is a string
            String[] role_list = row.getString("role_name").split(",");


            // empty role list
            String [] empty_role_list = new String[0];
            System.out.println("User ID: " + user_id);
            System.out.println("Role List: " + role_list);
            try {
                if (jdbcUserStoreManager.doCheckExistingUserWithID(user_id)) 
                    jdbcUserStoreManager.doUpdateRoleListOfUserWithID(user_id, empty_role_list, role_list);
            } catch (Exception e) {
                System.out.println("Error updating roles: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void writeToDB(ResultSet resultSet) {
    
        for (Row row : resultSet) {
            
            String user_id = row.getString("user_id");
            String username = row.getString("username");
            String credential = row.getString("credential");
            String[] role_list = row.getSet("role_list", String.class).toArray(new String[0]);
            Map <String, String> claimsMap = row.getMap("claims", String.class, String.class);
            String claims = row.getMap("claims", String.class, String.class).toString();
            String profile = row.getString("profile");
            boolean central_us = row.getBoolean("central_us");
            boolean east_us = row.getBoolean("east_us");
            
            System.out.println();
            System.out.println();

            System.out.println("User ID: " + user_id);
            System.out.println("Username: " + username);
            System.out.println("Credential: " + credential);
            System.out.println("Role List: " + role_list);
            System.out.println("Claims: " + claims);
            System.out.println("Profile: " + profile);
            System.out.println("Central US: " + central_us);
            System.out.println("East US: " + east_us);
            
            System.out.println();
            
            
            try {
                if (!jdbcUserStoreManager.doCheckExistingUserWithID(user_id)) {
                    System.out.println("User does not exist in the system. Adding user...");
                    jdbcUserStoreManager.doAddUserWithCustomID(user_id, username, credential, role_list, claimsMap, profile, false);
                } else {
                    System.out.println("User already exists in the system...");
                }
            } catch (Exception e) {
                System.out.println("Error adding user: " + e.getMessage());
                e.printStackTrace();
            }
        }
    
}

    public void read() {
        
        try {
            boolean central_us;
            if (region.equals("Central US")) {
                central_us = false;
            } else {
                central_us = true;
            }
            System.out.println("Keyspace: "+cassandraKeyspace + " Table: "+cassandraTable + " Region: "+region);
            System.out.println("Connected to Cassandra.");

            String query = String.format("SELECT * FROM %s.%s WHERE central_us = %s ALLOW FILTERING;", cassandraKeyspace, cassandraTable, central_us);
            String role_query = String.format("SELECT * FROM %s.%s WHERE central_us = %s ALLOW FILTERING;", cassandraKeyspace, cassandraTable, central_us);

            while (true) {
                // System.out.println("Reading data from Cassandra...");
                ResultSet resultSet = session.execute(query);
                // System.out.println("Data read from Cassandra.");

                // Write data to WSO2 IS
                writeToDB(resultSet);
                ResultSet roleResultSet = session.execute(role_query);
                updateRoles(roleResultSet);
                
                Thread.sleep(1000);
                // System.out.println();
                // System.out.println();
            }

        } catch (Exception e) {
            System.err.println("Error: " + e);
        }
    }

    public void close() {
        session.close();
    }

    public static void main(String[] args) {
        SyncTool syncTool = new SyncTool();
        syncTool.connectCosmos();
        System.out.println("Connected to Cosmos");
        System.out.println("..........................................");
        System.out.println("..........................................");
        syncTool.read();

    }
  

}