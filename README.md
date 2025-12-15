<div align="center">


[![License](https://img.shields.io/static/v1?label=license&message=Apache%20v2.0&color=brightgreen)](https://github.com/bytechefhq/bytechef/blob/master/LICENSE)
[![Docker Pulls](https://img.shields.io/docker/pulls/bytechef/bytechef)](https://hub.docker.com/r/bytechef/bytechef)
[![Build Status](https://github.com/bytechefhq/bytechef/actions/workflows/build_push.yml/badge.svg)](https://github.com/bytechefhq/bytechef/actions/workflows/build_push.yml)
[![Discord](https://img.shields.io/badge/Discord-Join%20Us-7389D8?logo=discord&logoColor=white)](https://discord.gg/VKvNxHjpYx)

[![API integration and workflow automation platform](https://raw.githubusercontent.com/bytechefhq/bytechef/master/static/bytechef_logo.png)](https://www.bytechef.io?utm_source=github&utm_medium=organic&utm_campaign=readme)

**Open-source, low-code, extendable API integration & workflow automation platform**

  [Documentation](https://docs.bytechef.io?utm_source=github&utm_medium=organic&utm_campaign=readme) · [Discord](https://discord.gg/VKvNxHjpYx) · [Connect on X](https://demo.bytechef.io)

</div>

## Installation

### Docker Compose (Fastest Setup)

**Requirement:** [Docker Desktop](https://www.docker.com/products/docker-desktop/)

This is the fastest way to start ByteChef. Download the `docker-compose.yml` file from the repository:
```bash
curl -O https://raw.githubusercontent.com/bytechefhq/bytechef/master/docker-compose.yml
docker compose -f docker-compose.yml up
```

Both PostgreSQL database and ByteChef containers will start automatically.

### Docker (Manual Setup)

If Docker Compose isn't supported in your environment, follow these steps:

#### 1. Create Docker Network
```bash
docker network create -d bridge bytechef_network
```

#### 2. Start PostgreSQL Container
```bash
docker run --name postgres -d -p 5432:5432 \
    --env POSTGRES_USER=postgres \
    --env POSTGRES_PASSWORD=postgres \
    --hostname postgres \
    --network bytechef_network \
    -v /opt/postgre/data:/var/lib/postgresql/data \
    postgres:15-alpine
```

#### 3. Start ByteChef Container
```bash
docker run --name bytechef -it -p 8080:8080 \
    --env BYTECHEF_DATASOURCE_URL=jdbc:postgresql://postgres:5432/bytechef \
    --env BYTECHEF_DATASOURCE_USERNAME=postgres \
    --env BYTECHEF_DATASOURCE_PASSWORD=postgres \
    --env BYTECHEF_SECURITY_REMEMBER_ME_KEY=e48612ba1fd46fa7089fe9f5085d8d164b53ffb2 \
    --network bytechef_network \
    docker.bytechef.io/bytechef/bytechef:latest
```

**Note:** Use `-d` flag instead of `-it` to run in detached mode.

## Features

* **Visual Workflow Editor**: Build and visualize workflows by dragging and dropping components
* **Event-Driven & Scheduled Workflows**: Automate with real-time event-driven workflows via simple trigger definitions
* **Multiple Flow Controls**: Use condition, switch, loop, each, parallel, and more
* **Built-In Code Editor**: Write workflow definitions in JSON and code blocks in Java, JavaScript, Python, and Ruby
* **200+ Built-In Components**: Extract data from any database, SaaS applications, internal APIs, or cloud storage
* **Extendable**: Develop custom connectors in Java, JavaScript, Python, or Ruby
* **AI Ready**: Built-in AI components for running multiple AI models and algorithms
* **Developer Ready**: Expose workflows as APIs; platform handles authentication
* **Version Control Friendly**: Push workflows to Git directly from ByteChef UI
* **Self-Hosted**: Install on-premise for complete control over execution and data
---

## Getting Started With ByteChef's Self-Hosted Instance

1. Start ByteChef with Docker Compose:
   
   ```bash
   curl -O https://raw.githubusercontent.com/bytechefhq/bytechef/master/docker-compose.yml
   docker compose -f docker-compose.yml up
   ```

2. Open <http://localhost:8080/login>

3. Click **Create Account** to set up your user

4. Sign in with your credentials

## What is ByteChef?

ByteChef is an **open-source, low-code, extendable** API integration and workflow automation platform that helps you:

**As an automation solution** - Integrate and build automation workflows across your SaaS apps, internal APIs, and databases.

**As an embedded solution** - Build integrations directly into your SaaS product, allowing your customers to connect applications they use with your product.


## Creating your first workflow

### Using the UI Editor

1. Navigate to the **Projects** section
2. Click **New Project**
3. Fill the necessary credentials
4. Create a new workflow in the project
5. Add a trigger and components you want to work with
6. Connect them to define your workflow logic
7. Configure each component's parameters in the properties panel
8. Test your workflow
9. Deploy

## Contributing

ByteChef is Open Source under the [Apache License v2.0](https://github.com/bytechefhq/bytechef/blob/master/LICENSE). If you would like to contribute to the software, read the [contributing guide](https://github.com/bytechefhq/bytechef/blob/master/CONTRIBUTING.md) to get started.

## Roadmap

Check out our [public roadmap](https://github.com/orgs/bytechefhq/projects/3) to see what we're working on next.

---

## License

ByteChef is released under the [Apache License v2.0](https://github.com/bytechefhq/bytechef/blob/master/LICENSE).

```
Copyright 2025 ByteChef

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
```

---

## Contributors

[![Contributors](https://contrib.rocks/image?repo=bytechefhq/bytechef)](https://github.com/bytechefhq/bytechef/graphs/contributors)

---

## Credits

ByteChef started as a fork of [Piper](https://github.com/runabol/piper), an open-source, distributed workflow engine.
