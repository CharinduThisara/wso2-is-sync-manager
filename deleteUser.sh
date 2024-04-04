#!/bin/bash

# Function to delete user by ID
delete_user() {
    local user_id=$(echo "$1" | awk '{print $3}')
    local url="https://wso2-is-east.eastus2.cloudapp.azure.com/scim2/Users/$user_id"

    echo "$url"
    echo "Deleting user with ID: $user_id..."

    # Send DELETE request to delete user
    curl -k -X DELETE \
    -u admin:admin \
    "$url" \
    -H 'accept: */*'
}


echo "Monitoring user_id.txt for new user IDs..."

# Infinite loop to continuously monitor user_id.txt for changes
while true; do
    # Check if user_id.txt file exists
    if [ -f "user_id.txt" ]; then
        # Read user IDs from user_id.txt and delete users
        while IFS= read -r user_id; do
            delete_user "$user_id"
        done < "user_id.txt"
    else
        echo "user_id.txt file not found!"
    fi
    # Wait for 60 seconds before checking for changes again
    sleep 60
done
