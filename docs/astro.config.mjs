import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';
import tailwind from '@astrojs/tailwind';

// https://astro.build/config
export default defineConfig({
    site: 'https://docs.bytechef.io',
	integrations: [
		starlight({
			title: 'ByteChef',
            logo: {
                src: './src/assets/logo.svg',
            },
            head: [
                // Plausible.io web analytics script tag.
                {
                    tag: 'script',
                    attrs: {
                        src: 'https://plausible.io/js/script.js',
                        'data-domain': 'docs.bytechef.io',
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
                { label: 'Welcome', link: '/welcome' },
				{
					label: 'Automation',
                    items: [
                        {
                            label: 'Getting Started',
                            items: [
                                { label: 'Introduction', link: '/automation/getting-started/introduction' },
                                { label: 'Quick Start', link: '/automation/getting-started/quick-start' },
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
                                { label: 'Introduction', link: '/embedded/getting-started/introduction' },
                                { label: 'Quick Start', link: '/embedded/getting-started/quick-start' },
                            ]
                        },
                    ],
                },
				{
					label: 'Reference',
                    items: [
                        {
                            label: 'Components',
                            autogenerate: { directory: '/reference/components' },
                            collapsed: true,
                        },
                        {
                            label: 'Flow Controls',
                            autogenerate: { directory: '/reference/task-dispatchers' },
                            collapsed: true,
                        }
                    ]
				},
			],
			customCss: ['./src/tailwind.css'],
		}),
		tailwind({ applyBaseStyles: false }),
	],
});
