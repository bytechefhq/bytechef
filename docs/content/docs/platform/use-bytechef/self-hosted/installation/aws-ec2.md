---
title: AWS EC2
description: Learn how to deploy ByteChef on AWS EC2
---

# AWS EC2

ByteChef is distributed as a single container image, `docker.bytechef.io/bytechef/bytechef:latest`, listening on port `8080`. Running it on an EC2 instance is a matter of sizing the instance, installing Docker, provisioning a PostgreSQL database, and starting the container with the required configuration.

## Prerequisites

- An AWS account with EC2 access and an SSH key pair for reaching the instance.
- An EC2 instance running a Docker-capable Linux AMI, with **at least 4 GB of RAM** - see [Instance sizing](#instance-sizing) below.
- A **PostgreSQL 15+** database. Use Amazon RDS for PostgreSQL for production, or run PostgreSQL in a second container for a quick evaluation.
- A security group that allows inbound traffic to port `8080` (or `443` if you front it with a load balancer / reverse proxy) and outbound access to the database.

## Instance sizing

ByteChef runs a JVM alongside the workflow engine, so memory is the binding constraint rather than CPU.

| Use case | Instance type | RAM |
|---|---|---|
| Testing / development | `t3.medium` | 4 GB |
| Production | `t3.large` or larger | 8 GB+ |

Do not use `t2.micro`, `t3.micro`, or `t3.small`. They have insufficient memory and the container will be killed under load.

## 1. Launch the instance

From the [AWS EC2 Console](https://console.aws.amazon.com/ec2/), choose **Launch Instance** and configure:

- **Name:** `bytechef-server`
- **AMI:** Ubuntu Server 22.04 LTS (or Amazon Linux 2023)
- **Instance type:** `t3.medium` or larger, per the table above
- **Key pair:** create a new one or select an existing one
- **Security group:** allow inbound `22` (SSH), `8080` (ByteChef), and `80`/`443` if you terminate TLS on the instance
- **Storage:** 30 GB `gp3`

## 2. Connect to the instance

```bash
chmod 400 your-key.pem
ssh -i your-key.pem ubuntu@<instance-public-ip>
```

## 3. Install Docker

On Ubuntu:

```bash
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker ubuntu
```

On Amazon Linux:

```bash
sudo yum install -y docker
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user
```

Log out and back in so the group membership takes effect.

## 4. Provision the database

Create an RDS for PostgreSQL instance (or a database named `bytechef` on an existing server), and note the JDBC URL, username, and password. Ensure the EC2 instance's security group can reach the database port.

## 5. Run ByteChef

Start the container, pointing it at your database and supplying the essential secrets:

```bash
sudo docker run --name bytechef -d -p 8080:8080 --restart unless-stopped \
    --env BYTECHEF_DATASOURCE_URL=jdbc:postgresql://<rds-endpoint>:5432/bytechef \
    --env BYTECHEF_DATASOURCE_USERNAME=<db-user> \
    --env BYTECHEF_DATASOURCE_PASSWORD=<db-password> \
    --env BYTECHEF_ENCRYPTION_PROVIDER=property \
    --env BYTECHEF_ENCRYPTION_PROPERTY_KEY=<stable-encryption-key> \
    --env BYTECHEF_SECURITY_REMEMBER_ME_KEY=<stable-remember-me-key> \
    --env BYTECHEF_PUBLIC_URL=https://bytechef.example.com \
    docker.bytechef.io/bytechef/bytechef:latest
```

Schema migrations run automatically on first start. Set a stable `BYTECHEF_ENCRYPTION_PROPERTY_KEY` and `BYTECHEF_SECURITY_REMEMBER_ME_KEY` (see [Configuration](/platform/use-bytechef/self-hosted/configuration)) so credentials and sessions survive container replacement.

For a throwaway evaluation you can skip steps 4 and 5 and bring up ByteChef and PostgreSQL together with the repository's Compose file:

```bash
curl -O https://raw.githubusercontent.com/bytechefhq/bytechef/master/docker-compose.yml
docker compose up -d
```

That file ships with fixed default credentials and a fixed remember-me key, so use it for evaluation only - never for an internet-reachable instance.

## 6. Access the instance

Browse to `http://<instance-public-dns>:8080/login` (or your `BYTECHEF_PUBLIC_URL` behind a load balancer) and click **Create account** to register the first user.

<!-- TODO screenshot: the ByteChef login screen reached at the EC2 instance's public URL, with the "Create account" link visible -->

## Production notes

- **Elastic IP:** allocate an Elastic IP and associate it with the instance, so the address survives a stop/start cycle. Without one, stopping the instance changes its public IP and breaks any webhook URL pointing at it.
- **TLS and reverse proxy:** terminate HTTPS at an Application Load Balancer or a reverse proxy and forward to port `8080`. Set `BYTECHEF_PUBLIC_URL` to the public HTTPS URL so webhook and OAuth2 redirect URLs are correct. For a single instance, nginx with Certbot is enough:

  ```bash
  sudo apt install nginx certbot python3-certbot-nginx -y
  sudo certbot --nginx -d your-domain.com
  ```

- **Health checks:** configure the target group / load balancer health check to `GET /actuator/health/readiness` on port `8080`.
- **Persistence:** all state is in PostgreSQL except file entries, whose provider defaults to **`filesystem`** - mount an EBS volume for `BYTECHEF_FILE_STORAGE_FILESYSTEM_BASEDIR` if you keep that default, or set `BYTECHEF_FILE_STORAGE_PROVIDER=jdbc` to keep files in the database, or `aws` (lowercase, with `BYTECHEF_CLOUD_PROVIDER=aws`) to keep them in S3.
- **Restart policy:** the `--restart unless-stopped` flag above brings the container back after a reboot. Manage it with a systemd unit if you need more control.
- **Backups:** take regular PostgreSQL dumps. With RDS, use automated snapshots; with a containerized database from the Compose file above:

  ```bash
  docker exec postgres pg_dump -U postgres bytechef > backup.sql
  ```

  See [Upgrades and backups](/platform/use-bytechef/self-hosted/management/upgrades) for the full procedure.

## Next steps

- [Docker installation](/platform/use-bytechef/self-hosted/installation/local-docker) - the container options in detail
- [Kubernetes](/platform/use-bytechef/self-hosted/installation/kubernetes) - for high-availability deployments
