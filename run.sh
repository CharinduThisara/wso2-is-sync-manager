home_path=wso2is-7.0.0 # Please Define the home_path variable to the path of the WSO2 IS 7.0.0

echo "Copying the .env configuration files to the WSO2 IS 7.0.0"
rm $home_path/.env
cp .env $home_path/

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64  # please change the JAVA_HOME path according to your java installation
sh $home_path/bin/wso2server.sh