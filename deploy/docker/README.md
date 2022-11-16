# ByteChef Docker Images

The ByteChef Docker images are built with all the components required for it to run, within a single Docker container. Files in this directory make up for the scripts and template files needed for building the image.

[//]: # (You may choose to use the ByteChef cloud instance &#40;at [app.bytechef.io]&#40;https://app.bytechef.io&#41;&#41; or start your own using this image.)

[//]: # ()
[//]: # (## ByteChef Cloud)

[//]: # ()
[//]: # (The fastest way to get started with ByteChef is using our cloud-hosted version. It's as easy as)

[//]: # ()
[//]: # (1. [Create an Account]&#40;https://app.bytechef.io/user/signup&#41;)

## Self Host

The following subsections describe how you can get started with one of _your own_ instances of ByteChef.

### Prerequisites

Ensure `docker` and `docker-compose` are installed and available for starting containers:

- Install Docker: [https://docs.docker.com/engine/install/](https://docs.docker.com/engine/install/)
- Install Docker Compose: [https://docs.docker.com/compose/install/](https://docs.docker.com/compose/install/)

You may verify the installation by running `docker --version` and `docker-compose --version`. The output should roughly be something like below:

```bash
$ docker --version
Docker version 20.10.7, build f0df350
$ docker-compose --version
docker-compose version 1.29.2, build 5becea4c
```

## Deployed from the Docker registry

TODO

## Built from the source code on the local machine

Build and start locally built server instance.

### 2. Commands

```bash
docker compose -f docker-compose.dev.server.yml up -d
```

Stop locally built server instance.

```bash
docker compose -f docker-compose.dev.server.yml down
```

Rebuild a docker image of the locally built server instance.

```bash
docker compose -f docker-compose.dev.server.yml down --rmi local
docker compose -f docker-compose.dev.server.yml up -d
```

## Local Development infrastructure

Use `docker-compose.dev.infra.yml` for required infrastructure(Postgres, Redis, RabbitMQ) when doing backend development:   

```bash
docker compose -f docker-compose.dev.infra.yml up
```

Use `docker-compose.dev.consul.yml` for running local Consul instance when running the server side as a services 
and using Consul for the service discovery:

```bash
docker compose -f docker-compose.dev.consul.yml up
```
