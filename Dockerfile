FROM eclipse-temurin:11.0.21_9-jre-jammy

WORKDIR /home

# Copy the WSO2 instance directory to /app inside the container
COPY ./wso2is-7.0.0 /home/wso2carbon/wso2is-7.0.0

# Set necessary permissions for the script (if required)
# RUN chmod +x /app/wso2is-6.1.0-Instance-1/bin/wso2server.sh

# Expose ports
EXPOSE 4000 9763 9443

# Start WSO2 Carbon server
ENTRYPOINT ["/home/wso2carbon/wso2is-7.0.0/bin/wso2server.sh"]
