/**
 * Restricted imports configuration for @typescript-eslint/no-restricted-imports
 *
 * This file defines which imports should be restricted and what alternatives
 * should be used instead. Add new restrictions here as needed.
 */

export const restrictedImports = {
    paths: [
        {
            name: '@/components/ui/button',
            importNames: ['Button'],
            message: "Import Button from '@/components/Button/Button' instead.",
            allowTypeImports: true,
        },
        {
            name: '@/components/ui/badge',
            importNames: ['Badge'],
            message: "Import Badge from '@/components/Badge/Badge' instead.",
            allowTypeImports: true,
        },
        {
            name: '@/components/ui/switch',
            importNames: ['Switch'],
            message: "Import Switch from '@/components/Switch/Switch' instead.",
            allowTypeImports: true,
        },
        {
            name: '@/components/ui/select',
            message:
                "Import Select primitives from '@/components/Select/Select' instead. The wrapper defaults SelectContent to position=\"popper\" so the dropdown renders below the trigger at the trigger width.",
            allowTypeImports: true,
        },
    ],
};
