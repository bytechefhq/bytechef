import {Meta, StoryObj} from '@storybook/react-vite';
import {CheckIcon, CircleIcon, TriangleAlert, XIcon} from 'lucide-react';

import Badge from './Badge';

const icons = {
    CheckIcon: <CheckIcon />,
    CircleIcon: <CircleIcon />,
    TriangleAlert: <TriangleAlert />,
    XIcon: <XIcon />,
} as const;

const labels = {
    'Label 1': 'Badge',
    'Label 2': 'Online',
    'Label 3': 'Published',
    'Label 4': 'Draft',
} as const;

const customContent = {
    'Example 1': (
        <>
            <span className="font-semibold">Custom</span>

            <span>Content</span>
        </>
    ),
    'Example 2': <span>Active</span>,
    'Example 3': (
        <>
            <span>State: </span>

            <span className="font-semibold">deployed</span>
        </>
    ),
} as const;

const meta = {
    title: 'Components/Badge',
    // eslint-disable-next-line sort-keys
    component: Badge,
    parameters: {
        controls: {
            include: ['styleType', 'weight', 'icon', 'label', 'children', 'aria-label'],
        },
        docs: {
            description: {
                component:
                    'A customizable badge component with various style types (colors/categories) and font weight options. This component supports text labels, icons, and custom content. Badge has two weight options: regular and semibold. Style types define the color categories (primary, secondary, success, warning, destructive, etc.) with filled and outline variants. Default badge is a visual component and does not have hover or active states. For hover or active states, use the className prop to add custom styles.',
            },
        },
        layout: 'centered',
    },
    // eslint-disable-next-line sort-keys
    argTypes: {
        'aria-label': {
            control: {type: 'text'},
            description:
                'Accessible label for the badge. **Required** when using icon-only badges (when `icon` is provided without `label` or `children`). This ensures screen readers can properly identify the badge content. Not allowed for text-only or icon+text badges as the text content serves as the accessible name.',
            table: {
                type: {summary: 'string'},
            },
        },
        children: {
            control: {type: 'select'},
            defaultValue: 'no-content',
            description: 'Custom content displayed inside the badge - **only** applied when no label is provided.',
            mapping: {'no-content': undefined, ...customContent},
            options: ['no-content', ...Object.keys(customContent)],
            table: {
                type: {summary: 'ReactNode'},
            },
        },
        icon: {
            control: {type: 'select'},
            description:
                'Icon displayed on the badge - can be any icon sent as a `ReactElement` through the `icon` prop and can be combined with label or custom content.',
            mapping: {'no-icon': undefined, ...icons},
            options: ['no-icon', ...Object.keys(icons)],
            table: {
                type: {summary: 'ReactElement'},
            },
        },
        label: {
            control: {type: 'select'},
            description: 'Badge text content - **always** takes precedence over custom content.',
            mapping: {'no-label': undefined, ...labels},
            options: ['no-label', ...Object.keys(labels)],
            table: {
                type: {summary: 'string'},
            },
        },
        styleType: {
            control: {type: 'select'},
            description: 'Color category/style type of the badge that determines colors and appearance',
            options: [
                'primary-filled',
                'primary-outline',
                'secondary-filled',
                'secondary-outline',
                'outline-outline',
                'success-filled',
                'success-outline',
                'warning-filled',
                'warning-outline',
                'destructive-filled',
                'destructive-outline',
            ],
            table: {
                type: {summary: 'string'},
            },
        },
        weight: {
            control: {type: 'select'},
            description: 'Font weight of the badge text - regular or semibold',
            options: ['regular', 'semibold'],
            table: {
                type: {summary: 'string'},
            },
        },
    },
    tags: ['autodocs'],
} satisfies Meta<typeof Badge>;

export default meta;
// eslint-disable-next-line @typescript-eslint/naming-convention
type Story = StoryObj<typeof Badge>;

export const DefaultBadge: Story = {
    args: {
        children: undefined,
        icon: undefined,
        label: 'Label 1',
        styleType: 'primary-filled',
        weight: 'regular',
    },
    parameters: {
        docs: {
            description: {
                story: 'Default badge component with label.',
            },
        },
    },
    render: (args: React.ComponentProps<typeof Badge>) => <Badge {...args} />,
};

export const BadgeStyleVariants: Story = {
    parameters: {
        docs: {
            description: {
                story: 'All badge style types available through the styleType prop.',
            },
        },
    },
    render: () => (
        <div className="grid grid-cols-6 items-center gap-4">
            <Badge label="Primary Filled" styleType="primary-filled" />

            <Badge label="Primary Outline" styleType="primary-outline" />

            <Badge label="Secondary Filled" styleType="secondary-filled" />

            <Badge label="Secondary Outline" styleType="secondary-outline" />

            <Badge label="Outline" styleType="outline-outline" />

            <Badge label="Success Filled" styleType="success-filled" />

            <Badge label="Success Outline" styleType="success-outline" />

            <Badge label="Warning Filled" styleType="warning-filled" />

            <Badge label="Warning Outline" styleType="warning-outline" />

            <Badge label="Destructive Filled" styleType="destructive-filled" />

            <Badge label="Destructive Outline" styleType="destructive-outline" />
        </div>
    ),
};

export const BadgeWeightVariants: Story = {
    parameters: {
        docs: {
            description: {
                story: 'Different font weight variants of the badge component.',
            },
        },
    },
    render: () => (
        <div className="flex items-center gap-4">
            <Badge label="Regular Weight" weight="regular" />

            <Badge label="Semibold Weight" weight="semibold" />
        </div>
    ),
};

export const BadgesWithIcons: Story = {
    parameters: {
        docs: {
            description: {
                story: 'Different style types of badges with various icons.',
            },
        },
    },
    render: () => (
        <div className="flex flex-wrap items-center gap-4">
            <Badge icon={<CheckIcon />} label="Success" styleType="success-filled" />

            <Badge icon={<CircleIcon />} label="Info" styleType="primary-outline" />

            <Badge icon={<TriangleAlert />} label="Warning" styleType="warning-filled" />

            <Badge icon={<XIcon />} label="Error" styleType="destructive-filled" />
        </div>
    ),
};

export const IconOnlyBadges: Story = {
    parameters: {
        docs: {
            description: {
                story: 'Icon-only badges using the icon content type.',
            },
        },
    },
    render: () => (
        <div className="flex items-center gap-4">
            <Badge aria-label="Success" icon={<CheckIcon />} styleType="success-filled" />

            <Badge aria-label="Info" icon={<CircleIcon />} styleType="primary-outline" />

            <Badge aria-label="Warning" icon={<TriangleAlert />} styleType="warning-filled" />

            <Badge aria-label="Error" icon={<XIcon />} styleType="destructive-filled" />
        </div>
    ),
};

export const BadgeUseCases: Story = {
    parameters: {
        docs: {
            description: {
                story: 'Common use cases for badges in applications.',
            },
        },
    },
    render: () => (
        <div className="flex flex-col gap-6">
            <div className="flex flex-col gap-2">
                <h3 className="text-sm font-semibold">Status Indicators</h3>

                <div className="flex items-center gap-2">
                    <Badge label="Online" styleType="success-filled" />

                    <Badge label="Offline" styleType="secondary-filled" />

                    <Badge label="Error" styleType="destructive-filled" />

                    <Badge label="Warning" styleType="warning-filled" />
                </div>
            </div>

            <div className="flex flex-col gap-2">
                <h3 className="text-sm font-semibold">Categories</h3>

                <div className="flex items-center gap-2">
                    <Badge label="Frontend" styleType="primary-outline" />

                    <Badge label="Backend" styleType="secondary-outline" />

                    <Badge label="DevOps" styleType="outline-outline" />
                </div>
            </div>

            <div className="flex flex-col gap-2">
                <h3 className="text-sm font-semibold">Notifications</h3>

                <div className="flex items-center gap-2">
                    <Badge icon={<CheckIcon />} label="Completed" styleType="success-filled" />

                    <Badge icon={<TriangleAlert />} label="Pending" styleType="warning-filled" />

                    <Badge icon={<XIcon />} label="Failed" styleType="destructive-filled" />
                </div>
            </div>

            <div className="flex flex-col gap-2">
                <h3 className="text-sm font-semibold">Version & Publication Status</h3>

                <div className="flex flex-wrap items-center gap-2">
                    <Badge label="V1 DRAFT" styleType="secondary-outline" weight="semibold" />

                    <Badge label="V2 PUBLISHED" styleType="success-outline" weight="semibold" />

                    <Badge label="V2 DEPLOYED" styleType="primary-outline" weight="semibold" />

                    <Badge label="V3 FAILED" styleType="destructive-outline" weight="semibold" />
                </div>
            </div>
        </div>
    ),
};

export const BadgesWithCustomChildren: Story = {
    parameters: {
        docs: {
            description: {
                story: 'Badges with custom children (divs, spans, etc.) instead of simple text labels.',
            },
        },
    },
    render: () => (
        <div className="flex flex-wrap items-center gap-4">
            <Badge styleType="primary-filled">
                <span className="font-semibold">Custom</span>

                <span>Content</span>
            </Badge>

            <Badge icon={<CheckIcon />} styleType="success-filled">
                <span>Active</span>
            </Badge>

            <Badge styleType="secondary-outline">
                <span>State: </span>

                <span className="font-semibold">deployed</span>
            </Badge>
        </div>
    ),
};
