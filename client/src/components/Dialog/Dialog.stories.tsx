import Button from '@/components/Button/Button';
import {PencilIcon, RocketIcon} from 'lucide-react';

import {Dialog, DialogContent, DialogTrigger} from './Dialog';
import {DialogCancelButton, DialogNextButton, DialogPreviousButton} from './DialogButtons';
import {DialogBody, DialogFooter, DialogHeader, DialogMain} from './DialogMain';
import {DialogSidebar} from './DialogSidebar';
import {DialogStepIndicator} from './DialogStepIndicator';
import {DialogSteps} from './DialogSteps';
import {type DialogStepI, DialogStepsProvider} from './DialogStepsProvider';

import type {Meta, StoryObj} from '@storybook/react-vite';

const meta = {
    component: Dialog,
    parameters: {
        layout: 'centered',
    },
    tags: ['!autodocs'],
    title: 'Components/Dialog',
} satisfies Meta<typeof Dialog>;

export default meta;

// eslint-disable-next-line @typescript-eslint/naming-convention
type Story = StoryObj<typeof meta>;

export const Default: Story = {
    render: () => (
        <Dialog defaultOpen>
            <DialogTrigger asChild>
                <Button label="Open dialog" />
            </DialogTrigger>

            <DialogContent>
                <DialogMain>
                    <DialogHeader
                        description="Update the skill instructions"
                        endContent={<Button label="Help" size="xs" variant="ghost" />}
                        icon={<PencilIcon />}
                        title="Edit Skill"
                    />

                    <DialogBody>
                        <p className="text-sm text-content-neutral-secondary">
                            Without a sidebar, DialogHeader owns the dialog title and description, so the dialog needs
                            no aria-describedby of its own. DialogCancelButton closes the dialog; Save is a plain
                            Button, because DialogNextButton only works inside a steps provider.
                        </p>
                    </DialogBody>

                    <DialogFooter>
                        <DialogCancelButton />

                        <Button label="Save" />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    ),
};

export const WithSidebar: Story = {
    render: () => (
        <Dialog defaultOpen>
            <DialogTrigger asChild>
                <Button label="Open dialog" />
            </DialogTrigger>

            <DialogContent hasSidebar>
                <DialogSidebar description="Deploy a project version" icon={<RocketIcon />} title="New Deployment">
                    <p className="text-sm text-content-neutral-secondary">
                        Sidebar children render here. The steps story puts DialogSteps and DialogStepIndicator in this
                        spot.
                    </p>
                </DialogSidebar>

                <DialogMain>
                    <DialogHeader
                        dialogDescription="Deploy a project version"
                        dialogTitle="New Deployment"
                        title="Details"
                    />

                    <DialogBody>
                        <p className="text-sm text-content-neutral-secondary">
                            DialogHeader owns the single dialog title, so dialogTitle prefixes it below 1024 px, where
                            the sidebar is hidden, and turns screen-reader only above it. From 1024 px the sidebar shows
                            and DialogContent switches to the 860 × 648 layout.
                        </p>
                    </DialogBody>

                    <DialogFooter>
                        <DialogCancelButton label="Close" />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    ),
};

const storySteps: DialogStepI[] = [
    {id: 'basics', label: 'Basics'},
    {id: 'workflows', label: 'Workflows'},
    {id: 'review', label: 'Review'},
];

const validateAfterDelay = () => new Promise<boolean>((resolve) => setTimeout(() => resolve(true), 600));

export const WithStepsAndIndicator: Story = {
    render: () => (
        <Dialog defaultOpen>
            <DialogTrigger asChild>
                <Button label="Open dialog" />
            </DialogTrigger>

            <DialogContent hasSidebar>
                <DialogStepsProvider steps={storySteps} validateStep={validateAfterDelay}>
                    <DialogSidebar description="Deploy a project version" icon={<RocketIcon />} title="New Deployment">
                        <DialogSteps />

                        <DialogStepIndicator />
                    </DialogSidebar>

                    <DialogMain>
                        <DialogHeader dialogDescription="Deploy a project version" dialogTitle="New Deployment" />

                        <DialogBody>
                            <p className="text-sm text-content-neutral-secondary">
                                DialogNextButton reads Continue until the last step, where lastStepLabel makes it
                                Deploy. Clicking it validates for 600 ms, showing a spinner and disabling itself, then
                                advances the steps and the indicator. DialogPreviousButton covers the widths where the
                                sidebar step list is hidden.
                            </p>
                        </DialogBody>

                        <DialogFooter startContent={<DialogCancelButton />}>
                            <DialogPreviousButton className="lg:hidden" />

                            <DialogNextButton lastStepLabel="Deploy" />
                        </DialogFooter>
                    </DialogMain>
                </DialogStepsProvider>
            </DialogContent>
        </Dialog>
    ),
};
