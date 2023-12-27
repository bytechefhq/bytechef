# Contributing to ByteChef

Thanks for taking the time for contribution to ByteChef!
We're very welcoming community and while it's very much appreciated if you follow these guidelines it's not a requirement.

## Code of Conduct
This project and everyone participating in it is governed by the [ByteChef Code of Conduct](./CODE_OF_CONDUCT.md).
By participating, you are expected to uphold this code. Please report unacceptable behavior to support@bytechef.io.

## How can I contribute?

There are many ways in which you can contribute to ByteChef.

### Report a bug
Report all issues through GitHub Issues using the [Report a Bug](https://github.com/bytechefhq/bytechef/issues/new?assignees=&labels=bug%2Cneeds+triaging&template=bug-report.yaml&title=%5Bbug%5D%3A+) template.
To help resolve your issue as quickly as possible, read the template and provide all the requested information.

### File a feature/component request
We welcome all feature requests, whether it's to add new functionality, improve existing connectors or to suggest a brand new connector.
File your feature request through GitHub Issues using the [Feature Request](https://github.com/bytechefhq/bytechef/issues/new?assignees=&labels=enhancement&template=feature-request.yaml&title=%5Bfeature%5D%3A+) template for improvements or [Connector Request](https://github.com/bytechefhq/bytechef/issues/new?assignees=&labels=nhancement,component&template=new-component-request.yaml&title=%5Bcomponent%5D%3A+) for improvements to the existing components or for the new ones.

### Improve the documentation
You can help by suggesting improvements to our documentation using the [Documentation Improvement](https://github.com/bytechefhq/bytechef/issues/new?assignees=&labels=documentation&template=documentation-improvement.yaml&title=%5Bdocs%5D%3A+) template or check [Step-by-step guide to contributing](#step-by-step-guide-to-contributing)!

### Close a Bug / Feature issue
Find [issues](https://github.com/bytechefhq/bytechef/issues?q=is%3Aissue+is%3Aopen+sort%3Aupdated-desc) where we need help. Search for issues with either [`good first issue`](https://github.com/bytechefhq/bytechef/issues?q=is%3Aissue+is%3Aopen+sort%3Aupdated-desc+label%3A%22good+first+issue%22+) and/or [`help wanted`](https://github.com/bytechefhq/bytechef/issues?q=is%3Aissue+is%3Aopen+sort%3Aupdated-desc+label%3A%22help+wanted%22) labels. Check out the following [Code Contribution Guide](#contributing-code-changes) to begin.

## Contributing Code Changes

Please review the following sections before proposing code changes.

### License
By contributing, you agree that your contributions will be licensed under the terms of the [ByteChef project licenses](https://github.com/bytechefhq/bytechef/blob/master/README.md#license).

[//]: # (### Developer Certificate of Origin &#40;DCO&#41;)

[//]: # ()
[//]: # (By contributing to ByteChef, Inc., You accept and agree to the terms and conditions in the [Developer Certificate of Origin]&#40;https://github.com/bytechefhq/bytechef/blob/master/DCO.md&#41; for Your present and future Contributions submitted to ByteChef, Inc. Your contribution includes any submissions to the [ByteChef repository]&#40;https://github.com/bytechefhq&#41; when you click on such buttons as `Propose changes` or `Create pull request`. Except for the licenses granted herein, You reserve all right, title, and interest in and to Your Contributions.)

### Step-by-step guide to contributing

#### We Use [GitHub Flow](https://guides.github.com/introduction/flow/index.html), So All Code Changes Happen Through Pull Requests
Pull requests are the best way to propose changes to the codebase (we use [Git-Flow](https://nvie.com/posts/a-successful-git-branching-model/)). We actively welcome your pull requests:

1. Fork the repo and create a new branch from the `develop` branch.
2. Branches are named as `bug/fix-name` or `feature/feature-name`
3. To work on the client codebase, go through [Client Side](#client-side) and [Setup with Docker](#setup-with-docker)
4. To work on the server codebase, go through [Server Side](#server-side) and [Local Setup](#local-setup)
4. Please add tests for your changes. Client-side changes require Vitest/Playwright tests while server-side changes require JUnit/Integration tests.
5. When you finish adding your changes, run the following commands inside the `client` directory if you worked on the client codebase:
    ```bash
    ./npm run format
    ./npm run check
    ```
   and/or inside the `server` directory if you worked on the server codebase:
    ```bash
    ./gradlew spotlessApply
    ./gradlew check
    ```
6. Once you are confident in your code changes, create a pull request in your fork to the `develop` branch in the bytechefhq/bytechef base repository.
7. If you've changed any APIs, please call this out in the pull request and ensure backward compatibility.
8. Link the issue of the base repository in your Pull request description. [Guide](https://docs.github.com/en/free-pro-team@latest/github/managing-your-work-on-github/linking-a-pull-request-to-an-issue)
9. When you raise a pull request, we automatically run tests on our CI. Please ensure that all the tests are passing for your code change. We will not be able to accept your change if the test suite doesn't pass.
10. Documentation: When new features are added or there are changes to existing features that require updates to documentation, we encourage you to add/update any missing documentation in the [`/docs` folder](https://github.com/bytechefhq/bytechef/tree/master/docs). To update an existing documentation page, you can simply click on the `Edit this page` button on the bottom left corner of the documentation page.


# Setup for local development

## Client Side

This section explains how you can setup a development environment for ByteChef client app. ByteChef's client (UI/frontend) uses the ReactJS library and Typescript.

### Prerequisites

- [Node v20+](https://nodejs.org/en/download/)
- [Docker](https://docs.docker.com/get-docker/)

### Steps for setup

1. Clone the ByteChef repository(if you haven't already), open terminal and `cd` into it

    ```bash
    git clone https://github.com/bytechefhq/bytechef.git
    cd bytechef
    ```

2. Change your directory to the `client` folder.

    ```bash
    cd client
    ```

2. Install dependencies.

    ```bash
    npm install
    ```

3. Serve with hot reload.

    ```bash
    npm run dev
    ```

#### Note:
By default, your client app points to the local API server - http://127.0.0.1:9555. If you don't have the API server running on your local system, your page will load with errors. To set up the API server on your local system, please follow [Setup With docker](#setup-with-docker) instructions.

### Running Source Formatting

```bash
npm run format
```

### Running Lint

```bash
npm run lint
```

### Running Typecheck

```bash
npm run typecheck
```

### Running Check

```bash
npm run check
```

### Running Build

```bash
npm run build
```

### Running Tests

```bash
npm run test
```

## Server Side

This section explains how you can set up a development environment for ByteChef server instance. As the server codebase is written in Java and is powered by Spring, you need Java and Gradle installed to build the code. You also need one instance of PostgreSQL and Redis each to run ByteChef server instance.

## Setup with Docker

Build and run the server codebase in a Docker container. This is the easiest way to get the server instance up and running if you are more interested in contributing to the [client codebase](#client-side).

### Prerequisites

[Docker](https://docs.docker.com/get-docker/)

### Steps for setup

1. Clone the ByteChef repository(if you haven't already), open terminal and `cd` into it

    ```bash
    git clone https://github.com/bytechefhq/bytechef.git
    cd bytechef
    ```

2. Change your directory to the `server` folder.

    ```bash
    cd server
    ```

3. Start up the container

    ```bash
    docker compose -f docker-compose.dev.server.yml up -d
    ```

#### Other useful Commands

Stop locally built server instance.

```bash
docker compose -f docker-compose.dev.server.yml down
```

Rebuild a docker image of the locally built server instance.

```bash
docker compose -f docker-compose.dev.server.yml down --rmi local
docker compose -f docker-compose.dev.server.yml up -d
```


## Local Setup

This section doesn't provide instructions to install Java and Gradle because these vary between different operating systems and distributions. Please refer to the documentation of your operating system or package manager to install these tools.

### Prerequisites

- [Docker](https://docs.docker.com/get-docker/)
- [Java - GraalVM for JDK 21+](https://www.graalvm.org/downloads/)
- Gradle - V8.5+. - Comes as part of the project as [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html)
- A PostgreSQL database - Refer to the [Setting up local development infrastructure](#setting-up-local-development-infrastructure-using-docker).
- A Redis instance - Refer to the [Setting up local development infrastructure](#setting-up-local-development-infrastructure-using-docker).

### Steps for setup

1. Clone the ByteChef repository, open terminal and `cd` into it

    ```bash
    git clone https://github.com/bytechefhq/bytechef.git
    cd bytechef
    ```

#### Setting up local development infrastructure using Docker

2. Change your directory to th `server` folder.

    ```bash
    cd server
    ```

3. Use `docker-compose.dev.infra.yml` for running required infrastructure (PostgreSQL, Redis):

    ```bash
    docker compose -f docker-compose.dev.infra.yml up -d
    ```

#### Building and running the code

4. Run the following command to compile codebase:

    ```bash
    ../gradlew clean compileJava
    ```

5. Change your directory to th `apps/server-app` folder.

    ```bash
    cd apps/server-app
    ```

6. Start the ByteChef server instance by running :

    ```bash
    ../../../gradlew bootRun
    ```

By default, the server will start on port 9555.

When the server starts, it automatically runs migrations on PostgreSQL and populate it with some initial required data.

You can check the status of the server by hitting the endpoint: http://localhost:9555/swagger-ui/index.html on your browser to get Swagger UI with the list of API endpoints.

### Running Source Formatting

```bash
./gradlew spotlessApply
```

### Running Check

```bash
./gradlew check
```

#### Running Tests

```bash
./gradlew test && ./gradlew testIntegration
```

### Troubleshooting

#### Out of date schema

If you see `Either revert the changes to the migration, or run repair to update the schema history` in the terminal log when starting the server, execute the following command which will remove the out of date schemas:

For server setup with Docker:
```bash
docker compose -f server/docker-compose.dev.server.yml down -v
```
For server local setup:
```bash
docker compose -f server/docker-compose.dev.infra.yml down -v
```

## Questions?
Contact us on [Discord](https://discord.gg/CBAC2rCDsq) or mail us at [support@bytechef.io](mailto:support@bytechef.io).
