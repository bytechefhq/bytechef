import {useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

import useWorkflowTestNodeStates from '../hooks/useWorkflowTestNodeStates';
import {type WorkflowTestNodeStateI} from '../stores/useWorkflowEditorStore';

type StatusType = WorkflowTestNodeStateI['status'];

const LEGEND_ITEMS: {className: string; label: string; status: StatusType}[] = [
    {className: 'bg-blue-500', label: 'Running', status: 'RUNNING'},
    {className: 'bg-green-500', label: 'Completed', status: 'COMPLETED'},
    {className: 'bg-red-500', label: 'Failed', status: 'FAILED'},
    {className: 'bg-amber-500', label: 'Blocked', status: 'BLOCKED'},
    {className: 'bg-amber-500', label: 'Withheld', status: 'WITHHELD'},
    {className: 'bg-sky-500', label: 'Redacted', status: 'REDACTED'},
];

const WorkflowTestStatusLegend = () => {
    const workflowTestNodeStates = useWorkflowTestNodeStates();

    const legendItems = useMemo(() => {
        const statuses = new Set(Object.values(workflowTestNodeStates).map((nodeState) => nodeState.status));

        return LEGEND_ITEMS.filter((legendItem) => statuses.has(legendItem.status));
    }, [workflowTestNodeStates]);

    if (legendItems.length === 0) {
        return null;
    }

    return (
        <ul
            aria-label="Run status legend"
            className="flex items-center gap-3 rounded-md bg-surface-neutral-secondary px-3 py-[10px]"
        >
            {legendItems.map((legendItem) => (
                <li
                    className="flex items-center gap-1.5 text-xs font-medium whitespace-nowrap text-content-neutral-primary"
                    data-testid="workflow-test-status-legend-item"
                    key={legendItem.status}
                >
                    <span className={twMerge('size-2 rounded-full', legendItem.className)} />

                    <span>{legendItem.label}</span>
                </li>
            ))}
        </ul>
    );
};

export default WorkflowTestStatusLegend;
