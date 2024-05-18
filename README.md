# WSO2 Identity Server Deployment with User Sync Agents for Active/Active user synchronization using Azure Cosmos DB.

### This project contains the Java codebase for the Sync Agents, along with build and deployment scripts. These components are part of our effort to implement an Active/Active Identity Server setup across two Azure regions using Azure Kubernetes Service (AKS) and Azure Cosmos DB.

## How to use the Sync. Agents

### Prerequisites:

1. #### You need an Azure Subscription in order to create the following resources.
   - Two Private Azure Kubernetes Clusters in two paired regions.[(Learn about Azure Paired regions)](https://learn.microsoft.com/en-us/azure/reliability/cross-region-replication-azure)
   - Two Vitual Machines attached to the same network of Each Kubernetes Cluster to access the clusters from local machine using SSH.
   - Azure Cosmos DB Account with two write regions enabled.[(Learn how to create one)](https://learn.microsoft.com/en-us/azure/cosmos-db/nosql/quickstart-portal)
   - Two MSSQL Databases in Azure(this is optional because you can use the default H2 Database as well)
2. #### For Building the Agents using Maven.
   - Java Development Kit (JDK) 8.
   - Apache Maven <mark>3.6.3</mark> installed.
   - WSO2 Identity Server 7.0.0 pack.(unzip it to the root of the project)
     ```bash
     unzip wso2is-7.0.0.zip
     ```
   - Proper permissions to access and modify files in the WSO2 Identity Server 7.0.0 pack.
3. #### For Deploying the Agents.
   > For this We used Azure Container Registries. But you may use Any container registry which can be accessed by the AKS Clusters.
   - Docker installed in your local Machine
   - Be logged into your preferred remote container registy.
     - For dockerhub usage
       ```bash
       docker login
       ```
     - For Azure Container registries[(see in detail)](https://learn.microsoft.com/en-us/azure/container-registry/container-registry-authentication?tabs=azure-cli)
       ```bash
       az login # you have to install AZURE CLI for this
       docker login <registry_name>.azurecr.io
       ```

### How to Use the scripts

1. #### Building
    - Edit the following parameters in **Build.sh**.
        - JAVA_HOME_PATH (path for java 8)
        - IS_HOME_PATH (path of the unzipped IS pack - eg: /home/user/Code_Bases/wso2-is-sync-manager/wso2is-7.0.0)
    - run the Build script.
        ```bash
        cd build && ./Build.sh
        ```
2. #### Deploying
    - Create the Docker image and push it to the container registry.
        ```bash
        cd build/docker && ./dockerBuildnPush
        ```
    - Establish a SSH connections to the VMs attached to the AKS virtual Networks.
    - Clone this repo in to the VM.
    - Run the Script
        ```bash
        # Set environment variables
        export AZURE_SUBSCRIPTION_ID="your_subscription_id"
        export AZURE_RESOURCE_GROUP="your_resource_group"
        export AKS_NAME="your_aks_name"
        cd deploy && ./deploy.sh
        ```