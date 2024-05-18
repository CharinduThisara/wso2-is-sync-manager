ACR_NAME=acrasgardeomainrnd001
IMAGE_NAME=is7.0
TAG=active

docker build -t $IMAGE_NAME:$TAG ../..

docker tag is7.0:agent1 $ACR_NAME.azurecr.io/$IMAGE_NAME:$TAG

docker push $ACR_NAME.azurecr.io/$IMAGE_NAME:$TAG