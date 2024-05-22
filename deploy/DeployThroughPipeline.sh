#!/bin/bash

# Prerequisites
# Install Azure CLI
# Install Kubeclt
# Install Helm

az account set --subscription $AZURE_SUBSCRIPTION_ID
az aks get-credentials --resource-group $AZURE_RESOURCE_GROUP --name $AKS_NAME --overwrite-existing

kubectl apply -f ./Service_Account/is-role.yaml
kubectl apply -f ./Service_Account/service-account.yaml
kubectl apply -f ./Service_Account/role-binding.yaml

helm install wso2is .

cd ..   