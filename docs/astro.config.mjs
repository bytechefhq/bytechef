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
			social: {
                discord: 'https://discord.gg/JcNSqJ7vK8',
				github: 'https://github.com/bytechefhq/bytechef',
                twitter: 'https://twitter.com/bytechefhq',
			},
            editLink: {
                baseUrl: 'https://github.com/bytechefhq/bytechef/edit/master/docs/',
            },
			sidebar: [
				{
					label: 'Guides',
					items: [
						// Each item here is one entry in the navigation menu.
						{ label: 'Example Guide', link: '/guides/example/' },
					],
				},
				{
					label: 'Reference',
					autogenerate: { directory: 'reference' },
				},
			],
			customCss: ['./src/tailwind.css'],
		}),
		tailwind({ applyBaseStyles: false }),
	],
});
