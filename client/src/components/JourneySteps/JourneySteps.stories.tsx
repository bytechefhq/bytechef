import Button from '@/components/Button/Button';
import {Meta, StoryObj} from '@storybook/react-vite';

import JourneySteps, {type JourneyStepI} from './JourneySteps';

const steps: JourneyStepI[] = [
    {id: 'createProject', label: 'Create a project', status: 'complete'},
    {id: 'buildWorkflow', label: 'Build and test a workflow', status: 'complete'},
    {id: 'publish', label: 'Publish a version', status: 'current'},
    {id: 'deploy', label: 'Deploy and enable', status: 'upcoming'},
];

const meta = {
    title: 'Components/JourneySteps',
    // eslint-disable-next-line sort-keys
    component: JourneySteps,
    parameters: {
        controls: {
            include: ['orientation'],
        },
        docs: {
            description: {
                component:
                    'Ordered progress indicator for a multi-step journey. Each step is complete, current or upcoming; the current step carries `aria-current="step"` and complete steps announce themselves as completed. Horizontal for a compact strip, vertical for a checklist with per-step actions.',
            },
        },
        layout: 'centered',
    },
    // eslint-disable-next-line sort-keys
    argTypes: {
        orientation: {
            control: {type: 'select'},
            description: 'Horizontal strip or vertical checklist.',
            options: ['horizontal', 'vertical'],
            table: {type: {summary: 'string'}},
        },
    },
    tags: ['autodocs'],
} satisfies Meta<typeof JourneySteps>;

export default meta;
// eslint-disable-next-line @typescript-eslint/naming-convention
type Story = StoryObj<typeof JourneySteps>;

export const Horizontal: Story = {
    args: {
        orientation: 'horizontal',
        steps,
    },
};

export const Vertical: Story = {
    parameters: {
        docs: {
            description: {
                story: 'Vertical checklist with a description and an action on the current step.',
            },
        },
    },
    render: () => (
        <JourneySteps
            className="w-96"
            orientation="vertical"
            steps={steps.map((step) =>
                step.status === 'current'
                    ? {
                          ...step,
                          action: <Button label="Open Lead sync" size="sm" />,
                          description: 'Publishing creates a read-only version you can deploy.',
                      }
                    : step
            )}
        />
    ),
};
