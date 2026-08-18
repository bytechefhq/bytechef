import Button from '@/components/Button/Button';
import LoadingDots from '@/components/LoadingDots';
import SelectedConnectionsList from '@/ee/pages/embedded/automation-hub/wizard/SelectedConnectionsList';
import {ActivationStateI} from '@/ee/pages/embedded/automation-hub/wizard/activationReducer';
import {AutomationWorkflowProjectWorkflowTemplate} from '@/ee/shared/middleware/embedded/public';
import {useEffect, useRef} from 'react';

interface ConfigureStepProps {
    busy: boolean;
    onConfigure: () => void;
    onRetryWiring: () => void;
    state: ActivationStateI;
    template: AutomationWorkflowProjectWorkflowTemplate;
    wiringComplete: boolean;
    wiringFailed: boolean;
}

/**
 * Step 2: runs the copy (COPY) or the provision (REFERENCE) the moment it is shown, then reports
 * what the automation ended up with. There is deliberately no inputs form in v1 — the public
 * frontend API has no endpoint that persists per-user workflow input values, so inputs are
 * configured in the builder after activation (spec §4 and §8).
 *
 * A COPY also has to have its connections wired onto the copied workflow here, and the step stays
 * un-advanceable until that settles — publishing an unwired copy produces a deployment that only
 * fails at run time.
 */
const ConfigureStep = ({
    busy,
    onConfigure,
    onRetryWiring,
    state,
    template,
    wiringComplete,
    wiringFailed,
}: ConfigureStepProps) => {
    const configureStartedRef = useRef(false);

    useEffect(() => {
        if (state.workflowUuid || state.error || configureStartedRef.current) {
            return;
        }

        configureStartedRef.current = true;

        onConfigure();
    }, [onConfigure, state.error, state.workflowUuid]);

    if (!state.workflowUuid) {
        return (
            <div className="flex flex-col items-center gap-3 py-8" data-testid="configure-step-pending">
                {state.error && !busy ? (
                    <Button label="Try again" onClick={onConfigure} variant="outline" />
                ) : (
                    <LoadingDots />
                )}
            </div>
        );
    }

    return (
        <div className="flex flex-col gap-4">
            {template.description && <p className="text-sm text-muted-foreground">{template.description}</p>}

            {state.requiredComponents.length > 0 && (
                <div className="flex flex-col gap-2">
                    <h3 className="text-sm font-medium">Connected accounts</h3>

                    <SelectedConnectionsList state={state} template={template} />
                </div>
            )}

            {wiringFailed && !busy ? (
                <Button label="Try again" onClick={onRetryWiring} variant="outline" />
            ) : (
                <p className="text-sm text-muted-foreground">
                    {wiringComplete
                        ? 'You can fine-tune this automation in the builder once it is running.'
                        : 'Connecting your accounts…'}
                </p>
            )}
        </div>
    );
};

export default ConfigureStep;
