ACR_NAME=acrasgardeomainrnd001
IMAGE_NAME=is7.0
TAG=active

docker build -t $IMAGE_NAME:$TAG .

docker tag $IMAGE_NAME:$TAG $ACR_NAME.azurecr.io/$IMAGE_NAME:$TAG

docker push $ACR_NAME.azurecr.io/$IMAGE_NAME:$TAG