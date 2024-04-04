package org.wso2.custom.user.operation.event.listener;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.*;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;

import groovy.transform.builder.InitializerStrategy.SET;

import java.net.InetSocketAddress;

import java.security.KeyStore;

import javax.net.ssl.SSLContext;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.File;
import java.util.Arrays;

/**
 *
 */
public class CustomGroupOperationEventListener extends AbstractGroupOperationEventListener {

    private static final String DEFAULT_KEYSPACE = "my_keyspace";
    private static final String DEFAULT_DATA_CENTER = "datacenter1";
    private static final String DEFAULT_TABLE = "my_table";
    private static final String DEFAULT_NODE = "127.0.0.1";
    private static final int DEFAULT_PORT = 9042;
    
    private static String COSMOS_CONFIG_PATH;
    private String systemUserPrefix = "system_";
    private CqlSession session;
    private String cassandraKeyspace;
    private String cassandraTable;   
    private String region;
    
        
    public CustomGroupOperationEventListener() {
        super();

        initializeCassandra();
    }

    public void initializeCassandra(){
        
        connectCosmos();
        System.out.println("Connected to Cosmos");

    
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
    
    public void connectCosmos() {

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
            region = dotenv.get("COSMOS_REGION");
            String cassandraUsername = dotenv.get("COSMOS_USER_NAME");
            String cassandraPassword = dotenv.get("COSMOS_PASSWORD");
            cassandraKeyspace = dotenv.get("COSMOS_KEYSPACE");
            cassandraTable = dotenv.get("COSMOS_TABLE");
        
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

    private void close() {
        this.session.close();
    }

    @Override
    public int getExecutionOrderId() {
        return 9000;
    }

    @Override
    public boolean postUpdateUserListOfGroup(String groupId, List<String> deletedUserIds, List<String> newUserIds,
                                            UserStoreManager userStoreManager) throws UserStoreException {
        System.out.println("doPreUpdateUserListOfGroup");
        System.out.println("Group ID: " + groupId);
        System.out.println("Deleted User IDs: " + deletedUserIds);
        System.out.println("New User IDs: " + newUserIds);

        return true;
    }
}

