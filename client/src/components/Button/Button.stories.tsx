/* eslint-disable sort-keys */
import {Meta, StoryObj} from '@storybook/react';
import {CircleIcon, Download, PlusIcon, Save, SaveIcon, Settings, Trash2, XIcon} from 'lucide-react';

import Button from './Button';

const ICON_SIZES = ['icon', 'iconSm', 'iconXs', 'iconXxs'] as const;

const icons = {
    CircleIcon: <CircleIcon />,
    PlusIcon: <PlusIcon />,
    XIcon: <XIcon />,
} as const;

const labels = {
    'Label 1': 'Button',
    'Label 2': 'Publish',
    'Label 3': 'Deploy',
};

const customContent = {
    'Example 1': (
        <div className="flex items-center gap-2">
            <span className="font-bold">All</span>

            <div className="rounded-md bg-background px-1.5 py-0.5 opacity-75">
                <span className="text-content-brand-primary">110</span>
            </div>
        </div>
    ),
    'Example 2': (
        <>
            <span className="font-bold">Save file</span>

            <SaveIcon />
        </>
    ),
};

const meta = {
    title: 'Components/Button',
    component: Button,
    parameters: {
        controls: {
            include: ['size', 'variant', 'label', 'icon', 'children', 'disabled'],
        },
        docs: {
            description: {
                component:
                    'A customizable button component with various sizes and variants. This component supports text, icons and custom content.',
            },
        },
        layout: 'centered',
    },
    argTypes: {
        children: {
            control: {type: 'select'},
            defaultValue: 'no-content',
            description:
                'Custom content displayed inside the button - **only** applied when no label is provided and **cannot** be used with icon button sizes.',
            if: {arg: 'size', neq: ICON_SIZES as unknown as string[]},
            mapping: {'no-content': undefined, ...customContent},
            options: ['no-content', ...Object.keys(customContent)],
            table: {
                type: {summary: 'ReactNode'},
            },
        },
        disabled: {
            control: {type: 'boolean'},
            description: 'Flag to indicate whether the button is disabled',
            table: {
                type: {summary: 'boolean'},
            },
        },
        icon: {
            control: {type: 'select'},
            description:
                'Icon displayed on the button - can be any icon sent as a `ReactElement` through the `icon` prop and can be combined with label or custom content.',
            mapping: {'no-icon': undefined, ...icons},
            options: ['no-icon', ...Object.keys(icons)],
            table: {
                type: {summary: 'ReactElement'},
            },
        },
        label: {
            control: {type: 'select'},
            description:
                'Button text content - **always** takes precedence over custom content and cannot be used with icon button sizes.',
            if: {arg: 'size', neq: ICON_SIZES as unknown as string[]},
            mapping: {'no-label': undefined, ...labels},
            options: ['no-label', ...Object.keys(labels)],
            table: {
                type: {summary: 'string'},
            },
        },
        size: {
            control: {type: 'select'},
            description:
                'Size variant of the button - icon sizes are to be used along with the `icon` prop and **cannot** be used with label or custom content.',
            options: ['xxs', 'xs', 'sm', 'default', 'lg', ...ICON_SIZES],
            table: {
                type: {summary: 'string'},
            },
        },
        variant: {
            control: {type: 'select'},
            description: 'Visual variant of the button that can be applied to all button types',
            options: [
                'default',
                'secondary',
                'destructive',
                'destructiveGhost',
                'destructiveOutline',
                'outline',
                'ghost',
                'link',
            ],
            table: {
                type: {summary: 'string'},
            },
        },
    },
    tags: ['autodocs'],
} satisfies Meta<typeof Button>;

export default meta;
// eslint-disable-next-line @typescript-eslint/naming-convention
type Story = StoryObj<typeof Button>;

export const DefaultButton: Story = {
    render: (args) => <Button {...args} />,

    args: {
        size: 'default',
        variant: 'default',
        label: 'Label 1',
        children: undefined,
        icon: undefined,
        disabled: false,
    },
    parameters: {
        docs: {
            description: {
                story: 'Default button component with label.',
            },
        },
    },
};

export const TextButtonSizeVariants: Story = {
    render: () => (
        <div className="flex items-center gap-4">
            <Button label="Large" size="lg" />

            <Button label="Default" size="default" />

            <Button label="sm" size="sm" />

            <Button label="xs" size="xs" />

            <Button label="xxs" size="xxs" />
        </div>
    ),
    parameters: {
        docs: {
            description: {
                story: 'Different size variants of the button component that can be used either with label or with custom content, and can be used with or without an icon.',
            },
        },
    },
};

export const IconButtonSizeVariants: Story = {
    render: () => (
        <div className="flex items-center gap-4">
            <Button icon={<CircleIcon />} size="icon" />

            <Button icon={<CircleIcon />} size="iconSm" />

            <Button icon={<CircleIcon />} size="iconXs" />

            <Button icon={<CircleIcon />} size="iconXxs" />
        </div>
    ),
    parameters: {
        docs: {
            description: {
                story: 'Icon button size variants that are to be used without label or custom content and always with an icon.',
            },
        },
    },
};

export const TextButtonStyleVariants: Story = {
    render: () => (
        <div className="flex flex-wrap items-center gap-4">
            <Button label="Default" size="sm" variant="default" />

            <Button label="Secondary" size="sm" variant="secondary" />

            <Button label="Destructive" size="sm" variant="destructive" />

            <Button label="Destructive Ghost" size="sm" variant="destructiveGhost" />

            <div className="rounded-md bg-surface-destructive-primary p-1.5">
                <Button label="Destructive Outline" size="sm" variant="destructiveOutline" />
            </div>

            <Button label="Outline" size="sm" variant="outline" />

            <Button label="Ghost" size="sm" variant="ghost" />

            <Button label="Link" size="sm" variant="link" />
        </div>
    ),
    parameters: {
        docs: {
            description: {
                story: 'All text button styles available through the variant prop.',
            },
        },
    },
};

export const IconButtonStyleVariants: Story = {
    render: () => (
        <div className="flex flex-wrap items-center gap-4">
            <Button icon={<CircleIcon />} size="icon" variant="default" />

            <Button icon={<CircleIcon />} size="icon" variant="secondary" />

            <Button icon={<CircleIcon />} size="icon" variant="destructive" />

            <Button icon={<CircleIcon />} size="icon" variant="destructiveGhost" />

            <div className="rounded-md bg-surface-destructive-primary p-1.5">
                <Button icon={<CircleIcon />} size="icon" variant="destructiveOutline" />
            </div>

            <Button icon={<CircleIcon />} size="icon" variant="outline" />

            <Button icon={<CircleIcon />} size="icon" variant="ghost" />

            <Button icon={<CircleIcon />} size="icon" variant="link" />
        </div>
    ),
    parameters: {
        docs: {
            description: {
                story: 'All icon button styles available through the variant prop.',
            },
        },
    },
};

export const TextButtonsWithDifferentIcons: Story = {
    render: () => (
        <div className="flex flex-wrap items-center gap-4">
            <Button icon={<Save />} label="Save" size="lg" />

            <Button icon={<Download />} label="Download" size="default" variant="secondary" />

            <Button icon={<PlusIcon />} label="Add Item" size="sm" variant="outline" />

            <Button icon={<Trash2 />} label="Delete item" size="xs" variant="destructiveGhost" />

            <Button icon={<Settings />} label="Settings" size="xxs" variant="outline" />
        </div>
    ),
    parameters: {
        docs: {
            description: {
                story: 'Different sizes and variants of text buttons with various icons passed as a prop.',
            },
        },
    },
};

export const TextButtonsDisabledStates: Story = {
    render: () => (
        <div className="flex flex-wrap items-center gap-4">
            <Button disabled label="Default" />

            <Button disabled label="Secondary" variant="secondary" />

            <Button disabled label="Destructive" variant="destructive" />

            <Button disabled label="Destructive Ghost" variant="destructiveGhost" />

            <div className="rounded-md bg-surface-destructive-primary p-1.5">
                <Button disabled label="Destructive Outline" variant="destructiveOutline" />
            </div>

            <Button disabled label="Outline" variant="outline" />

            <Button disabled label="Ghost" variant="ghost" />

            <Button disabled label="Link" variant="link" />
        </div>
    ),
    parameters: {
        docs: {
            description: {
                story: 'Disabled state for different text button variants.',
            },
        },
    },
};

export const IconButtonsDisabledStates: Story = {
    render: () => (
        <div className="flex flex-wrap items-center gap-4">
            <Button disabled icon={<CircleIcon />} />

            <Button disabled icon={<CircleIcon />} variant="secondary" />

            <Button disabled icon={<CircleIcon />} variant="destructive" />

            <Button disabled icon={<CircleIcon />} variant="destructiveGhost" />

            <div className="rounded-md bg-surface-destructive-primary p-1.5">
                <Button disabled icon={<CircleIcon />} variant="destructiveOutline" />
            </div>

            <Button disabled icon={<CircleIcon />} variant="outline" />

            <Button disabled icon={<CircleIcon />} variant="ghost" />

            <Button disabled icon={<CircleIcon />} variant="link" />
        </div>
    ),
    parameters: {
        docs: {
            description: {
                story: 'Disabled state for different icon button variants.',
            },
        },
    },
};
