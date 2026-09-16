import Button from '@/components/Button/Button';
import {PencilIcon, RocketIcon} from 'lucide-react';

import {Dialog, DialogClose, DialogContent, DialogTrigger} from './Dialog';
import {DialogBody, DialogFooter, DialogHeader, DialogMain} from './DialogMain';
import {DialogSidebar} from './DialogSidebar';
import {DialogStepIndicator} from './DialogStepIndicator';
import {DialogSteps} from './DialogSteps';
import {type DialogStepI, DialogStepsProvider} from './DialogStepsProvider';
import {useDialogSteps} from './hooks/useDialogSteps';

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
                            no aria-describedby of its own. The body scrolls; the header and footer stay put.
                        </p>
                    </DialogBody>

                    <DialogFooter>
                        <DialogClose asChild>
                            <Button label="Cancel" variant="outline" />
                        </DialogClose>

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

            <DialogContent>
                <DialogSidebar description="Deploy a project version" icon={<RocketIcon />} title="New Deployment">
                    <p className="text-sm text-content-neutral-secondary">
                        Sidebar children render here. The steps story puts DialogSteps and DialogStepIndicator in this
                        spot.
                    </p>
                </DialogSidebar>

                <DialogMain>
                    <DialogHeader title="Details" />

                    <DialogBody>
                        <p className="text-sm text-content-neutral-secondary">
                            The sidebar owns the dialog title, so this header renders a plain h2 instead of a second
                            one. From 1024 px the sidebar shows and DialogContent switches to the 860 × 648 layout.
                        </p>
                    </DialogBody>

                    <DialogFooter>
                        <DialogClose asChild>
                            <Button label="Close" variant="outline" />
                        </DialogClose>
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

const WizardNextButton = () => {
    const {goToNextStep, isLastStep, isPending} = useDialogSteps();

    return <Button disabled={isPending} label={isLastStep ? 'Finish' : 'Next'} onClick={() => goToNextStep()} />;
};

export const WithStepsAndIndicator: Story = {
    render: () => (
        <Dialog defaultOpen>
            <DialogTrigger asChild>
                <Button label="Open dialog" />
            </DialogTrigger>

            <DialogContent>
                <DialogStepsProvider steps={storySteps} validateStep={validateAfterDelay}>
                    <DialogSidebar description="Deploy a project version" icon={<RocketIcon />} title="New Deployment">
                        <DialogSteps />

                        <DialogStepIndicator />
                    </DialogSidebar>

                    <DialogMain>
                        <DialogHeader />

                        <DialogBody>
                            <p className="text-sm text-content-neutral-secondary">
                                The header title defaults to the current step label, and focus moves to it on every step
                                change. Below 1024 px it also shows the step position, because the sidebar is hidden
                                there.
                            </p>
                        </DialogBody>

                        <DialogFooter
                            startContent={
                                <DialogClose asChild>
                                    <Button label="Cancel" variant="ghost" />
                                </DialogClose>
                            }
                        >
                            <WizardNextButton />
                        </DialogFooter>
                    </DialogMain>
                </DialogStepsProvider>
            </DialogContent>
        </Dialog>
    ),
};
