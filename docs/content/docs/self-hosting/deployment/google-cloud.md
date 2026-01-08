---
description: "ByteChef on Google Cloud. Deploy ByteChef on Google Cloud Platform with comprehensive guides."
<<<<<<< HEAD
title: Google Cloud Platform
---

__Google Cloud Platform (GCP)__ is a self-hosted instance for running ByteChef in cloud-hosted self-managed environments. It provides scalable compute capacity, reliable infrastructure, and flexible deployment options while maintaining full control over your ByteChef instance.

Just like the other instances, ByteChef also supports deployment on GCP using __Docker__ and __Docker Compose__. This guide covers the Compute Engine instance setup, security configuration, and ByteChef deployment steps.

> **Warning**
> 
>ByteChef requires at least **4 GB RAM (e2-medium instance)** for stable operation. Smaller instances will crash frequently.

## Prerequisites

* Google Cloud account with Compute Engine access
* SSH key pair for instance access
* Basic familiarity with Google Cloud Console

## Quick Start

<Steps>

### Launch Compute Engine Instance

1. Go to [Google Cloud Console](https://console.cloud.google.com/compute/instances)
2. Click __"Create Instance"__
3. Configure:
   - __Name:__ bytechef-server
   - __Region:__ Choose closest to your users (e.g., us-central1)
   - __Machine type:__ e2-medium (4 GB RAM minimum)
   - __Boot disk:__ Ubuntu 22.04 LTS
   - __Boot disk size:__ 30 GB
   - __Firewall:__ Allow HTTP traffic, Allow HTTPS traffic

### Configure Firewall Rules

1. Click __"Set up firewall rules"__ or navigate to VPC Network → Firewall
2. Create rule for ByteChef:
   - __Name:__ allow-bytechef
   - __Targets:__ All instances in the network
   - __Source IP ranges:__ 0.0.0.0/0
   - __Protocols and ports:__ tcp:8080

### Connect to Instance

```bash
# Using gcloud CLI
gcloud compute ssh bytechef-server --zone=us-central1-a

# Or use SSH in browser (click "SSH" button in console)
```

### Install Docker

```bash
# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

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

Visit: `http://EXTERNAL_IP:8080`

Find your External IP in the Google Cloud Console under VM instances.

</Steps>

## Instance Requirements

| Use Case | Machine Type | RAM | Monthly Cost |
|----------|--------------|-----|--------------|
| Testing/Development | e2-medium | 4 GB | ~$25 |
| Production | e2-standard-2 | 8 GB | ~$50 |
| Heavy Workloads | e2-standard-4 | 16 GB | ~$100 |

<Callout type="error">
Do not use e2-micro, e2-small, or f1-micro. These instances have insufficient memory and will crash.
</Callout>

## Managing ByteChef

For detailed Docker commands and configuration, refer to the __[Docker documentation](/self-hosting/deployment/local-docker)__.


## Production Setup

### Reserve Static IP

Prevent IP changes when stopping/restarting:

1. VPC Network → IP Addresses → Reserve External Static Address
2. Attach to your ByteChef instance

### Configure SSL (Optional)

For HTTPS access:

```bash
# Install Nginx and Certbot
sudo apt install nginx certbot python3-certbot-nginx -y

# Get SSL certificate
sudo certbot --nginx -d your-domain.com
```

## Next Steps

* Review __[Docker configuration](/self-hosting/deployment/local-docker)__ for detailed Docker setup
* Configure __[PostgreSQL](/self-hosting/deployment/local-docker#postgresql-configuration)__ for production use
* Explore __[Kubernetes](/self-hosting/deployment/kubernetes)__ for high availability deployments
=======
title: Google Cloud
---

Documentation for deploying on Google Cloud Platform is coming soon. Please check back later!
>>>>>>> 002576c3f5 (docs: Update Docker deployment documentation and clean up files)
