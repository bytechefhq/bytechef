---
description: "ByteChef on AWS EC2. Deploy and manage ByteChef applications on Amazon EC2 instances."
title: AWS EC2
---

<<<<<<< HEAD
__AWS EC2__ is a self-hosted instance for running ByteChef in cloud-hosted self-managed environments. It provides scalable compute capacity, reliable infrastructure, and flexible deployment options while maintaining full control over your ByteChef instance.

ByteChef supports deployment on EC2 using the [Docker](/self-hosting/deployment/local-docker) self-hosted instance. This guide covers EC2 instance setup, security configuration, and ByteChef deployment steps.

> **Warning**
> 
>ByteChef requires at least **4 GB RAM (t3.medium instance)** for stable operation. Smaller instances will crash frequently.

## Prerequisites

* AWS account with EC2 access
* SSH key pair for instance access
* Basic familiarity with AWS Console

## Quick Start

<Steps>

### Launch EC2 Instance

1. Go to [AWS EC2 Console](https://console.aws.amazon.com/ec2/)
2. Click __"Launch Instance"__
3. Configure:
   - __Name:__ bytechef-server
   - __AMI:__ Ubuntu Server 22.04 LTS
   - __Instance type:__ t3.medium (minimum 4 GB RAM)
   - __Key pair:__ Create new or select existing
   - __Security group:__ Allow ports 22 (SSH), 80 (HTTP), 443 (HTTPS), 8080 (ByteChef)
   - __Storage:__ 30 GB gp3

### Connect to Instance

```bash
# Set permissions
chmod 400 your-key.pem

# Connect via SSH
ssh -i your-key.pem ubuntu@YOUR_EC2_IP
```

### Install Docker

```bash
# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker ubuntu

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Log out and back in for permissions
exit
```

### Deploy ByteChef

Reconnect via SSH, then:

```bash
# Download configuration
curl -O https://raw.githubusercontent.com/bytechefhq/bytechef/master/docker-compose.yml

# Start ByteChef
docker compose up -d
```

### Access ByteChef

Visit: `http://YOUR_EC2_IP:8080`

</Steps>

## Instance Requirements

| Use Case | Instance Type | RAM | Monthly Cost |
|----------|---------------|-----|--------------|
| Testing/Development | t3.medium | 4 GB | ~$30 |
| Production | t3.large | 8 GB | ~$60 |


Do not use `t2.micro`, `t3.micro`, or `t3.small`. These instances have insufficient memory and will crash.

## Managing ByteChef

For detailed Docker commands and configuration, refer to the __[Docker documentation](/self-hosting/deployment/local-docker)__.

## Production Setup

### Add Elastic IP

Prevent IP changes when stopping/restarting:

1. EC2 Console → Elastic IPs → Allocate
2. Associate with your ByteChef instance

### Configure SSL (Optional)

For HTTPS access:

```bash
# Install Nginx and Certbot
sudo apt install nginx certbot python3-certbot-nginx -y

# Get SSL certificate
sudo certbot --nginx -d your-domain.com
```

## Backup

Backup your PostgreSQL data regularly:

```bash
docker exec postgres pg_dump -U postgres bytechef > backup.sql
```


## Next Steps

* Review __[Docker configuration](/self-hosting/deployment/local-docker)__ for detailed Docker setup
* Configure __[PostgreSQL](/self-hosting/deployment/local-docker#postgresql-configuration)__ for production use
* Explore __[Kubernetes](/self-hosting/deployment/kubernetes)__ for high availability deployments
=======
Documentation for deploying on AWS EC2 is coming soon. Please check back later!
>>>>>>> 002576c3f5 (docs: Update Docker deployment documentation and clean up files)
