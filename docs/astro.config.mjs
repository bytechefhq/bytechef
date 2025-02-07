import {defineConfig} from 'astro/config';
import starlight from '@astrojs/starlight';
import tailwind from '@astrojs/tailwind';

// https://astro.build/config
export default defineConfig({
    site: 'https://docs.bytechef.io',
    integrations: [
        starlight({
            title: 'ByteChef',
            description: 'ByteChef is an open-source, low-code tool for integrating and building automation workflows across your SaaS apps, internal APIs, and databases.',
            logo: {
                src: './src/assets/icons/logo.svg',
            },
            head: [
                // Plausible.io web analytics script tag.
                {
                    tag: 'script',
                    attrs: {
                        src: '/javascript/analytics.js',
                    },
                },
            ],
            social: {
                discord: 'https://discord.gg/JcNSqJ7vK8',
                github: 'https://github.com/bytechefhq/bytechef',
                twitter: 'https://twitter.com/bytechefhq',
            },
            editLink: {
                baseUrl: 'https://github.com/bytechefhq/bytechef/edit/master/docs/',
            },
            sidebar: [
                {label: 'Welcome', link: '/welcome'},
                {
                    label: 'Automation',
                    items: [
                        {
                            label: 'Getting Started',
                            items: [
                                // { label: 'Introduction', link: '/automation/getting-started/introduction' },
                                {label: 'Glossary', link: '/automation/getting-started/glossary'},
                                {label: 'Quick Start - Star Repository on Github', link: '/automation/getting-started/quick-start-star-repository'},
                                {label: 'Quick Start - Trigger', link: '/automation/getting-started/quick-start-trigger'}
                            ],
                        },
                        {
                            label: 'Build Workflows',
                            items: [
                                {label: 'Overview', link: '/automation/build-workflows/overview'},
                                {label: 'Project Guides', link: '/automation/build-workflows/project-guides'},
                                {label: 'Workflow Guides', link: '/automation/build-workflows/workflow-guides'}
                            ],
                        }
                    ],
                },
                {
                    label: 'Embedded',
                    items: [
                        {
                            label: 'Getting Started',
                            items: [
                                // { label: 'Introduction', link: '/embedded/getting-started/introduction' },
                                {label: 'Quick Start', link: '/embedded/getting-started/quick-start'},
                            ]
                        },
                    ],
                },
                {
                    label: 'Developer Guide',
                    items: [
                        {
                            label: 'Build Component',
                            items: [
                                {label: 'Overview', link: '/developer-guide/build-component/overview'},
                                {label: 'Initial Setup', link: '/developer-guide/build-component/initial-setup'},
                                {
                                    label: 'Create Component Definition',
                                    link: '/developer-guide/build-component/create-component-definition'
                                },
                                {
                                    label: 'Add Connection',
                                    link: '/developer-guide/build-component/add-connection'
                                },
                                {
                                    label: 'Create Action',
                                    link: '/developer-guide/build-component/create-action'
                                },
                                {
                                    label: 'Create Trigger',
                                    link: '/developer-guide/build-component/create-trigger'
                                }
                            ],
                            collapsed: true,
                        },
                        {
                            label: 'Generate Component',
                            items: [
                                {label: 'Overview', link: '/developer-guide/generate-component/overview'},
                                {label: 'Initial Setup', link: '/developer-guide/generate-component/initial-setup'},
                                {
                                    label: 'OpenAPI Specification',
                                    link: '/developer-guide/generate-component/open-api-specification'
                                },
                                {
                                    label: 'Customize Component',
                                    link: '/developer-guide/generate-component/customize-component'
                                },
                                {
                                    label: 'Create Trigger',
                                    link: '/developer-guide/generate-component/create-trigger'
                                }
                            ],
                            collapsed: true,
                        },
                        {
                            label: 'Testing Triggers',
                            items: [
                                {label: 'Working with Triggers', link: '/developer-guide/testing-triggers/triggers'},
                            ],
                            collapsed: true,
                        },
                        {
                            label: 'Component Specification',
                            items: [
                                {label: 'Component', link: '/developer-guide/component-specification/component'},
                                {label: 'Connection', link: '/developer-guide/component-specification/connection'},
                                {label: 'Action', link: '/developer-guide/component-specification/action'},
                                {label: 'Trigger', link: '/developer-guide/component-specification/trigger'},
                                {label: 'Property', link: '/developer-guide/component-specification/property'}
                            ],
                            collapsed: true,
                        },
                        {
                            label: 'Components',
                            autogenerate: {directory: '/developer-guide/components'},
                            collapsed: true,
                        },
                        {
                            label: 'Observability',
                            autogenerate: {directory: '/developer-guide/observability'},
                            collapsed: true,
                        }
                    ]
                },
                {
                    label: 'Reference',
                    items: [
                        {label: 'Overview', link: '/reference/overview'},
                        {
                            label: 'Components',
                            autogenerate: {directory: '/reference/components'},
                            collapsed: true,
                        },
                        {
                            label: 'Flow Controls',
                            autogenerate: {directory: '/reference/task-dispatchers'},
                            collapsed: true,
                        }
                    ]
                },
            ],
            customCss: ['./src/tailwind.css'],
        }),
        tailwind({applyBaseStyles: false}),
    ],
});
