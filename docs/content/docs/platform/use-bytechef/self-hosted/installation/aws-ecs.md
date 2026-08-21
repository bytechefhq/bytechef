---
title: AWS ECS
description: Learn how to deploy ByteChef on AWS ECS
---

# AWS ECS

Amazon ECS runs the ByteChef container image, `docker.bytechef.io/bytechef/bytechef:latest`, as a managed service — a good fit when you want AWS to handle scheduling, health, and rolling deployments rather than managing an EC2 host yourself. This guide targets **Fargate** (serverless), but the same task definition works on EC2-backed ECS.

## Prerequisites

- An ECS cluster (Fargate requires no EC2 hosts).
- An **Amazon RDS for PostgreSQL 15+** database reachable from the cluster's subnets/security groups.
- An Application Load Balancer (recommended) to terminate TLS and health-check the service.
- AWS Secrets Manager (or SSM Parameter Store) for the sensitive values.

## 1. Store secrets

Put the sensitive configuration in Secrets Manager so it is injected into the container rather than baked into the task definition: the database password, `BYTECHEF_ENCRYPTION_PROPERTY_KEY`, and `BYTECHEF_SECURITY_REMEMBER_ME_KEY`. Use **stable** values for the encryption and remember-me keys so credentials and sessions survive task replacement.

## 2. Create the task definition

Define a single container that runs the image and exposes port `8080`:

```json
{
  "family": "bytechef",
  "requiresCompatibilities": ["FARGATE"],
  "networkMode": "awsvpc",
  "cpu": "1024",
  "memory": "2048",
  "containerDefinitions": [
    {
      "name": "bytechef",
      "image": "docker.bytechef.io/bytechef/bytechef:latest",
      "portMappings": [{ "containerPort": 8080, "protocol": "tcp" }],
      "environment": [
        { "name": "BYTECHEF_DATASOURCE_URL", "value": "jdbc:postgresql://<rds-endpoint>:5432/bytechef" },
        { "name": "BYTECHEF_DATASOURCE_USERNAME", "value": "bytechef" },
        { "name": "BYTECHEF_ENCRYPTION_PROVIDER", "value": "property" },
        { "name": "BYTECHEF_PUBLIC_URL", "value": "https://bytechef.example.com" }
      ],
      "secrets": [
        { "name": "BYTECHEF_DATASOURCE_PASSWORD", "valueFrom": "<secret-arn>:password::" },
        { "name": "BYTECHEF_ENCRYPTION_PROPERTY_KEY", "valueFrom": "<secret-arn>:encryptionKey::" },
        { "name": "BYTECHEF_SECURITY_REMEMBER_ME_KEY", "valueFrom": "<secret-arn>:rememberMeKey::" }
      ]
    }
  ]
}
```

Adjust CPU/memory to your workload — 1 vCPU / 2 GB is a reasonable starting point.

## 3. Create the service behind a load balancer

Create an ECS service from the task definition and attach it to an ALB target group:

- **Target port:** `8080`.
- **Health check path:** `/actuator/health/readiness`.
- Set `BYTECHEF_PUBLIC_URL` to the ALB's public HTTPS URL so webhook and OAuth2 redirect URLs resolve correctly.

Because schema migrations run at container startup, keep the service at a single task during the first deploy and any upgrade, then scale out once the schema is current. When running multiple tasks, review the multi-instance settings in [Running multiple instances](/platform/use-bytechef/self-hosted/configuration#running-multiple-instances) — a shared message broker and cache, and a shared encryption key.

## Applying it from the CLI

The steps above describe what to create; these are the `aws` commands that create it. They assume
the AWS CLI is installed and configured, and that the secrets from step 1 already exist.

Create the cluster (Fargate needs no EC2 hosts):

```bash
aws ecs create-cluster --cluster-name bytechef-cluster
```

Register the task definition from step 2. Keep it in a file rather than passing it inline — the
`secrets` block is easier to get right, and the file is worth committing:

```bash
aws ecs register-task-definition --cli-input-json file://bytechef-task-definition.json
```

Add an `executionRoleArn` to that file pointing at a task execution role. Fargate uses it to pull
the image and to read the values in the `secrets` block; without it the task fails to start and the
`secrets` entries never resolve.

Create the service, attaching it to the ALB target group from step 3:

```bash
aws ecs create-service \
  --cluster bytechef-cluster \
  --service-name bytechef \
  --task-definition bytechef \
  --desired-count 1 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[<subnet-id>],securityGroups=[<sg-id>]}" \
  --load-balancers "targetGroupArn=<target-group-arn>,containerName=bytechef,containerPort=8080"
```

Keep `--desired-count 1` for the first deploy, since schema migrations run at container startup.

To watch the rollout:

```bash
aws ecs describe-services --cluster bytechef-cluster --services bytechef
```

### Upgrading

An upgrade is a new task definition revision plus a service update. Change the `image` tag in your
task definition file, then:

```bash
aws ecs register-task-definition --cli-input-json file://bytechef-task-definition.json
aws ecs update-service --cluster bytechef-cluster --service bytechef --task-definition bytechef
```

Naming the family without a revision (`--task-definition bytechef`) picks up the revision you just
registered. Pin an explicit revision (`bytechef:42`) to roll back to a known one. Use
`--force-new-deployment` only when the tag has not changed and you want ECS to re-pull `:latest` —
with a changed revision it is redundant.

Scale the service back to one task before an upgrade that carries schema changes, so a single
container owns the migration. See [Upgrades](/platform/use-bytechef/self-hosted/management/upgrades).

### Do not run PostgreSQL as a Fargate task

It is tempting to add a second service running the `postgres` image next to ByteChef. Do not:
Fargate task storage is ephemeral, so the database is destroyed whenever the task is replaced —
by a deployment, a scale event, or a health-check failure. Use RDS, as in the prerequisites.

## 4. Access the instance

Browse to the ALB URL and click **Create account** to register the first user.

<!-- TODO screenshot: the ECS service showing a healthy running task, or the first-login screen reached through the ALB URL -->

## Storage note

Fargate tasks have ephemeral local storage, and file storage defaults to **`filesystem`** — so you must change it, not leave it alone. Set `BYTECHEF_FILE_STORAGE_PROVIDER=jdbc` to keep files in the database, or `BYTECHEF_FILE_STORAGE_PROVIDER=aws` with `BYTECHEF_CLOUD_PROVIDER=aws` and `BYTECHEF_FILE_STORAGE_AWS_BUCKET` to store them in S3. Write `aws` in lowercase — the S3 client is enabled by a case-sensitive comparison against that literal, and `AWS` silently leaves it off. See [File storage](/platform/use-bytechef/self-hosted/configuration/file-storage).

