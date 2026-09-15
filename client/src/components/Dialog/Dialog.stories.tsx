import Button from '@/components/Button/Button';
import {DialogTitle as ShadcnDialogTitle} from '@/components/ui/dialog';
import {RocketIcon} from 'lucide-react';

import {Dialog, DialogClose, DialogContent, DialogTrigger} from './Dialog';
import {DialogSidebar} from './DialogSidebar';
import {type DialogStepI, type DialogStepStatusI, DialogStepsProvider} from './DialogStepsProvider';
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
                        Sidebar children render here. DialogSteps and DialogStepIndicator go here later.
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

function getStepStateLabel({isCompleted, isReachable}: DialogStepStatusI) {
    if (isCompleted) {
        return 'completed';
    }

    if (isReachable) {
        return 'reachable';
    }

    return 'locked';
}

const StepsPreview = () => {
    const {currentStep, currentStepIndex, getStepStatus, goToNextStep, goToStep, isLastStep, isPending, steps} =
        useDialogSteps();

    return (
        <div className="flex w-[512px] max-w-full flex-col gap-4 rounded-lg bg-surface-neutral-primary p-6">
            <ShadcnDialogTitle>
                Step {currentStepIndex + 1} of {steps.length}: {currentStep.label}
            </ShadcnDialogTitle>

            <p className="text-sm text-content-neutral-secondary">
                Locked steps can&apos;t be clicked. Next validates for 600 ms, then completes the step.
            </p>

            <ol className="flex flex-col gap-1">
                {steps.map((step) => {
                    const stepStatus = getStepStatus(step.id);

                    return (
                        <li key={step.id}>
                            <button
                                aria-current={stepStatus.isCurrent ? 'step' : undefined}
                                className="flex w-full justify-between rounded-md px-3 py-2 text-sm hover:bg-surface-neutral-secondary disabled:cursor-not-allowed disabled:opacity-50 aria-[current=step]:font-semibold"
                                disabled={!stepStatus.isReachable}
                                onClick={() => goToStep(step.id)}
                                type="button"
                            >
                                <span>{step.label}</span>

                                <span className="text-content-neutral-secondary">{getStepStateLabel(stepStatus)}</span>
                            </button>
                        </li>
                    );
                })}
            </ol>

            <div className="flex justify-end gap-2">
                <DialogClose asChild>
                    <Button label="Cancel" variant="outline" />
                </DialogClose>

                <Button disabled={isPending} label={isLastStep ? 'Finish' : 'Next'} onClick={() => goToNextStep()} />
            </div>
        </div>
    );
};

export const WithSteps: Story = {
    render: () => (
        <Dialog defaultOpen>
            <DialogTrigger asChild>
                <Button label="Open dialog" />
            </DialogTrigger>

            <DialogContent aria-describedby={undefined}>
                <DialogStepsProvider steps={storySteps} validateStep={validateAfterDelay}>
                    <StepsPreview />
                </DialogStepsProvider>
            </DialogContent>
        </Dialog>
    ),
};
