import {ToggleGroup, ToggleGroupItem} from '@/components/ui/toggle-group';
import {
    VersionWorkflowChange,
    WorkflowChangeType,
    WorkflowNodeChangeKind,
    WorkflowNodeChangeTarget,
} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useGetTaskDispatcherDefinitionsQuery} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';
import {useMemo} from 'react';

import WorkflowChangeCanvas from './WorkflowChangeCanvas';
import {collectComponentNames, parseWorkflowDefinition} from './parseWorkflowDefinition';

export type SideType = 'new' | 'old';

const LEGEND: {className: string; label: string}[] = [
    {className: 'bg-green-500', label: 'Added'},
    {className: 'bg-amber-500', label: 'Modified'},
    {className: 'bg-blue-500', label: 'Moved'},
    {className: 'bg-red-500', label: 'Removed'},
];

const SIDE_KINDS: Record<SideType, WorkflowNodeChangeKind[]> = {
    new: [WorkflowNodeChangeKind.Added, WorkflowNodeChangeKind.Modified, WorkflowNodeChangeKind.Moved],
    old: [WorkflowNodeChangeKind.Removed, WorkflowNodeChangeKind.Modified, WorkflowNodeChangeKind.Moved],
};

function getRootTaskName(parentPath: string | null | undefined): string | undefined {
    return parentPath ? parentPath.split(/[./]/)[0] : undefined;
}

function deriveHighlights(change: VersionWorkflowChange, side: SideType): Record<string, WorkflowNodeChangeKind> {
    const kinds = SIDE_KINDS[side];
    const highlights: Record<string, WorkflowNodeChangeKind> = {};

    const applicableChanges = change.changes.filter((workflowChange) => kinds.includes(workflowChange.kind));

    applicableChanges.forEach((workflowChange) => {
        if (
            workflowChange.name &&
            (workflowChange.target === WorkflowNodeChangeTarget.Task ||
                workflowChange.target === WorkflowNodeChangeTarget.Trigger)
        ) {
            highlights[workflowChange.name] = workflowChange.kind;
        }
    });

    applicableChanges.forEach((workflowChange) => {
        const rootTaskName = getRootTaskName(workflowChange.parentPath);

        if (
            workflowChange.target === WorkflowNodeChangeTarget.ClusterElement &&
            rootTaskName &&
            !highlights[rootTaskName]
        ) {
            highlights[rootTaskName] = WorkflowNodeChangeKind.Modified;
        }
    });

    return highlights;
}

export function resolveCanvasSide(change: VersionWorkflowChange, selectedSide: SideType): SideType {
    if (selectedSide === 'new' && change.newDefinition) {
        return 'new';
    }

    return change.oldDefinition ? 'old' : 'new';
}

interface WorkflowChangeCanvasToolbarProps {
    baseVersionLabel?: string;
    change: VersionWorkflowChange;
    onSelectedSideChange: (side: SideType) => void;
    selectedSide: SideType;
    versionLabel: string;
}

export const WorkflowChangeCanvasToolbar = ({
    baseVersionLabel,
    change,
    onSelectedSideChange,
    selectedSide,
    versionLabel,
}: WorkflowChangeCanvasToolbarProps) => (
    <div className="flex items-center gap-4">
        {change.oldDefinition && change.newDefinition && (
            <ToggleGroup
                aria-label="Canvas version"
                onValueChange={(value) => value && onSelectedSideChange(value as SideType)}
                size="sm"
                type="single"
                value={resolveCanvasSide(change, selectedSide)}
                variant="outline"
            >
                <ToggleGroupItem className="px-3" value="old">{`${baseVersionLabel} (before)`}</ToggleGroupItem>

                <ToggleGroupItem className="px-3" value="new">{`${versionLabel} (after)`}</ToggleGroupItem>
            </ToggleGroup>
        )}

        {change.changeType === WorkflowChangeType.Modified && (
            <ul className="flex items-center gap-3 text-xs text-content-neutral-secondary">
                {LEGEND.map((legendItem) => (
                    <li className="flex items-center gap-1" key={legendItem.label}>
                        <span className={`size-2.5 rounded-sm ${legendItem.className}`} />

                        {legendItem.label}
                    </li>
                ))}
            </ul>
        )}
    </div>
);

interface WorkflowChangeCanvasTabProps {
    change: VersionWorkflowChange;
    selectedSide: SideType;
}

const WorkflowChangeCanvasTab = ({change, selectedSide}: WorkflowChangeCanvasTabProps) => {
    const side = resolveCanvasSide(change, selectedSide);

    const definition = side === 'new' ? change.newDefinition : change.oldDefinition;

    const highlights = useMemo(() => deriveHighlights(change, side), [change, side]);
    const workflow = useMemo(() => {
        if (!definition) {
            return undefined;
        }

        try {
            return parseWorkflowDefinition(definition);
        } catch {
            return undefined;
        }
    }, [definition]);
    const componentNames = useMemo(() => (workflow ? collectComponentNames(workflow) : []), [workflow]);

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery(
        {actionDefinitions: true, clusterElementDefinitions: true, include: componentNames, triggerDefinitions: true},
        componentNames.length > 0
    );
    const {data: taskDispatcherDefinitions} = useGetTaskDispatcherDefinitionsQuery();

    return (
        <div className="h-[70vh]">
            {workflow && (
                <WorkflowChangeCanvas
                    componentDefinitions={componentDefinitions ?? []}
                    highlights={highlights}
                    key={side}
                    taskDispatcherDefinitions={taskDispatcherDefinitions ?? []}
                    workflow={workflow}
                />
            )}
        </div>
    );
};

export default WorkflowChangeCanvasTab;
