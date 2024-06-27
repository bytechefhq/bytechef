// @ts-check
// Note: type annotations allow type checking and IDEs autocompletion

const {themes} = require('prism-react-renderer');
const lightCodeTheme = themes.github;
const darkCodeTheme = themes.dracula;

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'ByteChef Docs',
  tagline: 'An open-source, low-code API integration & workflow automation platform for connecting the apps you use every day. It allows you to visualize, design and automate your processes in minutes.',
  url: 'https://docs.bytechef.io',
  baseUrl: '/',
  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',
  favicon: 'img/favicon.ico',
  organizationName: 'bytechefhq', // Usually your GitHub org/user name.
  projectName: 'bytechef', // Usually your repo name.

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          routeBasePath: '/',
          sidebarPath: require.resolve('./sidebars.js'),
          // Please change this to your repo.
          editUrl: 'https://github.com/bytechefhq/bytechef/docs/',
        },
        blog: false,
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
      }),
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      forceDarkMode: true,
      navbar: {
        title: 'ByteChef',
        logo: {
          alt: 'ByteChef Logo',
          src: 'img/logo.svg',
        },
        items: [
          {
            type: 'docSidebar',
            sidebarId: 'platform',
            position: 'left',
            label: 'Platform',
          },
          {
            type: 'docSidebar',
            sidebarId: 'reference',
            position: 'left',
            label: 'Reference',
          },
          {
            href: 'https://github.com/bytechefhq',
            label: 'GitHub',
            position: 'right',
            // class: 'header-github-link'
          },
          {
            href: 'https://discord.gg/VKvNxHjpYx',
            label: 'Discord',
            position: 'right',
          },
          {
            href: 'https://twitter.com/bytechefhq',
            label: 'Twitter',
            position: 'right',
          },
        ],
      },
      // footer: {
      //   style: 'dark',
      //   links: [
      //     {
      //       title: 'Docs',
      //       items: [
      //         {
      //           label: 'Introduction',
      //           to: '/',
      //         },
      //       ],
      //     },
      //     {
      //       title: 'Community',
      //       items: [
      //         // {
      //         //   label: 'Stack Overflow',
      //         //   href: 'https://stackoverflow.com/questions/tagged/bytechef',
      //         // },
      //         {
      //           label: 'Discussions',
      //           href: 'https://github.com/bytechefhq/bytechef/discussions',
      //         },
      //         {
      //           label: 'Discord',
      //           href: 'https://discord.gg/VKvNxHjpYx',
      //         },
      //         {
      //           label: 'Twitter',
      //           href: 'https://twitter.com/bytechefhq',
      //         },
      //       ],
      //     },
      //     {
      //       title: 'More',
      //       items: [
      //         {
      //           label: 'GitHub',
      //           href: 'https://github.com/bytechefhq/bytechef',
      //         },
      //       ],
      //     },
      //   ],
      //   copyright: `Copyright © ${new Date().getFullYear()} ByteChef Inc.`,
      // },
      prism: {
        theme: lightCodeTheme,
        darkTheme: darkCodeTheme,
      },
      algolia: {
        // The application ID provided by Algolia
        appId: 'YOUR_APP_ID',

        // Public API key: it is safe to commit it
        apiKey: 'YOUR_SEARCH_API_KEY',

        indexName: 'YOUR_INDEX_NAME',

        // Optional: see doc section below
        contextualSearch: true,

        // Optional: Specify domains where the navigation should occur through window.location instead on history.push. Useful when our Algolia config crawls multiple documentation sites and we want to navigate with window.location.href to them.
        externalUrlRegex: 'external\\.com|bytechef\\.io',

        // Optional: Algolia search parameters
        searchParameters: {},

        //... other Algolia params
      },
      colorMode: {
        respectPrefersColorScheme: true,
      },
    }),
};

module.exports = config;
