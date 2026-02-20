---
description: "ByteChef on DigitalOcean. Deploy and manage ByteChef applications on DigitalOcean Droplets."
title: DigitalOcean
---

__DigitalOcean__ is a self-hosted instance for running ByteChef in cloud-hosted self-hosted environments. It provides simple, developer-friendly infrastructure with transparent pricing and straightforward deployment while maintaining full control over your ByteChef instance.

ByteChef supports deployment on DigitalOcean using __Docker__. This guide covers Droplet setup, firewall configuration, and ByteChef deployment steps.

> **Warning**
> 
>ByteChef requires at least **4 GB RAM** for stable operation. Smaller Droplets will crash frequently.

## Prerequisites

* A DigitalOcean account
* An SSH key added to your DigitalOcean account
* Basic knowledge of DigitalOcean's Control Panel

## Quick Start

<Steps>

### Create Droplet

1. Go to [DigitalOcean Control Panel](https://cloud.digitalocean.com)
2. Click __"Create"__ → __"Droplets"__
3. Configure:
   - __Image:__ Ubuntu 22.04 LTS
   - __Droplet size:__ Basic plan
   - __CPU options:__ Regular (4 GB RAM / 2 vCPUs minimum)
   - __Datacenter region:__ Choose closest to your users
   - __Authentication:__ SSH keys (select your key)
   - __Hostname:__ bytechef-server
4. Click __"Create Droplet"__

### Configure Firewall

1. In DigitalOcean Control Panel, go to __Networking__ → __Firewalls__
2. Click __"Create Firewall"__
3. Configure:
   - __Name:__ bytechef-firewall
   - __Inbound Rules:__
     - SSH: TCP, Port 22, All IPv4, All IPv6
     - HTTP: TCP, Port 80, All IPv4, All IPv6
     - HTTPS: TCP, Port 443, All IPv4, All IPv6
     - Custom: TCP, Port 8080, All IPv4, All IPv6
   - __Apply to Droplets:__ Select bytechef-server
4. Click __"Create Firewall"__

### Connect to Droplet

- Run this command to connect via SSH

```bash
ssh root@YOUR_DROPLET_IP
```

### Install Docker

- Install Docker

```bash
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
```

- Install Docker Compose

```bash
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

- Enable Docker service

```bash
sudo systemctl enable docker
sudo systemctl start docker
```

### Deploy ByteChef

- Download configuration

```bash
curl -O https://raw.githubusercontent.com/bytechefhq/bytechef/master/docker-compose.yml
```

- Start ByteChef

```bash
docker compose up -d
```

### Access ByteChef

Visit: `http://YOUR_DROPLET_IP:8080`

</Steps>

## Droplet Requirements

| Use Case | Size | RAM | Monthly Cost |
|----------|------|-----|--------------|
| Testing/Development | Basic (4 GB) | 4 GB | ~$24 |
| Production | Basic (8 GB) | 8 GB | ~$48 |

<Callout type="error">
Do not use Droplets with less than 4 GB RAM. These instances have insufficient memory and will crash.
</Callout>

## Managing ByteChef

For detailed Docker commands and configuration, refer to the __[Docker documentation](/self-hosting/deployment/local-docker)__.

### Reserve Floating IP

To prevent IP changes when recreating Droplets, follow these steps:

1. DigitalOcean Control Panel → __Networking__ → __Floating IPs__
2. Click __"Assign Floating IP"__
3. Select your bytechef-server Droplet
4. Click __"Assign Floating IP"__

## Next Steps

* Review __[Docker configuration](/self-hosting/deployment/local-docker)__ for detailed Docker setup
* Configure __[PostgreSQL](/self-hosting/deployment/local-docker#postgresql-configuration)__ for production use
* Explore __[Kubernetes](/self-hosting/deployment/kubernetes)__ for high availability deployments