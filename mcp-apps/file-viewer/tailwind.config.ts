import type {Config} from 'tailwindcss';

// Only the design tokens the read-only workflow graph actually uses. Values are CSS
// variables declared in src/app.css, mirroring the ByteChef light theme.
export default {
    content: ['./index.html', './src/**/*.{ts,tsx}'],
    theme: {
        extend: {
            colors: {
                'content-neutral-secondary': 'hsl(var(--content-neutral-secondary))',
                'stroke-neutral-secondary': 'hsl(var(--stroke-neutral-secondary))',
                'stroke-neutral-tertiary': 'hsl(var(--stroke-neutral-tertiary))',
                'surface-neutral-primary': 'hsl(var(--surface-neutral-primary))',
                'surface-neutral-primary-hover': 'hsl(var(--surface-neutral-primary-hover))',
            },
        },
    },
} satisfies Config;
