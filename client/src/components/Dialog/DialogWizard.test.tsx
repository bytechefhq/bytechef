import {render, screen, userEvent} from '@/shared/util/test-utils';
import {RocketIcon} from 'lucide-react';
import {describe, expect, it, vi} from 'vitest';

import {Dialog, DialogContent} from './Dialog';
import {DialogNextButton} from './DialogButtons';
import {DialogFooter} from './DialogMain';
import {type DialogStepI} from './DialogStepsProvider';
import {DialogWizard} from './DialogWizard';
import {useDialogSteps} from './hooks/useDialogSteps';

const steps: DialogStepI[] = [
    {id: 'basic', label: 'Basic'},
    {id: 'workflows', label: 'Workflows'},
];

const StepContent = () => {
    const {currentStep} = useDialogSteps();

    return <p>{`Content of ${currentStep.label}`}</p>;
};

function renderWizard(onComplete = vi.fn()) {
    render(
        <Dialog open>
            <DialogContent aria-describedby={undefined} hasSidebar>
                <DialogWizard
                    description="Deploy a project version."
                    footer={
                        <DialogFooter>
                            <DialogNextButton label="Next" lastStepLabel="Save" />
                        </DialogFooter>
                    }
                    icon={<RocketIcon data-testid="wizard-icon" />}
                    onComplete={onComplete}
                    steps={steps}
                    title="New Deployment"
                >
                    <StepContent />
                </DialogWizard>
            </DialogContent>
        </Dialog>
    );

    return onComplete;
}

describe('DialogWizard', () => {
    it('should render the sidebar with the icon, title, description, steps and progress', () => {
        renderWizard();

        expect(screen.getByTestId('wizard-icon')).toBeInTheDocument();
        expect(screen.getByText('New Deployment')).toBeInTheDocument();
        expect(screen.getByRole('navigation', {name: 'Steps'})).toHaveTextContent('BasicWorkflows');
        expect(screen.getByRole('progressbar', {name: 'Step 1 of 2'})).toBeInTheDocument();
    });

    it('should title the dialog with the wizard title and the current step', () => {
        renderWizard();

        expect(screen.getByRole('dialog', {name: 'New Deployment - Basic'})).toBeInTheDocument();
        expect(screen.getByText('Content of Basic')).toBeInTheDocument();
    });

    it('should advance through the steps and complete on the last one', async () => {
        const onComplete = renderWizard();

        await userEvent.click(screen.getByRole('button', {name: 'Next'}));

        expect(screen.getByText('Content of Workflows')).toBeInTheDocument();
        expect(screen.getByRole('progressbar', {name: 'Step 2 of 2'})).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(onComplete).toHaveBeenCalledTimes(1);
    });
});
