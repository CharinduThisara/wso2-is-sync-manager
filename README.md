# WSO2 Identity Server Deployment Script

This script automates the deployment process for the WSO2 Identity Server by performing the following tasks:

1. Building SyncAgent-read and SyncAgent-write Maven projects.
2. Copying the built artifacts to the appropriate directories in the WSO2 Identity Server.
3. Updating configuration files with custom settings.
4. Starting the WSO2 Identity Server.

## Prerequisites

Before running this script, ensure that you have the following:

- WSO2 Identity Server 7.0.0 installed on your system.
- Java Development Kit (JDK) version 1.8 or later installed.
- Apache Maven installed.
- Proper permissions to access and modify files in the WSO2 Identity Server directory.

## Usage

1. Define the `home_path` variable at the beginning of the script to the path of your WSO2 Identity Server installation directory.

```bash
home_path='path/to/wso2is-7.0.0'
