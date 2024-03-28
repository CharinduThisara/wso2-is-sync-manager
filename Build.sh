# Please Define the home_path variable to the path of the WSO2 IS 7.0.0
home_path=wso2is-7.0.0

cd SyncAgent-read
JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64 mvn clean install
cd ..

cd SyncAgent-write
JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64 mvn clean install
cd ..

echo "Copying the Agent jar files to the WSO2 IS 7.0.0"
rm $home_path/repository/components/dropins/com.sync.tool-1.0-SNAPSHOT.jar
cp SyncAgent-read/target/com.sync.tool-1.0-SNAPSHOT.jar $home_path/repository/components/dropins/

rm $home_path/repository/components/dropins/org.wso2.custom.user.operation.event.listener-1.0-SNAPSHOT.jar
cp SyncAgent-write/target/org.wso2.custom.user.operation.event.listener-1.0-SNAPSHOT.jar $home_path/repository/components/dropins/

echo "Copying the .env configuration files to the WSO2 IS 7.0.0"
rm $home_path/.env
cp .env $home_path/

echo "Copying the reference.conf configuration files to the WSO2 IS 7.0.0"
rm $home_path/repository/conf/reference.conf
cp config/reference.conf $home_path/repository/conf/

echo "Copying the deployment.toml configuration files to the WSO2 IS 7.0.0"
rm $home_path/repository/conf/deployment.toml
cp config/deployment.toml $home_path/repository/conf/

echo "Copying the libraries to the WSO2 IS 7.0.0"
cp lib/* $home_path/repository/components/lib/
cp dropins/* $home_path/repository/components/dropins/

echo
echo "Build Completed Successfully"
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
sh $home_path/bin/wso2server.sh
