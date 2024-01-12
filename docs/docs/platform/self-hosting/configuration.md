---
id: configuration
title: Configuration
---

It's possible to change some ByteChef default values using environment variables.

For a full list of available configurations see [Environment Variables](environment-variables.md).

## How to set

Location of these environment variables depends on how you are running ByteChef:

### Docker

For Docker, you can set your environment variables in the `bytechef: environment:` element of your `docker-compose.yaml` file. For example:

```
n8n:
  environment:
    - ATLAS_WORKFLOW-REPOSITORY_GIT_ENABLED=true
    - ATLAS_WORKFLOW-REPOSITORY_GIT_URL=<git url>
```

Or using the `-e` flag from the command line:

```
docker run -it --rmOperation \
  --name bytechef \
  -p 4000:4000 \
  -e ATLAS_WORKFLOW-REPOSITORY_GIT_ENABLED="true" \
  -e ATLAS_WORKFLOW-REPOSITORY_GIT_URL="<git url>" \
  bytechef/bytechef
```

### Configuration by file

You can also configure ByteChef using a configuration file. The file defined via environment variable in `.properties` or `.yml` format:

```
export SPRING_CONFIG_LOCATION=file:///Users/home/config/application.yml
```

A possible configuration file could look like this:

```
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/bytechef
    username: user
    password: user
    
bytechef:
  storage:
    provider: file
    fileStorageDir: /tmp/files
  workflow-repository:
    git:
      enabled: true
      # The URL to the Git Repo
      url: https://github.com/mygithub/workflows.git
      branch: master
      username: user
      password: user    
```

