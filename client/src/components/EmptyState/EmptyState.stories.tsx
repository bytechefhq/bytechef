import Button from '@/components/Button/Button';
import JourneySteps from '@/components/JourneySteps/JourneySteps';
import {Meta, StoryObj} from '@storybook/react-vite';
import {ActivityIcon, FolderIcon, Layers3Icon, Link2Icon} from 'lucide-react';

import EmptyState from './EmptyState';

const icons = {
    ActivityIcon: <ActivityIcon />,
    FolderIcon: <FolderIcon />,
    Layers3Icon: <Layers3Icon />,
    Link2Icon: <Link2Icon />,
} as const;

const meta = {
    title: 'Components/EmptyState',
    // eslint-disable-next-line sort-keys
    component: EmptyState,
    parameters: {
        controls: {
            include: ['title', 'description', 'icon', 'note'],
        },
        docs: {
            description: {
                component:
                    'First-run empty state for list pages. Says what the thing is, offers the next step the user can complete, and optionally shows progress (`children`), an environment note and a docs link. Use `EmptyFilterResult` when a filter, not an empty workspace, hides the items.',
            },
        },
        layout: 'centered',
    },
    // eslint-disable-next-line sort-keys
    argTypes: {
        description: {
            control: {type: 'text'},
            description: 'One or two sentences explaining what the entity is and why it matters.',
            table: {type: {summary: 'ReactNode'}},
        },
        icon: {
            control: {type: 'select'},
            description: 'Lucide icon rendered inside a brand-tinted tile.',
            mapping: icons,
            options: Object.keys(icons),
            table: {type: {summary: 'ReactNode'}},
        },
        note: {
            control: {type: 'text'},
            description: 'Small secondary line, e.g. which environment the page is scoped to.',
            table: {type: {summary: 'ReactNode'}},
        },
        title: {
            control: {type: 'text'},
            description: 'Rendered as a level 2 heading.',
            table: {type: {summary: 'string'}},
        },
    },
    tags: ['autodocs'],
} satisfies Meta<typeof EmptyState>;

export default meta;
// eslint-disable-next-line @typescript-eslint/naming-convention
type Story = StoryObj<typeof EmptyState>;

export const DefaultEmptyState: Story = {
    args: {
        description: 'A project groups related workflows.',
        icon: 'FolderIcon',
        title: 'Create your first project',
    },
    render: (args: React.ComponentProps<typeof EmptyState>) => (
        <EmptyState {...args} actions={<Button label="Create Project" />} />
    ),
};

export const WithJourneyAndDocs: Story = {
    parameters: {
        docs: {
            description: {
                story: 'Deployments page once a published version exists: progress strip, environment note and docs link.',
            },
        },
    },
    render: () => (
        <EmptyState
            actions={<Button label="Create Deployment" />}
            description="A deployment runs a published version of a project. Choose the version, pick which workflows to enable, and connect their accounts."
            docsLink={{
                href: 'https://docs.bytechef.io/platform/automation/deploy/workflows',
                label: 'How deployments work',
            }}
            icon={<Layers3Icon />}
            note="Deployments are separate for each environment. You're viewing Development."
            title="Create your first deployment"
        >
            <JourneySteps
                steps={[
                    {id: 'createProject', label: 'Create a project', status: 'complete'},
                    {id: 'buildWorkflow', label: 'Build and test a workflow', status: 'complete'},
                    {id: 'publish', label: 'Publish a version', status: 'complete'},
                    {id: 'deploy', label: 'Deploy and enable', status: 'current'},
                ]}
            />
        </EmptyState>
    ),
};

export const WithVerticalChecklist: Story = {
    parameters: {
        docs: {
            description: {
                story: 'Executions page before the user has a project: a vertical checklist with the action on the current step.',
            },
        },
    },
    render: () => (
        <EmptyState
            description="Runs show up here after a deployed workflow is triggered. Four steps to get there:"
            docsLink={{
                href: 'https://docs.bytechef.io/platform/automation/monitor/workflow-executions',
                label: 'How executions work',
            }}
            icon={<ActivityIcon />}
            title="No workflow runs yet"
        >
            <JourneySteps
                className="max-w-md"
                orientation="vertical"
                steps={[
                    {
                        action: <Button label="Go to Projects" size="sm" />,
                        description: 'Projects group related workflows.',
                        id: 'createProject',
                        label: 'Create a project',
                        status: 'current',
                    },
                    {id: 'buildWorkflow', label: 'Build and test a workflow', status: 'upcoming'},
                    {id: 'publish', label: 'Publish a version', status: 'upcoming'},
                    {id: 'deploy', label: 'Deploy and enable', status: 'upcoming'},
                ]}
            />
        </EmptyState>
    ),
};

export const HeaderOnly: Story = {
    parameters: {
        docs: {
            description: {
                story: 'Without actions, children, note or docs link the content section is not rendered.',
            },
        },
    },
    render: () => (
        <EmptyState
            description="A connection stores the credentials an app needs."
            icon={<Link2Icon />}
            title="Connect your first app"
        />
    ),
};
