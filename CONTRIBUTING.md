# Contributing to ByteChef

Thanks for taking the time for contribution to ByteChef!
We're very welcoming community and while it's very much appreciated if you follow these guidelines it's not a requirement.
![anl-contributing-md](https://static.scarf.sh/a.png?x-pxid=a1e692ba-38cb-4361-a10b-b013c5e691b6)

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

### Code of Conduct
This project and everyone participating in it is governed by the [ByteChef Code of Conduct](./CODE_OF_CONDUCT.md).
By participating, you are expected to uphold this code. Please report unacceptable behavior to support@bytechef.io.

### Step-by-step guide to contributing

#### We Use [GitHub Flow](https://guides.github.com/introduction/flow/index.html), So All Code Changes Happen Through Pull Requests
Pull requests are the best way to propose changes to the codebase (we use [Git-Flow](https://nvie.com/posts/a-successful-git-branching-model/)). We actively welcome your pull requests:

1. Fork the repo and create a new branch from the `develop` branch (currently, temporarily, it's the `master` branch).
2. Branches are named as `bug/fix-name` or `feature/feature-name`
3. To work on the client codebase, go through [Client Side](#client-side). To work on the server codebase, go through [Server Side](#server-side).
4. Once you are confident in your code changes, create a pull request in your fork to the `develop` branch (currently, temporarily, it's the `master` branch) in the bytechefhq/bytechef base repository.
5. If you've changed any APIs, please mention it in the pull request and ensure backward compatibility.
6. Link the issue of the base repository in your Pull request description. [Guide](https://docs.github.com/en/free-pro-team@latest/github/managing-your-work-on-github/linking-a-pull-request-to-an-issue)
7. When you raise a pull request, we automatically run tests on our CI. Please ensure that all the tests are passing for your code change. We will not be able to accept your change if the test suite doesn't pass.

[//]: # (8. Documentation: When new features are added or there are changes to existing features that require updates to documentation, we encourage you to add/update any missing documentation in the [`/docs` folder]&#40;https://github.com/bytechefhq/bytechef/tree/master/docs&#41;. To update an existing documentation page, you can simply click on the `Edit this page` button on the bottom left corner of the documentation page.)


### Client Side

1. Run the client successfully: [Setup with Docker](#setup-with-docker).
2. Add your changes.
3. Please add tests for your changes. Client-side changes require Vitest/Playwright tests.
4. When you finish adding your changes, run the following commands inside the `client` directory:
    ```bash
    ./npm run format
    ./npm run check
    ```
   If there are no errors, you can continue through the steps.

### Server Side

1. Run the server successfully: [Local Setup](#local-setup).
2. Add your changes.
3. Please add tests for your changes. Server-side changes require JUnit/Integration tests.
4. If you are working on a component, you need to (re)generate the .json file. The .json file is located in `./src/test/resources/definition/.json`. If such file exists, you have to delete it. Open a file located in `./src/test/java/com/bytechef/component/...` that ends with the postfix `ComponentHandlerTest`. By running all tests if that file, the new .json file will be automatically generated.
4. When you finish adding your changes, run the following commands inside `server` directory or in the root directory of your component if you worked on a component:
    ```bash
    ./gradlew spotlessApply
    ./gradlew check
    ```
   If there are no errors, you can continue through the steps.

# Setup ByteChef platform for local development and testing
ByteChef platform consists of three major parts. User interface is implemented with Node, ReactJS and TypeScript. Backend microservices are implemented in Java programming language and Spring framework. Dependent infrastructure PostgreSQL, RabbitMQ, Redis. Each part could be encapsulated in Docker container to reduce efforts.

## How to run platform
### Prerequisites
- [Docker](https://docs.docker.com/get-docker/)
- [Java - GraalVM for JDK 21.0.2+](https://www.graalvm.org/downloads/)
- Gradle - V8.5+. - Comes as part of the project as [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html)
- A PostgreSQL database - Refer to the [Setting up local development infrastructure](#setting-up-local-development-infrastructure-using-docker).
- A Redis instance - Refer to the [Setting up local development infrastructure](#setting-up-local-development-infrastructure-using-docker).
- [Node v20+](https://nodejs.org/en/download/)
- [Docker](https://docs.docker.com/get-docker/)
### Run from command line
1. Open terminal application.
2. Make sure `java -version` and `JAVA_HOME` references Java JDK 21+
3. Clone the ByteChef repository to local directory we will call `BYTECHEF_HOME`.

    ```bash
    git clone https://github.com/bytechefhq/bytechef.git
    cd bytechef
    ```
4. **Change working directory to the `BYTECHEF_HOME/server` folder.**
5. Start up the docker container with dependent infrastructure

    ```bash
    docker compose -f docker-compose.dev.infra.yml up -d
    ```
6. Compile codebase:

    ```bash
    ../gradlew clean compileJava
    ```

7. **Change working directory to the `BYTECHEF_HOME/server/apps/server-app` folder.**
8. Start the ByteChef server instance by running:

    ```bash
    ../../../gradlew bootRun
    ```
9. **Change working directory to the `BYTECHEF_HOME/client` folder.**
10. Install dependencies.

    ```bash
    npm install
    ```

11. Serve with hot reload.

    ```bash
    npm run dev
    ```

#### Note:
User interface application connects to the microservices server at the predefined URL http://127.0.0.1:9555. If microservices backend API server is not present, your page will load with errors. The API server starts on default port 9555. To configure the API server in details, please follow [Setup With docker](#setup-with-docker) instructions. The API server status is available at the endpoint: http://localhost:9555/swagger-ui/index.html. Type it in the browser's address bar to get Swagger UI with the list of API endpoints.

If ran for the first time the API server automatically populate database with required data and demo projects. Subsequent runs against existing database would trigger table updates on PostgreSQL.

## User Interface Development tasks
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

## Backend Microservices Developemnt Tasks

This section explains how you can set up a development environment for ByteChef server instance. As the server codebase is written in Java and is powered by Spring, you need Java and Gradle installed to build the code. You also need one instance of PostgreSQL and Redis each to run ByteChef server instance.

## Setup with Docker

Build and run the server codebase in a Docker container. This is the easiest way to get the server instance up and running if you are more interested in contributing to the [client codebase](#client-side).


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

#### Initial users available:
- username: admin@localhost.com
  - password: admin
- username: user@localhost.com
  - password: user


## Local Setup

This section doesn't provide instructions to install Java and Gradle because these vary between different operating systems and distributions. Please refer to the documentation of your operating system or package manager to install these tools.

### Steps for setup


#### Setting up local development infrastructure using Docker

Use `docker-compose.dev.infra.yml` for running required infrastructure (PostgreSQL, Redis):

    ```bash
    docker compose -f docker-compose.dev.infra.yml up -d
    ```
### Running Source Formatting for Java Code

```bash
./gradlew spotlessApply
```

### Running Check

```bash
./gradlew check
```

### Running Tests

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

#### spotlessApply generates unwanted files
If you ran our source formatting for java code (`./gradlew spotlessApply`) and a bunch of files were generated, it might be your git configuration.

Run `git config --list --local` in your command line. If `core.filemode` is true, set it to false:
```bash
git config core.filemode false
```
If you are using windows, you need to switch LF endings to CRLF endings:
```bash
git config core.autocrlf true
```


## Questions?
Contact us on [Discord](https://discord.gg/VKvNxHjpYx) or mail us at [support@bytechef.io](mailto:support@bytechef.io).
