---
description: "ByteChef AWS ECS usage guide. Deploy and manage ByteChef using Amazon Elastic Container Service."
title: AWS ECS
---

[AWS ECS](https://aws.amazon.com/ecs/) is a container orchestration tool from AWS you can use to run ByteChef in self-hosted cloud environments. It provides automatic scaling, integrated load balancing, and simplified container deployment while maintaining full control over your ByteChef instance.

ByteChef deploys on AWS ECS using [ByteChef's Docker](https://hub.docker.com/r/bytechef/bytechef) image with [AWS Fargate](https://aws.amazon.com/fargate/) (serverless) or EC2 launch types. This guide uses Fargate for simplified infrastructure management.

## Prerequisites

Ensure you have the following:

* An AWS account with ECS access permissions
* [AWS CLI](https://aws.amazon.com/cli/) installed and configured on your system
* Basic knowledge of AWS Console and container concepts

## Getting Started with ByteChef

- Download the ECS task definitions and deploy using AWS CLI:


```shell
curl -O https://raw.githubusercontent.com/bytechefhq/bytechef/master/ecs/postgres-task.json
curl -O https://raw.githubusercontent.com/bytechefhq/bytechef/master/ecs/bytechef-task.json
```

- Create the ECS cluster:

```shell
aws ecs create-cluster --cluster-name bytechef-cluster
```

- Register the task definitions:

```shell
aws ecs register-task-definition --cli-input-json file://postgres-task.json
aws ecs register-task-definition --cli-input-json file://bytechef-task.json
```

- Create the ECS services (replace subnet and security group IDs):

```shell
aws ecs create-service \
  --cluster bytechef-cluster \
  --service-name postgres \
  --task-definition bytechef-postgres \
  --desired-count 1 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxx],securityGroups=[sg-xxx]}"

aws ecs create-service \
  --cluster bytechef-cluster \
  --service-name bytechef \
  --task-definition bytechef-app \
  --desired-count 1 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxx],securityGroups=[sg-xxx],assignPublicIp=ENABLED}"
```

This workflow accomplishes the following:

* Creates an ECS cluster for ByteChef services
* Deploys PostgreSQL as a persistent service for data retention
* Launches the ByteChef application with port `8080` exposed
* Configures Fargate tasks with 2 vCPU and 4 GB RAM for stable operation
* Assigns public IP to ByteChef for external access

After the services start, find the ByteChef task's public IP in the ECS Console and access ByteChef at: `http://PUBLIC_IP:8080`

## PostgreSQL Configuration

ByteChef stores all credentials, workflows, and run history in PostgreSQL. The default ECS deployment includes a PostgreSQL container.

> **Warning**:
> Ensure proper EFS or EBS volumes are configured for PostgreSQL persistence. Losing your task storage will result in data loss. For production, it's best to use AWS RDS for PostgreSQL instead of containerized databases.

To connect to an external PostgreSQL server (such as AWS RDS), update the ByteChef task definition:

```json
{
  "environment": [
    {
      "name": "BYTECHEF_DATASOURCE_URL",
      "value": "jdbc:postgresql://your-rds-endpoint:5432/bytechef"
    },
    {
      "name": "BYTECHEF_DATASOURCE_USERNAME",
      "value": "postgres"
    },
    {
      "name": "BYTECHEF_DATASOURCE_PASSWORD",
      "value": "your_password"
    }
  ]
}
```

Full ECS deployment examples for PostgreSQL are in the [ByteChef GitHub repository](https://github.com/bytechefhq/bytechef).

## Upgrading ByteChef

To deploy a newer version, update the task definition and force a new deployment:

```shell
aws ecs register-task-definition --cli-input-json file://bytechef-task.json

aws ecs update-service \
  --cluster bytechef-cluster \
  --service bytechef \
  --task-definition bytechef-app \
  --force-new-deployment
```

This upgrade process:

* Updates the task definition with the new image tag
* Forces a new deployment with the updated task
* Ensures the latest version is running

### Upgrading with Task Definition Revisions

When managing ByteChef via ECS task definitions:

```shell
aws ecs register-task-definition --cli-input-json file://bytechef-task.json

aws ecs update-service \
  --cluster bytechef-cluster \
  --service bytechef \
  --task-definition bytechef-app:NEW_REVISION

aws ecs describe-services --cluster bytechef-cluster --services bytechef
```

This revision management process:

* Registers a new task definition revision
* Updates the service to use the new revision
* Monitors the deployment status

## Next Steps

* Review [ByteChef configuration options](/self-hosting/configuration/environment-variables)
* Check out the [Kubernetes setup](/self-hosting/deployment/kubernetes)
* Build your [initial automation](/automation)