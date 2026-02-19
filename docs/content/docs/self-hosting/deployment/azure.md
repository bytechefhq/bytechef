---
description: "ByteChef on Azure. Deploy and manage ByteChef on Microsoft Azure cloud platform."
title: Azure
---

__Microsoft Azure__ is a self-hosted instance for running ByteChef. Just like other innstannces, it provides scalable compute capacity and flexible deployment while maintaining full control over your ByteChef instance.

ByteChef supports deployment on Azure using __Docker__. This guide covers Azure's self-hosted instance setup and security configuration.

> **Warning**
> 
>ByteChef requires at least **4 GB RAM (Standard_B2s instance)** for stable operation. Smaller instances will crash frequently.

## Prerequisites

* Azure account with Virtual Machines access
* SSH key pair for instance access
* Basic familiarity with Azure Portal

## Quick Start

<Steps>

### Launch Azure Virtual Machine

1. Go to [Azure Portal](https://portal.azure.com)
2. Navigate to __Virtual Machines__ and Click __"Create" → "Azure virtual machine"__
3. Configure:
   - __Virtual machine name:__ bytechef-server
   - __Image:__ Ubuntu Server 22.04 LTS
   - __Size:__ Standard_B2s (4 GB RAM minimum)
   - __Authentication type:__ SSH public key
   - __Username:__ azureuser
   - __SSH public key source:__ Generate new or use existing
   - __Public inbound ports:__ SSH (22), HTTP (80), HTTPS (443)
   - Click __"Review + create"__ → __"Create"__

### Add Firewall Rule for ByteChef Port

1. In your VM resource, go to __Networking__ → __Network settings__
2. Click __"Create port rule" → "Inbound port rule"__
3. Configure:
   - __Destination port ranges:__ 8080
   - __Protocol:__ TCP
   - __Name:__ Allow-ByteChef-8080
4. Click __"Add"__

### Connect to Instance

Set key permissions to ensure secure access:

```bash
chmod 400 ~/Downloads/bytechef-key.pem
```

Connect to your Azure VM using SSH:

```bash
ssh -i ~/Downloads/bytechef-key.pem azureuser@YOUR_PUBLIC_IP
```

### Install Docker

Download the Docker installation script:

```bash
curl -fsSL https://get.docker.com -o get-docker.sh
```

Run the Docker installation script:

```bash
sudo sh get-docker.sh
```

Add your user to the docker group to run Docker without sudo:

```bash
sudo usermod -aG docker azureuser
```

Download the latest version of Docker Compose:

```bash
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
```

Make Docker Compose executable:

```bash
sudo chmod +x /usr/local/bin/docker-compose
```

Log out to apply the new group permissions:

```bash
exit
```

Note: After logging out, you'll need to log back in for the docker group permissions to take effect.

### Deploy ByteChef

Reconnect via SSH, then:

Download the ByteChef docker-compose configuration file:

```bash
curl -O https://raw.githubusercontent.com/bytechefhq/bytechef/master/docker-compose.yml
```

Start the ByteChef containers in detached mode:

```bash
docker compose up -d
```

### Access ByteChef

Visit: `http://YOUR_PUBLIC_IP:8080`

</Steps>

## Instance Requirements

| Use Case | VM Size | RAM | Monthly Cost |
|----------|---------|-----|--------------|
| Testing/Development | Standard_B2s | 4 GB | ~$30 |
| Production | Standard_B2ms | 8 GB | ~$60 |

<Callout type="error">
Do not use Standard_B1s, Standard_B1ms, or any VM with less than 4 GB RAM. These instances have insufficient memory and will crash.
</Callout>

## Managing ByteChef

For detailed Docker commands and configuration, refer to the __[Docker documentation](/self-hosting/deployment/local-docker)__.

Common commands:

Common Docker Compose commands:

```bash
# Start
docker compose up -d

# Stop
docker compose down

# View logs
docker compose logs -f

# Restart
docker compose restart
```

## Production Setup

### Reserve Static IP

Prevent IP changes when stopping/restarting:

1. Azure Portal → __Public IP addresses__ → Find your VM's IP
2. Click on the IP address resource
3. Go to __Configuration__
4. Change __Assignment__ from __Dynamic__ to __Static__
5. Click __"Save"__

### Optional: Configure SSL 

To configure SSL for HTTPS access, follow these steps:

- Install Nginx and Certbot:

```bash
sudo apt install nginx certbot python3-certbot-nginx -y
```

- Get SSL certificate:
```bash
sudo certbot --nginx -d your-domain.com
```

## Next Steps

* Review __[Docker configuration](/self-hosting/deployment/local-docker)__ for detailed Docker setup
* Configure __[PostgreSQL](/self-hosting/deployment/local-docker#postgresql-configuration)__ for production use
* Explore __[Kubernetes](/self-hosting/deployment/kubernetes)__ for high availability deployments