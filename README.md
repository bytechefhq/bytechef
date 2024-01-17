<p align="center">
<a href="https://www.bytechef.io?utm_source=github&utm_medium=organic&utm_campaign=readme">
  <img src="static/bytechef_logo.png" alt="ByteChef Logo" width="350">
</a>
</p>

ByteChef is an open-source, low-code, extendable API integration & workflow automation platform that allows you to automate work by connecting applications and building workflows across your organization.

[//]: # ([Website]&#40;https://www.bytechef.io?utm_source=github&utm_medium=organic&utm_campaign=readme&#41; • [Docs]&#40;https://docs.bytechef.io?utm_source=github&utm_medium=organic&utm_campaign=readme&#41; • [Blog]&#40;https://blog.bytechef.io?utm_source=github&utm_medium=organic&utm_campaign=readme&#41; • [Discord]&#40;https://discord.gg/PybnUM3Y&#41; • [Forum]&#40;https://github.com/bytechefhq/bytechef/discussions&#41; • [Twitter]&#40;https://twitter.com/bytechefhq&#41;)

[Website](https://www.bytechef.io?utm_source=github&utm_medium=organic&utm_campaign=readme) - [Discord](https://discord.gg/Y9Dejg5R) - [Twitter](https://twitter.com/bytechefhq)

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

[//]: # (## Getting Started)
[//]: # ()
[//]: # (### ByteChef Cloud)
[//]: # ()
[//]: # ([ByteChef Cloud]&#40;https://app.bytechef.io?utm_source=github&utm_medium=organic&utm_campaign=readme&#41; is the fastest way to get started with ByteChef. It provides managed infrastructure as well as an instant and free trial access for development projects and proofs of concept.)
[//]: # ()
[//]: # (<a href="https://app.bytechef.io?utm_source=github&utm_medium=organic&utm_campaign=readme"><img src="static/button_get-started-now.png" alt="Get started now" width="200px"></a>)
[//]: # ()
[//]: # (For a step-by-step guide on ByteChef Cloud, [see the docs]&#40;https://docs.bytechef.io/cloud/getting-started?utm_medium=organic&utm_campaign=readme&#41;.)
[//]: # ()
[//]: # (### Try using Docker)
[//]: # ()
[//]: # (Want to give ByteChef a quick spin on your local machine? You can run the following command from your terminal to have ByteChef up and running right away.)
[//]: # ()
[//]: # (```bashTaskHandler)
[//]: # (docker run \)
[//]: # (  --name bytechef \)
[//]: # (  --restart unless-stopped \)
[//]: # (  -p 3000:3000 \)
[//]: # (  bytechef/try:latest)
[//]: # (```)
[//]: # (Then, open http://localhost:3000 in your browser.)
[//]: # ()
[//]: # (### Self-Hosted)
[//]: # ()
[//]: # (If you want to self-host ByteChef, we have [guides]&#40;https://docs.bytechef.io/self-hosting?utm_source=github&utm_medium=organic&utm_campaign=readme&#41; for Docker, AWS and more.)
[//]: # ()
[//]: # (## Documentation)
[//]: # ()
[//]: # (- [Getting Started]&#40;https://docs.bytechef.io/introduction?utm_source=github&utm_medium=organic&utm_campaign=readme&#41;)
[//]: # (- [Architecture]&#40;https://docs.bytechef.io/architecture?utm_source=github&utm_medium=organic&utm_campaign=readme&#41;)
[//]: # (- [Components Reference]&#40;https://docs.bytechef.io/components&#41;)

## Community support

[//]: # (For general help using ByteChef, please refer to the official [documentation]&#40;https://docs.bytechef.io?utm_source=github&utm_medium=organic&utm_campaign=readme&#41;. For additional help, you can use one of these channels to ask a question:)

[//]: # (- [Discord]&#40;https://discord.gg/VxCenmHB5P; - Discussions with the community and the team.)
[//]: # (- [GitHub]&#40;https://github.com/bytechefhq/bytechef/issues&#41; - For bug reports and feature requests.)
[//]: # (- [Forum]&#40;https://github.com/bytechefhq/bytechef/discussions&#41; - Ask for help and propose new ideas.)
[//]: # (- [Twitter]&#40;https://twitter.com/bytechefhq&#41; - Get the product updates easily.)

For help, you can use one of these channels to ask a question:

- [Discord](https://discord.gg/Y9Dejg5R) - Discussions with the community and the team.
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
