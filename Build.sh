# Please Define path of the WSO2 IS 7.0.0 pack relative to the place of execution of the script (or provide the absolute path)
IS_HOME_PATH=/home/charindut/IS/Code_Bases/wso2-is-sync-manager/wso2is-7.0.0

# Define the JAVA_HOME path according to your java 8 installation
JAVA_HOME_PATH=/usr/lib/jvm/java-1.8.0-openjdk-amd64

echo "Building the Agent jar files"

cd SyncAgent-read
JAVA_HOME=$JAVA_HOME_PATH mvn clean install
cd ..

cd SyncAgent-write
JAVA_HOME=$JAVA_HOME_PATH mvn clean install
cd ..

echo "Copying the Agent jar files to the WSO2 IS 7.0.0"
rm $IS_HOME_PATH/repository/components/dropins/com.sync.tool-1.0-SNAPSHOT.jar
cp SyncAgent-read/target/com.sync.tool-1.0-SNAPSHOT.jar $IS_HOME_PATH/repository/components/dropins/

rm $IS_HOME_PATH/repository/components/dropins/org.wso2.custom.user.operation.event.listener-1.0-SNAPSHOT.jar
cp SyncAgent-write/target/org.wso2.custom.user.operation.event.listener-1.0-SNAPSHOT.jar $IS_HOME_PATH/repository/components/dropins/

echo "Copying the reference.conf configuration files to the WSO2 IS 7.0.0"
rm $IS_HOME_PATH/repository/conf/reference.conf
cp config/reference.conf $IS_HOME_PATH/repository/conf/

echo "Copying the deployment.toml configuration files to the WSO2 IS 7.0.0"
rm $IS_HOME_PATH/repository/conf/deployment.toml
cp config/deployment.toml $IS_HOME_PATH/repository/conf/

echo "Copying the libraries to the WSO2 IS 7.0.0"
cp lib/* $IS_HOME_PATH/repository/components/lib/
cp dropins/* $IS_HOME_PATH/repository/components/dropins/

echo
echo "Build Completed Successfully"
