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
If you want to give documentation feedback, please join our [Discord Community](https://discord.gg/VKvNxHjpYx) and drop us a message.

## Documentation Issues
To enter documentation bugs or submit any feature request for documentation, please create a new [GitHub issue](https://github.com/bytechefhq/bytechef/issues/new?assignees=&labels=documentation&template=03_documentation_report.yml&title=%5Bdocs%5D%3A+). Please check if there is an existing issue first.

If you think the issue is with the ByteChef product itself, please choose the relevant issue template [here](https://github.com/bytechefhq/bytechef/issues/new/choose).

## Contributing
To contribute to ByteChef documentation, you need to fork this repository and submit a pull request for the Markdown and/or image changes that you're proposing.

### Repository organization
The documentation is built with [Astro Starlight](https://starlight.astro.build). The content in this directory follows the organization of documentation at https://docs.bytechef.io

Inside the `docs` directory, you'll see the following folders and files:

```
.
├── public/
├── src/
│   ├── assets/
│   ├── content/
│   │   ├── docs/
│   │   └── config.ts
│   └── env.d.ts
├── astro.config.mjs
├── package.json
├── tailwind.config.mjs
└── tsconfig.json
```

Starlight looks for `.md` or `.mdx` files in the `src/content/docs/` directory. Each file is exposed as a route based on its file name.

Images can be added to `src/assets/` and embedded in Markdown with a relative link.

Static assets, like favicons, can be placed in the `public/` directory.

### Workflow
The two suggested workflows are:

- For small changes, use the "Edit this page" button on each page to edit the Markdown file directly on GitHub.
- If you plan to make significant changes or preview the changes locally, clone the repo to your system to and follow the installation and local development steps in [Local setup](#local-setup).

### Conventions

- Name directories and files using lowercase letters. Use dash `-` as word separator.
  Example:
    - `/docs/src/content/docs/reference/components/components/sap-component.md`
    - `/docs/src/content/docs/how-to/bulk-database-updates.md`
- The front matter for every markdown file should include the `title` and a `description`.
  ```yaml
  ---
  title: Building custom component with ByteChef
  description: This page explains the steps needed to build a custom component
  ---
  ```

- Images are important to bring the product to life and clarify the written content. For images you're adding to the repo, store them in the `src/assets` subfolder. For every topic there needs to be a folder inside `src/assets` section, for example: `src/assets/component-reference/components/airtable/airtable-component.png`.
  When you link to an image, the path and filename are case-sensitive. The convention is for image filenames to be all lowercase and use dashes `-` for separators.

  >Example code for adding an image in markdown file:
  ```
  <div style={{textAlign: 'center'}}>

  ![ByteChef - Component - Airtable](component-reference/components/airtable/airtable-component.png)

  </div>
  ```

## Local setup

### Requirements
Rely to the latest LTS Node.js version 20 and its minors. Use nvm or other tool to setup or upgrade your OS environment. One may use [nvm](https://github.com/nvm-sh/nvm) to manage versions.

### 🧞 Commands

All commands are run from the `docs` directory, from a terminal:

| Command                   | Action                                           |
| :------------------------ | :----------------------------------------------- |
| `npm install`             | Installs dependencies                            |
| `npm run dev`             | Starts local dev server at `localhost:4321`      |
| `npm run build`           | Build your production site to `./dist/`          |
| `npm run preview`         | Preview your build locally, before deploying     |
| `npm run astro ...`       | Run CLI commands like `astro add`, `astro check` |
| `npm run astro -- --help` | Get help using the Astro CLI                     |

## Thank you

Thanks for all your contributions and efforts towards improving the ByteChef documentation. We thank you being part of our ✨ community ✨!
