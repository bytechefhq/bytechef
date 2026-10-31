import {BadgeStyleType} from '@/components/Badge/Badge';
import {VersionWorkflowChangeSummary, WorkflowChangeType} from '@/shared/middleware/graphql';

const BADGE_STYLE_TYPES: Record<WorkflowChangeType, BadgeStyleType> = {
    [WorkflowChangeType.Added]: 'success-outline',
    [WorkflowChangeType.Modified]: 'warning-outline',
    [WorkflowChangeType.Removed]: 'destructive-outline',
    [WorkflowChangeType.Unchanged]: 'secondary-outline',
};

const CHANGE_TYPE_LABELS: Record<WorkflowChangeType, string> = {
    [WorkflowChangeType.Added]: 'Added',
    [WorkflowChangeType.Modified]: 'Modified',
    [WorkflowChangeType.Removed]: 'Removed',
    [WorkflowChangeType.Unchanged]: 'Unchanged',
};

export function getWorkflowChangeTypeBadgeStyleType(changeType: WorkflowChangeType): BadgeStyleType {
    return BADGE_STYLE_TYPES[changeType];
}

export function getWorkflowChangeTypeLabel(changeType: WorkflowChangeType): string {
    return CHANGE_TYPE_LABELS[changeType];
}

export function formatVersionWorkflowChangesSummary(workflows: VersionWorkflowChangeSummary[]): string {
    if (workflows.length === 0) {
        return 'No workflow changes.';
    }

    const parts: string[] = [];

    for (const changeType of [WorkflowChangeType.Added, WorkflowChangeType.Modified, WorkflowChangeType.Removed]) {
        const count = workflows.filter((workflow) => workflow.changeType === changeType).length;

        if (count > 0) {
            parts.push(`${count} ${CHANGE_TYPE_LABELS[changeType].toLowerCase()}`);
        }
    }

    const workflowLabel = workflows.length === 1 ? 'workflow' : 'workflows';

    return `${workflows.length} ${workflowLabel} changed (${parts.join(', ')})`;
}

export function formatVersionLabel(version: number | null | undefined, draft: boolean): string {
    return draft ? 'Draft' : `V${version}`;
}

export function formatVersionRange(
    baseVersion: number | null | undefined,
    version: number | null | undefined,
    draft: boolean
): string {
    const versionLabel = formatVersionLabel(version, draft);

    if (baseVersion == null) {
        return versionLabel;
    }

    return `${formatVersionLabel(baseVersion, false)} → ${versionLabel}`;
}
