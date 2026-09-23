import Button from '@/components/Button/Button';
import LoadingDots from '@/components/LoadingDots';
import {Alert, AlertDescription} from '@/components/ui/alert';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {useAutomationHubStore} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';
import ActivateStep from '@/ee/pages/embedded/automation-hub/wizard/ActivateStep';
import ConfigureStep from '@/ee/pages/embedded/automation-hub/wizard/ConfigureStep';
import ConnectAccountsStep from '@/ee/pages/embedded/automation-hub/wizard/ConnectAccountsStep';
import {ActivationStepType, canProceed} from '@/ee/pages/embedded/automation-hub/wizard/activationReducer';
import {useActivationFlow, useRequiredComponents} from '@/ee/pages/embedded/automation-hub/wizard/useActivationFlow';
import {AutomationWorkflowProjectWorkflowTemplate} from '@/ee/shared/middleware/embedded/public';
import {useMemo} from 'react';

const REQUIRED_COMPONENTS_ERROR_MESSAGE = 'This automation could not be set up. Please try again.';

const STEPS: {label: string; step: ActivationStepType}[] = [
    {label: 'Connect', step: 'connect'},
    {label: 'Configure', step: 'configure'},
    {label: 'Activate', step: 'activate'},
];

interface ActivationWizardContentProps {
    onClose: () => void;
    requiredComponents: string[];
    template: AutomationWorkflowProjectWorkflowTemplate;
}

const ActivationWizardContent = ({onClose, requiredComponents, template}: ActivationWizardContentProps) => {
    const {activate, busy, dispatch, editWorkflow, openInBuilder, state} = useActivationFlow(
        template,
        requiredComponents
    );

    const steps = useMemo(
        () =>
            STEPS.filter(
                ({step}) =>
                    (step !== 'connect' || requiredComponents.length > 0) &&
                    (step !== 'configure' || state.inputs.length > 0)
            ),
        [requiredComponents.length, state.inputs.length]
    );

    const errorShown = !!state.error && !state.highlightedComponent && !busy;

    const editWorkflowAllowed = useAutomationHubStore((state) => state.editWorkflowAllowed);

    const backShown = state.step !== 'connect' && steps[0]?.step !== state.step;

    return (
        <>
            {errorShown && (
                <Alert variant="destructive">
                    <AlertDescription>{state.error}</AlertDescription>
                </Alert>
            )}

            {state.step === 'connect' && <ConnectAccountsStep dispatch={dispatch} state={state} template={template} />}

            {state.step === 'configure' && <ConfigureStep dispatch={dispatch} state={state} />}

            {(state.step === 'activate' || state.step === 'done') && (
                <ActivateStep busy={busy} state={state} template={template} />
            )}

            <DialogFooter>
                {state.step === 'done' ? (
                    <>
                        {editWorkflowAllowed && (
                            <Button label="Open in builder" onClick={openInBuilder} variant="outline" />
                        )}

                        <Button label="Done" onClick={onClose} />
                    </>
                ) : (
                    <>
                        {editWorkflowAllowed && (
                            <Button disabled={busy} label="Edit workflow" onClick={editWorkflow} variant="outline" />
                        )}

                        {backShown && (
                            <Button
                                disabled={busy}
                                label="Back"
                                onClick={() => dispatch({type: 'BACK'})}
                                variant="outline"
                            />
                        )}

                        {state.step === 'activate' ? (
                            <Button disabled={busy} label="Activate" onClick={activate} />
                        ) : (
                            <Button
                                disabled={busy || !canProceed(state)}
                                label="Next"
                                onClick={() => dispatch({type: 'NEXT'})}
                            />
                        )}
                    </>
                )}
            </DialogFooter>
        </>
    );
};

interface ActivationWizardProps {
    onClose: () => void;
    template: AutomationWorkflowProjectWorkflowTemplate;
}

const ActivationWizard = ({onClose, template}: ActivationWizardProps) => {
    const {isError, isLoading, refetch, requiredComponents} = useRequiredComponents(template);

    return (
        <Dialog onOpenChange={(open) => !open && onClose()} open>
            <DialogContent className="sm:max-w-xl" showCloseButton>
                <DialogHeader>
                    <DialogTitle>{template.label}</DialogTitle>

                    <DialogDescription>Set this automation up and switch it on.</DialogDescription>
                </DialogHeader>

                {isLoading && (
                    <div className="flex justify-center py-8" data-testid="activation-wizard-loading">
                        <LoadingDots />
                    </div>
                )}

                {!isLoading && isError && (
                    <div className="flex flex-col gap-3" data-testid="activation-wizard-error">
                        <Alert variant="destructive">
                            <AlertDescription>{REQUIRED_COMPONENTS_ERROR_MESSAGE}</AlertDescription>
                        </Alert>

                        <div className="flex justify-end">
                            <Button label="Try again" onClick={() => refetch()} variant="outline" />
                        </div>
                    </div>
                )}

                {!isLoading && !isError && (
                    <ActivationWizardContent
                        onClose={onClose}
                        requiredComponents={requiredComponents}
                        template={template}
                    />
                )}
            </DialogContent>
        </Dialog>
    );
};

export default ActivationWizard;
