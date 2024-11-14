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
                                {label: 'Quick Start', link: '/automation/getting-started/quick-start'},
                            ],
                        },
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
                                {label: 'Overview', link: '/developer_guide/build_component/overview'},
                                {label: 'Initial Setup', link: '/developer_guide/build_component/initial_setup'},
                                {
                                    label: 'Create Component Definition',
                                    link: '/developer_guide/build_component/create_component_definition'
                                },
                                {
                                    label: 'Add Connection',
                                    link: '/developer_guide/build_component/add_connection'
                                },
                                {
                                    label: 'Create Action',
                                    link: '/developer_guide/build_component/create_action'
                                }
                            ],
                            collapsed: true,
                        },
                        {
                            label: 'Generate Component',
                            items: [
                                {label: 'Overview', link: '/developer_guide/generate_component/overview'},
                                {label: 'Initial Setup', link: '/developer_guide/generate_component/initial_setup'},
                                {
                                    label: 'OpenAPI Specification',
                                    link: '/developer_guide/generate_component/open_api_specification'
                                },
                                {
                                    label: 'Customize Component',
                                    link: '/developer_guide/generate_component/customize_component'
                                },
                            ],
                            collapsed: true,
                        },
                        {
                            label: 'Testing Triggers',
                            items: [
                                {label: 'Working with Triggers', link: '/developer_guide/testing_triggers/triggers'},
                            ],
                            collapsed: true,
                        },
                        {
                            label: 'Component Specification',
                            items: [
                                {label: 'Component', link: '/developer_guide/component_specification/component'},
                                {label: 'Connection', link: '/developer_guide/component_specification/connection'},
                                {label: 'Action', link: '/developer_guide/component_specification/action'},
                                {label: 'Property', link: '/developer_guide/component_specification/property'}
                            ],
                            collapsed: true,
                        },
                        {
                            label: 'Components',
                            autogenerate: {directory: '/developer_guide/components'},
                            collapsed: true,
                        }
                    ]
                },
                {
                    label: 'Reference',
                    items: [
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
