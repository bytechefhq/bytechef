import {CheckIcon, EraserIcon, Loader2Icon, ShieldAlertIcon, ShieldBanIcon, UserXIcon, XIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

import {type WorkflowTestNodeStateI} from '../stores/useWorkflowEditorStore';

function formatTestNodeDuration(durationMillis: number): string {
    if (durationMillis < 1000) {
        return `${durationMillis}ms`;
    }

    if (durationMillis < 60000) {
        return `${(durationMillis / 1000).toFixed(1)}s`;
    }

    const minutes = Math.floor(durationMillis / 60000);
    const seconds = Math.round((durationMillis % 60000) / 1000);

    return `${minutes}m ${seconds}s`;
}

const OUTCOME_TITLES: Partial<Record<WorkflowTestNodeStateI['status'], string>> = {
    BLOCKED: 'Blocked',
    DENIED: 'Denied',
    REDACTED: 'Redacted',
    WITHHELD: 'Withheld',
};

interface WorkflowTestNodeStatusProps {
    durationPlacement?: 'bottom' | 'top';
    hideStatusBadge?: boolean;
    testNodeState: WorkflowTestNodeStateI;
}

const WorkflowTestNodeStatus = ({
    durationPlacement = 'bottom',
    hideStatusBadge,
    testNodeState,
}: WorkflowTestNodeStatusProps) => (
    <>
        {!hideStatusBadge && (
            <span
                className={twMerge(
                    'absolute -top-3 -right-3 z-10 flex size-6 items-center justify-center rounded-full border-2 bg-surface-neutral-primary [&_svg.lucide]:size-3.5',
                    testNodeState.status === 'RUNNING' && 'border-blue-500 text-blue-500',
                    testNodeState.status === 'COMPLETED' && 'border-green-500 text-green-500',
                    testNodeState.status === 'FAILED' && 'border-red-500 text-red-500',
                    (testNodeState.status === 'BLOCKED' || testNodeState.status === 'WITHHELD') &&
                        'border-amber-500 text-amber-500',
                    testNodeState.status === 'DENIED' && 'border-rose-500 text-rose-500',
                    testNodeState.status === 'REDACTED' && 'border-sky-500 text-sky-500'
                )}
                title={testNodeState.status === 'FAILED' ? testNodeState.error : OUTCOME_TITLES[testNodeState.status]}
            >
                {testNodeState.status === 'RUNNING' && <Loader2Icon className="size-3.5 animate-spin" />}

                {testNodeState.status === 'COMPLETED' && <CheckIcon className="size-3.5" />}

                {testNodeState.status === 'FAILED' && <XIcon className="size-3.5" />}

                {testNodeState.status === 'BLOCKED' && <ShieldBanIcon className="size-3.5" />}

                {testNodeState.status === 'WITHHELD' && <ShieldAlertIcon className="size-3.5" />}

                {testNodeState.status === 'DENIED' && <UserXIcon className="size-3.5" />}

                {testNodeState.status === 'REDACTED' && <EraserIcon className="size-3.5" />}
            </span>
        )}

        {testNodeState.durationMillis != null &&
            testNodeState.status !== 'RUNNING' &&
            !OUTCOME_TITLES[testNodeState.status] && (
                <span
                    className={twMerge(
                        'absolute z-10 rounded-full border border-stroke-neutral-tertiary bg-surface-neutral-primary px-1.5 text-xs leading-4 text-content-neutral-secondary',
                        durationPlacement === 'bottom' ? '-bottom-2.5 left-1/2 -translate-x-1/2' : '-top-2.5 right-5'
                    )}
                >
                    {formatTestNodeDuration(testNodeState.durationMillis)}
                </span>
            )}
    </>
);

export default WorkflowTestNodeStatus;
