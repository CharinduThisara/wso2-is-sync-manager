#!/bin/bash

# Install Azure-CLI

sudo apt-get update
sudo apt-get install ca-certificates curl apt-transport-https lsb-release gnupg

sudo mkdir -p /etc/apt/keyrings
curl -sLS https://packages.microsoft.com/keys/microsoft.asc |
    gpg --dearmor |
    sudo tee /etc/apt/keyrings/microsoft.gpg > /dev/null
sudo chmod go+r /etc/apt/keyrings/microsoft.gpg

AZ_DIST=$(lsb_release -cs)
echo "deb [arch=`dpkg --print-architecture` signed-by=/etc/apt/keyrings/microsoft.gpg] https://packages.microsoft.com/repos/azure-cli/ $AZ_DIST main" |
    sudo tee /etc/apt/sources.list.d/azure-cli.list -y

sudo apt-get update
sudo apt-get install azure-cli

# Install kubectl 

sudo apt-get update

sudo apt-get install -y apt-transport-https ca-certificates curl

curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.29/deb/Release.key | sudo gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg -y

echo 'deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/v1.29/deb/ /' | sudo tee /etc/apt/sources.list.d/kubernetes.list -y

sudo apt-get update
sudo apt-get install -y kubectl

# Install Helm

curl https://baltocdn.com/helm/signing.asc | gpg --dearmor | sudo tee /usr/share/keyrings/helm.gpg > /dev/null
sudo apt-get install apt-transport-https --yes
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/helm.gpg] https://baltocdn.com/helm/stable/debian/ all main" | sudo tee /etc/apt/sources.list.d/helm-stable-debian.list -y
sudo apt-get update
sudo apt-get install helm

# Login to Azure
az login --use-device-code
az account set --subscription $AZURE_SUBSCRIPTION_ID
az aks get-credentials --resource-group $AZURE_RESOURCE_GROUP --name $AKS_NAME --overwrite-existing

kubectl apply -f ./Service_Account/is-role.yaml
kubectl apply -f ./Service_Account/service-account.yaml
kubectl apply -f ./Service_Account/role-binding.yaml

helm install wso2is .

cd ..   