import {CheckIcon, Loader2Icon, XIcon} from 'lucide-react';
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

interface WorkflowTestNodeStatusProps {
    /** The corner badge gives way to another one sitting in the same corner (the node's issue badge). */
    hideStatusBadge?: boolean;
    testNodeState: WorkflowTestNodeStateI;
}

/**
 * How a node ran: a status badge in the box's top-right corner and, once it finished, its duration
 * beneath the box. Positioned absolutely, so the box it sits in must be `relative`.
 */
const WorkflowTestNodeStatus = ({hideStatusBadge, testNodeState}: WorkflowTestNodeStatusProps) => (
    <>
        {!hideStatusBadge && (
            <span
                className={twMerge(
                    'absolute -top-3 -right-3 z-10 flex size-6 items-center justify-center rounded-full border-2 bg-surface-neutral-primary [&_svg.lucide]:size-3.5',
                    testNodeState.status === 'RUNNING' && 'border-blue-500 text-blue-500',
                    testNodeState.status === 'COMPLETED' && 'border-green-500 text-green-500',
                    testNodeState.status === 'FAILED' && 'border-red-500 text-red-500'
                )}
                title={testNodeState.error}
            >
                {testNodeState.status === 'RUNNING' && <Loader2Icon className="size-3.5 animate-spin" />}

                {testNodeState.status === 'COMPLETED' && <CheckIcon className="size-3.5" />}

                {testNodeState.status === 'FAILED' && <XIcon className="size-3.5" />}
            </span>
        )}

        {testNodeState.durationMillis != null && testNodeState.status !== 'RUNNING' && (
            <span className="absolute -bottom-2.5 left-1/2 z-10 -translate-x-1/2 rounded-full border border-stroke-neutral-tertiary bg-surface-neutral-primary px-1.5 text-xs leading-4 text-content-neutral-secondary">
                {formatTestNodeDuration(testNodeState.durationMillis)}
            </span>
        )}
    </>
);

export default WorkflowTestNodeStatus;
