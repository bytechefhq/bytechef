import GettingStartedStatusBox from '@/components/GettingStarted/GettingStartedStatusBox';
import {Link} from 'react-router-dom';

import {AutomationGettingStartedStatusType} from '../automationGettingStartedStatus';
import {FirstRunStateType} from '../getAutomationGettingStartedSteps';

interface FirstRunStepContentProps {
    executionsLinkVisible?: boolean;
    firstRun: AutomationGettingStartedStatusType['firstRun'];
    firstRunState: FirstRunStateType;
    lastFailedRun: AutomationGettingStartedStatusType['lastFailedRun'];
    waitingHint?: string;
}

const EXECUTIONS_LINK_CLASS_NAME = 'text-xs font-medium text-content-brand-primary hover:underline';

const DEFAULT_WAITING_HINT = "Click Run now in the editor's Getting started card.";

const getRunReceivedText = (firstRun: AutomationGettingStartedStatusType['firstRun']) => {
    if (!firstRun) {
        return 'Setup is complete. See Workflow Executions for runs.';
    }

    return 'Your workflow ran.';
};

const FirstRunStepContent = ({
    executionsLinkVisible = true,
    firstRun,
    firstRunState,
    lastFailedRun,
    waitingHint = DEFAULT_WAITING_HINT,
}: FirstRunStepContentProps) => (
    <div className="mt-2 space-y-3">
        {firstRunState === 'done' && (
            <GettingStartedStatusBox
                data-testid="run-received"
                title={firstRun ? 'Run received' : "You're set up"}
                tone="success"
            >
                <p className="text-xs text-content-neutral-secondary">{getRunReceivedText(firstRun)}</p>

                {executionsLinkVisible && (
                    <div className="flex items-center gap-3 pt-1">
                        <Link className={EXECUTIONS_LINK_CLASS_NAME} to="/automation/workflow-executions">
                            View workflow executions
                        </Link>
                    </div>
                )}
            </GettingStartedStatusBox>
        )}

        {firstRunState === 'waiting' && (
            <GettingStartedStatusBox title="Waiting for the first run" tone="waiting">
                {waitingHint && <p className="text-xs text-content-neutral-secondary">{waitingHint}</p>}
            </GettingStartedStatusBox>
        )}

        {firstRunState === 'failed' && (
            <GettingStartedStatusBox data-testid="last-failed-run" title="Run failed" tone="error">
                <p className="text-xs text-content-destructive-primary">
                    {`Runs are arriving, but none has succeeded yet. Latest: ${[lastFailedRun?.status, lastFailedRun?.errorMessage].filter(Boolean).join(' - ')}`}
                </p>

                <Link className={EXECUTIONS_LINK_CLASS_NAME} to="/automation/workflow-executions">
                    View workflow executions
                </Link>
            </GettingStartedStatusBox>
        )}
    </div>
);

export default FirstRunStepContent;
