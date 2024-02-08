<p align="center">
  <img alt="ByteChef logo" src="static/img/logo.svg" width="32px" />
  <h1 align="center">ByteChef Documentation</h1>
</p>

This repository contains the ByteChef documentation website code and Markdown source files for [docs.bytechef.io](docs.bytechef.io)

## Index
- [Feedback](#feedback)
- [Documentation Issues](#documentation-issues)
- [Contributing](#contributing)
    - [Repository organization](#repository-organization)
    - [Workflow](#workflow)
    - [Conventions](#conventions)
- [Local setup](#local-setup)

## Feedback
If you want to give documentation feedback, please join our [Discord Community](https://discord.gg/VKvNxHjpYx) or go to our [Forum](https://github.com/bytechefhq/bytechef/discussions) and drop us a message.

## Documentation Issues
To enter documentation bugs or submit any feature request for documentation, please create a new [GitHub issue](https://github.com/bytechefhq/bytechef/issues/new?assignees=&labels=documentation&template=03_documentation_report.yml&title=%5Bdocs%5D%3A+). Please check if there is an existing issue first.

If you think the issue is with the ByteChef product itself, please choose the relevant issue template [here](https://github.com/bytechefhq/bytechef/issues/new/choose).

## Contributing
To contribute to ByteChef documentation, you need to fork this repository and submit a pull request for the Markdown and/or image changes that you're proposing.

### Repository organization
The content in this directory follows the organization of documentation at https://docs.bytechef.io

This directory contains the following folders:

\docs - contains the Markdown files used for the content, grouped by sub-folders.

\src - contains the ByteChef documentation website code. The documentation website is built using [Docusaurus 2](https://docusaurus.io/).

\static\img - contains folders that references the images (such as screenshots) used in the \docs\topic.

### Workflow
The two suggested workflows are:

- For small changes, use the "Edit this page" button on each page to edit the Markdown file directly on GitHub.
- If you plan to make significant changes or preview the changes locally, clone the repo to your system to and follow the installation and local development steps in [Local setup](#local-setup).

### Conventions

- Name directories and files using lowercase letters. Use dash `-` as word separator.
  Example:
    - `/docs/components/components/sap-component.md`
    - `/docs/how-to/bulk-database-updates.md`
- The front matter for every markdown file should include the `id` and a `title`. `id` will be used to reference the file in `sidebar.js`
  ```yaml
  ---
  id: building-custom-component
  title: Building custom component with ByteChef
  ---
  ```

- Images are important to bring the product to life and clarify the written content. For images you're adding to the repo, store them in the `img` subfolder inside `static` folder. For every topic there needs to be a folder inside `\static\img\` section, for example: `static\img\component-reference\components\airtable\airtable-component.png`.
  When you link to an image, the path and filename are case-sensitive. The convention is for image filenames to be all lowercase and use dashes `-` for separators.

  >Example code for adding an image in markdown file:
  ```
  <div style={{textAlign: 'center'}}>

  ![ByteChef - Component - Airtable](/img/component-reference/components/airtable/airtable-component.png)

  </div>
  ```

## Local setup

### Requirements
Rely to the latest LTS Node.js version 20 and its minors. Use nvm or other tool to setup or upgrade your OS environment. One may use [nvm](https://github.com/nvm-sh/nvm) to manage versions.

### Installation
Position to ../docs directory and install local instance of documentation manager
```
$ npm install
```

### Local Development
Assuming all previous steps succeed one can start documentation manager application from ../docs directory: 
```
$ npm start
```

This command starts a local development server and opens up a browser window. Most changes are reflected live without having to restart the server.

### Build

```
$ npm run build
```

This command generates static content into the `build` directory and can be served using any static contents hosting service.

### Deployment

Using SSH:

```
$ USE_SSH=true npm run deploy
```

Not using SSH:

```
$ GIT_USER=<Your GitHub username> npm run deploy
```

If you are using GitHub pages for hosting, this command is a convenient way to build the website and push to the `gh-pages` branch.

## Thank you

Thanks for all your contributions and efforts towards improving the ByteChef documentation. We thank you being part of our ✨ community ✨!
