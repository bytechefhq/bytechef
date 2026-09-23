import SelectedConnectionsList from '@/ee/pages/embedded/automation-hub/wizard/SelectedConnectionsList';
import {ActivationStateI} from '@/ee/pages/embedded/automation-hub/wizard/activationReducer';
import {AutomationWorkflowProjectWorkflowTemplate} from '@/ee/shared/middleware/embedded/public';
import {CircleCheckIcon} from 'lucide-react';

interface ActivateStepProps {
    busy: boolean;
    state: ActivationStateI;
    template: AutomationWorkflowProjectWorkflowTemplate;
}

const ActivateStep = ({busy, state, template}: ActivateStepProps) => {
    if (state.step === 'done') {
        return (
            <div className="flex flex-col items-center gap-2 py-8 text-center">
                <CircleCheckIcon className="size-8 text-(--hub-enable)" />

                <p className="text-base font-medium">Your automation is running</p>

                <p className="text-sm text-muted-foreground">{template.label} is now active.</p>
            </div>
        );
    }

    return (
        <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-1">
                <h3 className="text-sm font-medium">Automation</h3>

                <p className="text-sm text-muted-foreground">{template.label}</p>

                {template.description && <p className="text-sm text-muted-foreground">{template.description}</p>}
            </div>

            {state.requiredComponents.length > 0 && (
                <div className="flex flex-col gap-2">
                    <h3 className="text-sm font-medium">Connected accounts</h3>

                    <SelectedConnectionsList state={state} template={template} />
                </div>
            )}

            {busy && <p className="text-sm text-muted-foreground">Setting up your automation…</p>}
        </div>
    );
};

export default ActivateStep;
