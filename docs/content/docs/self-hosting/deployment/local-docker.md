---
description: "ByteChef Docker Usage. Deploy and manage ByteChef using Docker containers in your environment."
title: Docker
---

[Docker](https://www.docker.com/) is a recommended approach for running ByteChef in self-hosted environments. It delivers an isolated runtime, prevents software conflicts across systems, and streamlines database and configuration handling.

ByteChef also supports [Docker Compose](/deployment/docker-compose) for multi-container setups. Docker Compose examples are available in the [ByteChef GitHub repository](https://github.com/bytechefhq/bytechef).

## Prerequisites

Ensure Docker is installed on your system:

* [Docker Desktop](https://docs.docker.com/get-docker/) works on Mac, Windows, and Linux. It bundles Docker Engine with Docker Compose.
* For Linux systems, [Docker Engine](https://docs.docker.com/engine/install/) and Docker Compose can be installed separately. This approach suits headless servers or users preferring the command line.

## Getting Started with ByteChef

Execute the following commands in your terminal:

```shell
curl -O https://raw.githubusercontent.com/bytechefhq/bytechef/master/docker-compose.yml

docker compose up
```

This workflow accomplishes the following:

* Binds port `8080` from the container to your host system.
* Initializes a PostgreSQL instance for data retention.
* Performs initial database setup automatically.
* Attaches the `postgres_data` volume to `/var/lib/postgresql/data` so your data remains intact after container restarts.

After the container starts, access ByteChef at: [http://localhost:8080](http://localhost:8080)

## PostgreSQL Configuration

ByteChef stores all credentials, workflows, and run history in PostgreSQL. Docker Compose includes PostgreSQL out of the box.

> **Warning**
> 
> Ensure the `postgres_data` volume is properly configured. Loss of this volume will result in data loss. The `postgres_data` volume preserves your workflows, credentials, and execution records between restarts.

To connect to an external PostgreSQL server, update your `docker-compose.yml`:

```yaml
bytechef:
  environment:
    BYTECHEF_DATASOURCE_URL: jdbc:postgresql://your-host:5432/bytechef
    BYTECHEF_DATASOURCE_USERNAME: postgres
    BYTECHEF_DATASOURCE_PASSWORD: your_password
```

Full Docker Compose samples for PostgreSQL are in the [ByteChef GitHub repository](https://github.com/bytechefhq/bytechef/blob/master/docker-compose.yml).

## Upgrading ByteChef

To deploy a newer version, fetch the updated image and relaunch:

```shell
# Fetch the current version
docker pull docker.bytechef.io/bytechef/bytechef:latest

# Or get a specific release
docker pull docker.bytechef.io/bytechef/bytechef:1.0.0

# Shut down existing containers
docker compose down

# Launch with the new version
docker compose up -d
```

### Upgrading with Docker Compose

When managing ByteChef via Docker Compose:

```shell
# Go to your compose directory
cd /path/to/your/compose/file

# Download latest images
docker compose pull

# Halt and clean up running containers
docker compose down

# Restart with fresh images
docker compose up -d
```

## Next steps

* Review [ByteChef configuration options](/self-hosting/configuration/environment-variables)
* Check out [Kubernetes setup](/self-hosting/deployment/kubernetes)
* Build your [initial automation](/automation)