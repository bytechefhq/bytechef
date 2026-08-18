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
import ActivateStep from '@/ee/pages/embedded/automation-hub/wizard/ActivateStep';
import ConfigureStep from '@/ee/pages/embedded/automation-hub/wizard/ConfigureStep';
import ConnectAccountsStep from '@/ee/pages/embedded/automation-hub/wizard/ConnectAccountsStep';
import {ActivationStepType, canProceed} from '@/ee/pages/embedded/automation-hub/wizard/activationReducer';
import {useActivationFlow, useRequiredComponents} from '@/ee/pages/embedded/automation-hub/wizard/useActivationFlow';
import {
    AutomationWorkflowProjectKindEnum,
    AutomationWorkflowProjectWorkflowTemplate,
} from '@/ee/shared/middleware/embedded/public';
import {useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

const REQUIRED_COMPONENTS_ERROR_MESSAGE = 'This automation could not be set up. Please try again.';

const STEPS: {label: string; step: ActivationStepType}[] = [
    {label: 'Connect', step: 'connect'},
    {label: 'Configure', step: 'configure'},
    {label: 'Activate', step: 'activate'},
];

interface ActivationWizardContentProps {
    kind: AutomationWorkflowProjectKindEnum;
    onClose: () => void;
    requiredComponents: string[];
    template: AutomationWorkflowProjectWorkflowTemplate;
}

const ActivationWizardContent = ({kind, onClose, requiredComponents, template}: ActivationWizardContentProps) => {
    const {activate, busy, configure, dispatch, openInBuilder, retryWiring, state, wiringComplete, wiringFailed} =
        useActivationFlow(template, kind, requiredComponents);

    const steps = useMemo(
        () => (requiredComponents.length === 0 ? STEPS.filter(({step}) => step !== 'connect') : STEPS),
        [requiredComponents.length]
    );

    const currentStepIndex = useMemo(() => steps.findIndex(({step}) => step === state.step), [state.step, steps]);

    // A fresh missing-connection highlight supersedes any earlier failure message: the reducer
    // deliberately leaves `error` in place on MISSING_CONNECTION, so rendering both would put a
    // stale banner next to the very row the user is being asked to fix. The message is likewise
    // hidden while a retry is in flight.
    const errorShown = !!state.error && !state.highlightedComponent && !busy;

    const backShown = state.step === 'activate' || (state.step === 'configure' && requiredComponents.length > 0);

    return (
        <>
            <ol className="flex items-center gap-2 text-sm" data-testid="activation-wizard-stepper">
                {steps.map(({label, step}, index) => (
                    <li className="flex items-center gap-2" key={step}>
                        {index > 0 && <span className="text-muted-foreground">/</span>}

                        <span
                            className={twMerge(
                                'text-muted-foreground',
                                (state.step === 'done' || index <= currentStepIndex) && 'font-medium text-foreground'
                            )}
                        >
                            {label}
                        </span>
                    </li>
                ))}
            </ol>

            {errorShown && (
                <Alert variant="destructive">
                    <AlertDescription>{state.error}</AlertDescription>
                </Alert>
            )}

            {state.step === 'connect' && <ConnectAccountsStep dispatch={dispatch} state={state} template={template} />}

            {state.step === 'configure' && (
                <ConfigureStep
                    busy={busy}
                    onConfigure={configure}
                    onRetryWiring={retryWiring}
                    state={state}
                    template={template}
                    wiringComplete={wiringComplete}
                    wiringFailed={wiringFailed}
                />
            )}

            {(state.step === 'activate' || state.step === 'done') && <ActivateStep state={state} template={template} />}

            <DialogFooter>
                {state.step === 'done' ? (
                    <>
                        {kind === 'COPY' && (
                            <Button label="Open in builder" onClick={openInBuilder} variant="outline" />
                        )}

                        <Button label="Done" onClick={onClose} />
                    </>
                ) : (
                    <>
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
                                disabled={
                                    busy ||
                                    // Scoped to the configure step: this is the shared Next
                                    // button, and gating it on the connect step too would leave a
                                    // failed wiring stranding the user there with no banner (BACK
                                    // clears it) and no Try again (it lives in the configure step).
                                    (state.step === 'configure' && !wiringComplete) ||
                                    !canProceed(state)
                                }
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
    kind: AutomationWorkflowProjectKindEnum;
    onClose: () => void;
    template: AutomationWorkflowProjectWorkflowTemplate;
}

/**
 * The three-step activation wizard: connect accounts → configure → activate.
 *
 * The required-component lookup is resolved BEFORE the flow mounts, because the reducer's initial
 * step is derived from it exactly once: mounting the flow against a still-loading (and therefore
 * empty) list would skip the connect step for a template that needs it. A lookup that has NEVER
 * produced data is just as disqualifying and gets the same treatment for the same reason — an
 * empty list would skip the connect step, `buildWiringRequests` would return nothing, and Activate
 * would publish and enable a copy with no connections wired onto it.
 *
 * `isError` (see `useRequiredComponents`) is what gates this, not the query's raw `error`: a
 * background refetch failure on an already-open wizard (the query has a 5-minute `staleTime`, and
 * `HubConnectionDialog` mounts a second observer of the same key) must leave the open wizard
 * alone rather than unmount `ActivationWizardContent` mid-flight — that would drop the reducer
 * state and `copiedWorkflowUuidRef`, so a subsequent "Try again" would copy the template a second
 * time.
 *
 * Closing the dialog after step 2 leaves a disabled automation behind — honest state, which the
 * user can enable from its template card — so there is deliberately no cleanup on close.
 */
const ActivationWizard = ({kind, onClose, template}: ActivationWizardProps) => {
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
                        kind={kind}
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
