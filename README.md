<p align="center">
<a href="https://www.bytechef.io?utm_source=github&utm_medium=organic&utm_campaign=readme">
  <img src="static/bytechef_logo.png" alt="ByteChef Logo" width="350">
</a>
</p>

ByteChef is an open-source, low-code, extendable API integration and workflow automation platform. It automates daily routines that require interaction between independent business applications. ByteChef maintains automation definitions in easy-to-understand workflow like format.
![anl-readme-md](https://static.scarf.sh/a.png?x-pxid=ceb7a380-3bfc-4e25-8068-1445d2d02359)

[Website](https://www.bytechef.io?utm_source=github&utm_medium=organic&utm_campaign=readme) - [Discord](https://discord.gg/VKvNxHjpYx) - [Twitter](https://twitter.com/bytechefhq)

[![CI Build](https://github.com/bytechefhq/bytechef/actions/workflows/build_push.yml/badge.svg)](https://github.com/bytechefhq/bytechef/actions/workflows/build_push.yml) [![License](https://img.shields.io/static/v1?label=license&message=Apache&nbsp;v2.0&color=brightgreen)](https://github.com/bytechefhq/bytechef/blob/master/LICENSE)

> UPDATE: ByteChef is under active development, we are in the alpha stage and some features might be currently missing or disabled.

<img src="static/screenshot.png" alt="ByteChef Screenshot" style="max-width: 100%;">

## Why ByteChef?

Due to increasing number of various applications and services used inside organizations, engineering teams are under constant pressure to develop new integrations and workflows to automate work inside those organizations.
ByteChef has the necessary infrastructure and features to integrate various applications and services and build workflows across organization:

1. Install ByteChef on the premise to have a full control over execution and data, besides being able to use a hosted version.
2. Define workflows via the UI editor by drag-and-dropping connectors and defining their relations, thus empowering your citizen developers. If you need to go beyond no-code workflow definition, your developers can leverage our low-code capabilities and write custom functions as part of your workflows, or extend ByteChef by developing custom connectors.
3. A more extensive range of various flow controls that can be used in the workflow like if, switch, loop, each, parallel, etc. Also, new conditionals can be added.
4. Develop custom connectors in four languages: Java, JavaScript, Python, and Ruby.
5. Write custom functions(executed during the workflow execution when you need to write the code) in the languages mentioned above.
6. All languages are executed inside ONE runtime(no need for various execution runtimes for each language), simplifying scalability. You can start with one instance only, scale as required, and at the same time mix languages for your customizations.

ByteChef can help you as:

1. Automation solution, which allows customers to integrate applications used internally to automate their own business processes.
2. Embedded solution targeted explicitly for products which allow your customers to integrate applications they use with your product.

## Getting Started
There are couple ways to give ByteChef a quick spin on your local machine. You can use this to test, learn or contribute.

[//]: # ()
[//]: # (### ByteChef Cloud)
[//]: # ()
[//]: # ([ByteChef Cloud]&#40;https://app.bytechef.io?utm_source=github&utm_medium=organic&utm_campaign=readme&#41; is the fastest way to get started with ByteChef. It provides managed infrastructure as well as an instant and free trial access for development projects and proofs of concept.)
[//]: # ()
[//]: # (<a href="https://app.bytechef.io?utm_source=github&utm_medium=organic&utm_campaign=readme"><img src="static/button_get-started-now.png" alt="Get started now" width="200px"></a>)
[//]: # ()
[//]: # (For a step-by-step guide on ByteChef Cloud, [see the docs]&#40;https://docs.bytechef.io/cloud/getting-started?utm_medium=organic&utm_campaign=readme&#41;.)
[//]: # ()

### Docker
1. Docker Compose

Requirement: Docker Desktop

This is the fastest possible way to start Bytechef. Download docker-compose.yml [docker-compose.yml](https://github.com/bytechefhq/bytechef/blob/master/docker-compose.yml) to your machine. Find it in this bytechef repository root. Execute:
```bashTaskHandler
docker compose -f docker-compose.yml up
```
Both postgres database and bytechef docker container would start.
2. Docker (run containers)

This option demands pinch of focus as it allows user to profile containers. Run the following commands from your terminal to have ByteChef up and running right away.

#### Create Docker Network
```bashTaskHandler
docker network create -d bridge bytechef_network
```
#### Start Postgres Docker Container
```bashTaskHandler
docker run --name postgres -d -p 5432:5432 \
    --env POSTGRES_USER=postgres \
    --env POSTGRES_PASSWORD=postgres \
    --network bytechef_network \
    -v /opt/postgre/data:/var/lib/postgresql/data \
    postgres:15-alpine
```
NOTE: `-v` mount option is not mandatory. It mounts local DB storage to make easier access to DB infrastructure files.

#### Start ByteChef Docker Container
```bashTaskHandler
docker run --name bytechef -it -p 80:8080 \
    --env SERVER_PORT=8080 \
    --env SPRING_PROFILES_ACTIVE=prod \
    --env BYTECHEF_DATASOURCE_URL=jdbc:postgresql://postgres:5432/bytechef \
    --env BYTECHEF_DATASOURCE_USERNAME=postgres \
    --env BYTECHEF_DATASOURCE_PASSWORD=postgres \
    --env BYTECHEF_SECURITY_REMEMBER_ME_KEY=e48612ba1fd46fa7089fe9f5085d8d164b53ffb2 \
    --network bytechef_network \
    bytechef/bytechef:latest
```
NOTE: `-it` (interactive) flag may be replaced with `-d` (daemon). Keep it interactive if you want to track logs which can be handy for troubleshooting.

3. Access Bytechef

Use browser and open http://localhost/login. Chose Create Account link to setup user and than use same user and password to sign in.

[//]: # (### Self-Hosted)
[//]: # ()
[//]: # (If you want to self-host ByteChef, we have [guides]&#40;https://docs.bytechef.io/self-hosting?utm_source=github&utm_medium=organic&utm_campaign=readme&#41; for Docker, AWS and more.)
[//]: # ()
## Documentation

- [Getting Started](https://docs.bytechef.io/introduction?utm_source=github&utm_medium=organic&utm_campaign=readme)

[//]: # (- [Architecture]&#40;https://docs.bytechef.io/architecture?utm_source=github&utm_medium=organic&utm_campaign=readme&#41;)
[//]: # (- [Components Reference]&#40;https://docs.bytechef.io/components&#41;)

## Community support

[//]: # (For general help using ByteChef, please refer to the official [documentation]&#40;https://docs.bytechef.io?utm_source=github&utm_medium=organic&utm_campaign=readme&#41;. For additional help, you can use one of these channels to ask a question:)

[//]: # (- [Discord]&#40;https://discord.gg/VKvNxHjpYx; - Discussions with the community and the team.)
[//]: # (- [GitHub]&#40;https://github.com/bytechefhq/bytechef/issues&#41; - For bug reports and feature requests.)
[//]: # (- [Forum]&#40;https://github.com/bytechefhq/bytechef/discussions&#41; - Ask for help and propose new ideas.)
[//]: # (- [Twitter]&#40;https://twitter.com/bytechefhq&#41; - Get the product updates easily.)

For help, you can use one of these channels to ask a question:

- [Discord](https://discord.gg/VKvNxHjpYx) - Discussions with the community and the team.
- [GitHub](https://github.com/bytechefhq/bytechef/issues) - For bug reports and feature requests.
- [Twitter](https://twitter.com/bytechefhq) - Get the product updates easily.

## Roadmap
Check out our [roadmap](https://github.com/orgs/bytechefhq/projects/3) to get informed of the latest features released and the upcoming ones.

### Contributing

If you'd like to contribute, kindly read our [Contributing Guide](CONTRIBUTING.md) to learn and understand about our development process, how to propose bug fixes and improvements, and how to build and test your changes to ByteChef.

## License
ByteChef is released under Apache License v2.0. See [LICENSE](LICENSE) for more information.

## Note
This project has started as a fork of [Piper](https://github.com/okayrunner/piper), an open-source, distributed workflow engine.
