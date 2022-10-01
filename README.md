<p align="center">
<a href="https://www.bytechef.io?utm_source=github&utm_medium=organic&utm_campaign=readme">
  <img src="static/bytechef_logo.png" alt="ByteChef Logo" width="350">
</a>
</p>

ByteChef is an open-source, low-code, extendable API integration & workflow automation platform that allows you to automate work by connecting applications and building workflows across your organization.

[Website](https://www.bytechef.io?utm_source=github&utm_medium=organic&utm_campaign=readme) • [Docs](https://docs.bytechef.io?utm_source=github&utm_medium=organic&utm_campaign=readme) • [Blog](https://blog.bytechef.io?utm_source=github&utm_medium=organic&utm_campaign=readme) • [Discord](https://discord.gg/PybnUM3Y) • [Forum](https://github.com/bytechefhq/bytechef/discussions) • [Twitter](https://twitter.com/bytechefhq)

[![GitHub Workflow Status](https://img.shields.io/github/workflow/status/bytechefhq/bytechef/ByteChef%20CI)](https://github.com/bytechefhq/bytechef/actions/workflows/gradle.yml) [![License](https://img.shields.io/static/v1?label=license&message=Apache&nbsp;v2.0&color=brightgreen)](https://github.com/bytechefhq/bytechef/blob/master/LICENSE)

## Why ByteChef?

Due to increasing number of various applications and services used inside organizations engineering teams are under constant pressure to develop new integrations and workflows to automate work inside those organizations.
ByteChef has the necessary infrastructure and features to integrate various applications and services and build workflows across organization:

1. Install ByteChef on the premise to have a full control over execution and data, besides being able to use a hosted version.
2. Define workflows via the UI editor by drag-and-dropping connectors and defining their relations, thus empowering your citizen developers. If you need to go beyond no-code workflow definition, your developers can leverage our low-code capabilities and write custom functions as part of your workflows, or extend ByteChef by developing custom connectors.
3. A more extensive range of various flow controls that can be used in the workflow like if, switch, loop, each, parallel, etc. Also, new conditionals can be added.
4. Develop custom connectors in four languages: Java, JavaScript, Python, and Ruby.
5. Write custom functions(executed during the workflow execution when you need to write the code) in the languages mentioned above.
6. Regarding multiple languages support, it is essential to emphasize that all languages are executed inside ONE runtime(no need for various execution runtimes for each language), simplifying scalability. You can start with one instance only, scale as required, and at the same time mix languages for your customizations.

[//]: # (7. embedded solution targeted explicitly for saas products which allow customers using your saas product to integrate application they use with it.)

## Getting Started

### ByteChef Cloud

[ByteChef Cloud](https://app.bytechef.io?utm_source=github&utm_medium=organic&utm_campaign=readme) is the fastest way to get started with ByteChef. It provides managed infrastructure as well as an instant and free trial access for development projects and proofs of concept.

<a href="https://app.bytechef.io?utm_source=github&utm_medium=organic&utm_campaign=readme"><img src="static/button_get-started-now.png" alt="Get started now" width="200px"></a>

For a step-by-step guide on ByteChef Cloud, [see the docs](https://docs.bytechef.io/cloud/getting-started?utm_medium=organic&utm_campaign=readme).

### Try using Docker
Want to give ByteChef a quick spin on your local machine? You can run the following command from your terminal to have ByteChef up and running right away.

```bashTaskHandler
docker run \
  --name bytechef \
  --restart unless-stopped \
  -p 3000:3000 \
  -v bytechef_data:/var/lib/postgresql/13/main \
  bytechef/try:latest
```

Then, open http://localhost:3000 in your browser.

### Self-Hosted

If you want to self-host ByteChef, we have [guides](https://docs.bytechef.io/self-hosting?utm_source=github&utm_medium=organic&utm_campaign=readme) for Docker, AWS and more.

## Documentation

- [Getting Started](https://docs.bytechef.io/introduction?utm_source=github&utm_medium=organic&utm_campaign=readme)
- [Architecture](https://docs.bytechef.io/architecture?utm_source=github&utm_medium=organic&utm_campaign=readme)
- [Components Reference](https://docs.bytechef.io/components)

## Community support
For general help using ByteChef, please refer to the official [documentation](https://docs.bytechef.io?utm_source=github&utm_medium=organic&utm_campaign=readme). For additional help, you can use one of these channels to ask a question:

- [Discord](https://discord.gg/PybnUM3Y) - Discussions with the community and the team.
- [GitHub](https://github.com/bytechefhq/bytechef/issues) - For bug reports and feature requests.
- [Forum](https://github.com/bytechefhq/bytechef/discussions) - Ask for help and propose new ideas.
- [Twitter](https://twitter.com/bytechefhq) - Get the product updates easily.

## Roadmap
Check out our [roadmap](https://github.com/orgs/bytechefhq/projects/3) to get informed of the latest features released and the upcoming ones.

### Contributing

If you'd like to contribute, kindly read our [Contributing Guide](CONTRIBUTING.md) to learn and understand about our development process, how to propose bug fixes and improvements, and how to build and test your changes to ByteChef.

## License
ByteChef is released under version 2.0 of the [Apache License](LICENSE).

## Note
This project has started as a fork of [Piper](https://github.com/okayrunner/piper), an open-source, distributed workflow engine.
