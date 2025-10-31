import {Meta, StoryObj} from '@storybook/react';
import {CheckIcon, CircleIcon, TriangleAlert, XIcon} from 'lucide-react';

import Badge from './Badge';

const icons = {
    CheckIcon: <CheckIcon />,
    CircleIcon: <CircleIcon />,
    TriangleAlert: <TriangleAlert />,
    XIcon: <XIcon />,
} as const;

const meta = {
    title: 'Components/Badge',
    // eslint-disable-next-line sort-keys
    component: Badge,
    parameters: {
        controls: {
            include: ['styleType', 'weight', 'icon', 'children'],
        },
        docs: {
            description: {
                component:
                    'A customizable badge component with various style types and content options. This component supports text, icons, and custom content. Default badge is visual component and does not have hover or active states. For hover or active states, use the className prop to add custom styles.',
            },
        },
        layout: 'centered',
    },
    // eslint-disable-next-line sort-keys
    argTypes: {
        children: {
            control: {type: 'text'},
            description: 'Badge text content',
            table: {
                type: {summary: 'string'},
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
        styleType: {
            control: {type: 'select'},
            description: 'Visual style type of the badge that determines colors and appearance',
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
            description: 'Font weight of the badge text',
            options: ['regular', 'semibold'],
            table: {
                type: {summary: 'string'},
            },
        },
    },
    decorators: [
        (Story, context) => {
            const {styleType} = context.args;

            const isDestructiveVariant = styleType === 'destructive-outline' || styleType === 'warning-outline';

            return isDestructiveVariant ? (
                <div className={'rounded-md bg-surface-destructive-primary p-1.5'}>
                    <Story />
                </div>
            ) : (
                <Story />
            );
        },
    ],
    tags: ['autodocs'],
} satisfies Meta<typeof Badge>;

export default meta;
// eslint-disable-next-line @typescript-eslint/naming-convention
type Story = StoryObj<typeof Badge>;

export const DefaultBadge: Story = {
    args: {
        children: 'Label 1',
        icon: undefined,
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
    render: (args) => <Badge {...args} />,
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
            <Badge styleType="primary-filled">Primary Filled</Badge>

            <Badge styleType="primary-outline">Primary Outline</Badge>

            <Badge styleType="secondary-filled">Secondary Filled</Badge>

            <Badge styleType="secondary-outline">Secondary Outline</Badge>

            <Badge styleType="outline-outline">Outline</Badge>

            <Badge styleType="success-filled">Success Filled</Badge>

            <Badge styleType="success-outline">Success Outline</Badge>

            <Badge styleType="warning-filled">Warning Filled</Badge>

            <Badge styleType="warning-outline">Warning Outline</Badge>

            <Badge styleType="destructive-filled">Destructive Filled</Badge>

            <Badge styleType="destructive-outline">Destructive Outline</Badge>
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
            <Badge weight="regular">Regular Weight</Badge>

            <Badge weight="semibold">Semibold Weight</Badge>
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
            <Badge icon={<CheckIcon />} styleType="success-filled">
                Success
            </Badge>

            <Badge icon={<CircleIcon />} styleType="primary-outline">
                Info
            </Badge>

            <Badge icon={<TriangleAlert />} styleType="warning-filled">
                Warning
            </Badge>

            <Badge icon={<XIcon />} styleType="destructive-filled">
                Error
            </Badge>
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
                    <Badge styleType="success-filled">Online</Badge>

                    <Badge styleType="secondary-filled">Offline</Badge>

                    <Badge styleType="destructive-filled">Error</Badge>

                    <Badge styleType="warning-filled">Warning</Badge>
                </div>
            </div>

            <div className="flex flex-col gap-2">
                <h3 className="text-sm font-semibold">Categories</h3>

                <div className="flex items-center gap-2">
                    <Badge styleType="primary-outline">Frontend</Badge>

                    <Badge styleType="secondary-outline">Backend</Badge>

                    <Badge styleType="outline-outline">DevOps</Badge>
                </div>
            </div>

            <div className="flex flex-col gap-2">
                <h3 className="text-sm font-semibold">Notifications</h3>

                <div className="flex items-center gap-2">
                    <Badge icon={<CheckIcon />} styleType="success-filled">
                        Completed
                    </Badge>

                    <Badge icon={<TriangleAlert />} styleType="warning-filled">
                        Pending
                    </Badge>

                    <Badge icon={<XIcon />} styleType="destructive-filled">
                        Failed
                    </Badge>
                </div>
            </div>

            <div className="flex flex-col gap-2">
                <h3 className="text-sm font-semibold">Version & Publication Status</h3>

                <div className="flex flex-wrap items-center gap-2">
                    <Badge styleType="secondary-outline" weight="semibold">
                        V1 DRAFT
                    </Badge>

                    <Badge styleType="success-outline" weight="semibold">
                        V2 PUBLISHED
                    </Badge>

                    <Badge styleType="primary-outline" weight="semibold">
                        V2 DEPLOYED
                    </Badge>
                </div>
            </div>
        </div>
    ),
};
