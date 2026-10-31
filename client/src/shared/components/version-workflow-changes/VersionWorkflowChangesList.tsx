import Badge from '@/components/Badge/Badge';
import LoadingIcon from '@/components/LoadingIcon';
import {VersionWorkflowChanges, WorkflowChangeType} from '@/shared/middleware/graphql';

import {
    formatVersionWorkflowChangesSummary,
    getWorkflowChangeTypeBadgeStyleType,
    getWorkflowChangeTypeLabel,
} from './versionWorkflowChangesUtils';

interface VersionWorkflowChangesListProps {
    changes?: VersionWorkflowChanges;
    isError?: boolean;
    isLoading: boolean;
    onWorkflowClick: (workflowUuid: string) => void;
}

const VersionWorkflowChangesList = ({
    changes,
    isError,
    isLoading,
    onWorkflowClick,
}: VersionWorkflowChangesListProps) => {
    if (isError) {
        return <p className="text-sm text-content-destructive">Couldn&apos;t load workflow changes.</p>;
    }

    if (isLoading) {
        return (
            <div className="flex justify-center py-2">
                <LoadingIcon />
            </div>
        );
    }

    if (!changes) {
        return null;
    }

    return (
        <div className="flex flex-col gap-2">
            <p className="text-sm text-content-neutral-secondary">
                {formatVersionWorkflowChangesSummary(changes.workflows)}
            </p>

            {changes.workflows.map((workflow) => {
                const workflowName = workflow.label || workflow.workflowUuid;

                return (
                    <button
                        aria-label={`Open changes of ${workflowName}`}
                        className="flex items-center justify-between gap-2 rounded-md border border-stroke-neutral-secondary px-3 py-2 text-left text-sm hover:bg-surface-neutral-primary-hover"
                        key={workflow.workflowUuid}
                        onClick={() => onWorkflowClick(workflow.workflowUuid)}
                        type="button"
                    >
                        <span className="truncate">{workflowName}</span>

                        <span className="flex shrink-0 items-center gap-2">
                            {workflow.changeType === WorkflowChangeType.Modified && (
                                <span className="text-xs text-content-neutral-secondary">
                                    {workflow.changeCount === 1 ? '1 change' : `${workflow.changeCount} changes`}
                                </span>
                            )}

                            <Badge
                                label={getWorkflowChangeTypeLabel(workflow.changeType)}
                                styleType={getWorkflowChangeTypeBadgeStyleType(workflow.changeType)}
                            />
                        </span>
                    </button>
                );
            })}
        </div>
    );
};

export default VersionWorkflowChangesList;
