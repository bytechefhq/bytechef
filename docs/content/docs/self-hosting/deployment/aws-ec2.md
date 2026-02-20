---
description: "ByteChef on AWS EC2. Deploy and manage ByteChef applications on Amazon EC2 instances."
title: AWS EC2
---

__AWS EC2__ is a self-hosted instance for running ByteChef in cloud-hosted self-managed environments. It provides scalable compute capacity, reliable infrastructure, and flexible deployment options while maintaining full control over your ByteChef instance.

ByteChef supports deployment on EC2 using the [Docker](/self-hosting/deployment/local-docker) self-hosted instance. This guide covers EC2 instance setup, security configuration, and ByteChef deployment steps.

## Prerequisites

* AWS account with EC2 access
* SSH key pair for instance access
* Basic familiarity with AWS Console

## Quick Start

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

### Connect and Deploy

1. SSH into your instance:
```shell
ssh -i your-key.pem ubuntu@your-instance-ip
```

2. Install Docker:
```shell
sudo apt update
sudo apt install docker.io -y
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ubuntu
```

3. Deploy ByteChef:
```shell
docker run -d \
  --name bytechef \
  -p 8080:8080 \
  -e BYTECHEF_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bytechef \
  -e BYTECHEF_DATASOURCE_USERNAME=postgres \
  -e BYTECHEF_DATASOURCE_PASSWORD=postgres \
  docker.bytechef.io/bytechef/bytechef:latest
```

4. Access ByteChef at: `http://your-instance-ip:8080`

## Next Steps

* Review [ByteChef configuration options](/self-hosting/configuration/environment-variables)
* Check out [AWS ECS setup](/self-hosting/deployment/aws-ecs)
* Build your [initial automation](/automation)
