import colors from 'tailwindcss/colors';
import starlightPlugin from '@astrojs/starlight-tailwind';

/** @type {import('tailwindcss').Config} */
export default {
	content: ['./src/**/*.{astro,html,js,jsx,md,mdx,svelte,ts,tsx,vue}'],
	theme: {
		extend: {
			colors: {
				// Your preferred accent color. Indigo is closest to Starlight’s defaults.
				accent: colors.indigo,
				// Your preferred gray scale. Zinc is closest to Starlight’s defaults.
				gray: colors.zinc,
			},
            fontFamily: {
                // Your preferred text font. Starlight uses a system font stack by default.
                sans: ['"Atkinson Hyperlegible"'],
                // Your preferred code font. Starlight uses system monospace fonts by default.
                mono: ['"IBM Plex Mono"'],
            },
		},
	},
	plugins: [starlightPlugin()],
};
