package com.sync.tool;

import java.util.Map;

import org.wso2.carbon.user.core.claim.ClaimManager;
import org.wso2.carbon.user.core.profile.ProfileConfigurationManager;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.common.User;
import org.wso2.carbon.user.core.jdbc.UniqueIDJDBCUserStoreManager;

public class CustomJDBCUserStoreManager extends UniqueIDJDBCUserStoreManager {

    public CustomJDBCUserStoreManager(RealmConfiguration realmConfig, Map<String, Object> properties,
    ClaimManager claimManager, ProfileConfigurationManager profileManager, UserRealm realm, Integer tenantId)
    throws UserStoreException {

        super(realmConfig, properties, claimManager, profileManager, realm, tenantId);
    }

    // Function to Add user with a Custom UUID
    public User doAddUserWithCustomID(String UUID, String userName, Object credential, String[] roleList, Map<String, String> claims,
                        String profileName, boolean requirePasswordChange) throws UserStoreException {
                            
                             // Assign the Custom UUID
                            String userID = UUID;
                            
                            // Assign username to the username claim.
                            claims = addUserNameAttribute(userName, claims);
                    
                            // Assign userID to the userid claim.

                    
                            claims = addUserIDAttribute(userID, claims);
                            persistUser(userID, userName, credential, roleList, claims, profileName, requirePasswordChange);
            
                    return getUser(userID, userName);

    }


}
