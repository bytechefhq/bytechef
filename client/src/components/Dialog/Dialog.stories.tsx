import Button from '@/components/Button/Button';
import {DialogTitle as ShadcnDialogTitle} from '@/components/ui/dialog';
import {RocketIcon} from 'lucide-react';

import {Dialog, DialogClose, DialogContent, DialogTrigger} from './Dialog';
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

            <DialogContent aria-describedby={undefined}>
                <div className="flex w-[512px] max-w-full flex-col gap-4 rounded-lg bg-surface-neutral-primary p-6">
                    <ShadcnDialogTitle>Dialog title</ShadcnDialogTitle>

                    <p className="text-sm text-content-neutral-secondary">
                        DialogContent has no surface of its own, so this box carries the background.
                    </p>

                    <DialogClose asChild>
                        <Button label="Close" variant="outline" />
                    </DialogClose>
                </div>
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

                <div className="flex w-[512px] max-w-full flex-col gap-4 rounded-lg bg-surface-neutral-primary p-6 lg:w-auto lg:flex-1">
                    <p className="text-sm text-content-neutral-secondary">
                        From 1024 px the sidebar shows and DialogContent switches to the 860 × 648 layout. Narrower, the
                        sidebar hides, but its title and description stay in the DOM for screen readers.
                    </p>

                    <DialogClose asChild>
                        <Button label="Close" variant="outline" />
                    </DialogClose>
                </div>
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

const StepsMainPanel = () => {
    const {currentStep, goToNextStep, isLastStep, isPending} = useDialogSteps();

    return (
        <div className="flex w-[512px] max-w-full flex-col gap-4 rounded-lg bg-surface-neutral-primary p-6 lg:w-auto lg:flex-1">
            <p className="text-sm text-content-neutral-secondary">
                {currentStep.label} content. Next validates for 600 ms, then the step list and the indicator move on.
            </p>

            <div className="mt-auto flex justify-end gap-2">
                <DialogClose asChild>
                    <Button label="Cancel" variant="outline" />
                </DialogClose>

                <Button disabled={isPending} label={isLastStep ? 'Finish' : 'Next'} onClick={() => goToNextStep()} />
            </div>
        </div>
    );
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

                    <StepsMainPanel />
                </DialogStepsProvider>
            </DialogContent>
        </Dialog>
    ),
};
