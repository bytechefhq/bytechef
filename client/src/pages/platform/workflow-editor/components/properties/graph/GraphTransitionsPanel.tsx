import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {flattenDefinitionTasks} from '@/pages/platform/workflow-editor/utils/flattenDefinitionTasks';
import {
    findNodesWithDuplicateDefault,
    isDynamicTransitionTarget,
    isUnconditional,
    moveTransition,
    removeTransition,
    transitionsFrom,
} from '@/pages/platform/workflow-editor/utils/graph/graphTransitionMutations';
import {saveGraphTransitions} from '@/pages/platform/workflow-editor/utils/graph/saveGraphParameters';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {GraphTransitionType} from '@/shared/types';
import {ArrowDownIcon, ArrowUpIcon, Trash2Icon, TriangleAlertIcon} from 'lucide-react';
import {useMemo} from 'react';

export interface GraphTransitionRowI {
    condition?: string;
    dynamic: boolean;
    index: number;
    missingTarget: boolean;
    movableDown: boolean;
    movableUp: boolean;
    to: string;
    transition: GraphTransitionType;
    unconditional: boolean;
}

export interface GraphTransitionGroupI {
    danglingSource: boolean;
    duplicateDefault: boolean;
    nodeName: string;
    rows: Array<GraphTransitionRowI>;
    terminal: boolean;
}

export function buildGraphTransitionGroups(
    memberNames: Array<string>,
    transitions: Array<GraphTransitionType>
): Array<GraphTransitionGroupI> {
    const declaredMemberNames = new Set(memberNames);
    const nodesWithDuplicateDefault = findNodesWithDuplicateDefault(transitions);
    const duplicateDefaultNodeNames = new Set(nodesWithDuplicateDefault);

    const undeclaredSourceNames = transitions.reduce<Array<string>>((names, transition) => {
        if (!declaredMemberNames.has(transition.from) && !names.includes(transition.from)) {
            names.push(transition.from);
        }

        return names;
    }, []);

    return [...memberNames, ...undeclaredSourceNames].map((nodeName) => {
        const declaredTransitions = transitionsFrom(transitions, nodeName);

        const conditionalTransitions = declaredTransitions.filter(({transition}) => !isUnconditional(transition));
        const unconditionalTransitions = declaredTransitions.filter(({transition}) => isUnconditional(transition));

        const rows = [conditionalTransitions, unconditionalTransitions].flatMap((sameKindTransitions) =>
            sameKindTransitions.map(({index, transition}, positionInKind) => {
                const dynamic = isDynamicTransitionTarget(transition.to);

                return {
                    condition: transition.condition,
                    dynamic,
                    index,
                    missingTarget: !dynamic && !declaredMemberNames.has(transition.to),
                    movableDown: positionInKind < sameKindTransitions.length - 1,
                    movableUp: positionInKind > 0,
                    to: transition.to,
                    transition,
                    unconditional: isUnconditional(transition),
                };
            })
        );

        return {
            danglingSource: !declaredMemberNames.has(nodeName),
            duplicateDefault: duplicateDefaultNodeNames.has(nodeName),
            nodeName,
            rows,
            terminal: rows.length === 0,
        };
    });
}

interface GraphTransitionsPanelProps {
    graphId: string;
}

export default function GraphTransitionsPanel({graphId}: GraphTransitionsPanelProps) {
    const workflowTasks = useWorkflowDataStore((state) => state.workflow.tasks);

    const {updateWorkflowMutation} = useWorkflowEditor();

    const graphTask: WorkflowTask | undefined = useMemo(() => {
        const definitionTasks = workflowTasks ?? [];

        return flattenDefinitionTasks(definitionTasks).find((task) => task.name === graphId);
    }, [graphId, workflowTasks]);

    const memberNames = useMemo(
        () => ((graphTask?.parameters?.nodes as Array<WorkflowTask> | undefined) ?? []).map((node) => node.name),
        [graphTask]
    );

    const transitions = useMemo(
        () => (graphTask?.parameters?.transitions as Array<GraphTransitionType> | undefined) ?? [],
        [graphTask]
    );

    const groups = useMemo(() => buildGraphTransitionGroups(memberNames, transitions), [memberNames, transitions]);

    const undeclaredStartNodeName = useMemo(() => {
        const declaredStartNodeName = (graphTask?.parameters?.startNode as string | undefined)?.trim();

        return declaredStartNodeName && !memberNames.includes(declaredStartNodeName)
            ? declaredStartNodeName
            : undefined;
    }, [graphTask, memberNames]);

    const saveTransitions = (
        mutate: (currentTransitions: Array<GraphTransitionType>) => Array<GraphTransitionType>
    ) => {
        if (!updateWorkflowMutation) {
            return;
        }

        saveGraphTransitions({graphId, mutate, updateWorkflowMutation});
    };

    if (!groups.length) {
        return (
            <p className="p-4 text-sm text-muted-foreground">
                This graph has no nodes yet. Add a node from the canvas to route between them here.
            </p>
        );
    }

    return (
        <div className="space-y-4 p-4">
            {undeclaredStartNodeName && (
                <p className="flex items-start gap-1 text-xs text-content-destructive">
                    <TriangleAlertIcon className="mt-px size-3 shrink-0" />

                    <span>
                        The graph declares no node named &quot;{undeclaredStartNodeName}&quot;, so a run cannot start
                        there. Pick a start node on the canvas.
                    </span>
                </p>
            )}

            {groups.map((group) => (
                <fieldset
                    aria-label={`${group.nodeName} transitions`}
                    className="min-w-0 space-y-2 rounded-md border border-stroke-neutral-secondary p-3"
                    key={group.nodeName}
                >
                    <div className="flex items-center justify-between gap-2">
                        <span className="truncate text-sm font-medium">{group.nodeName}</span>

                        {group.terminal && <Badge label="terminal" styleType="secondary-outline" />}
                    </div>

                    {group.danglingSource && (
                        <p className="flex items-start gap-1 text-xs text-content-destructive">
                            <TriangleAlertIcon className="mt-px size-3 shrink-0" />

                            <span>
                                The graph declares no node named &quot;{group.nodeName}&quot;, so the transitions below
                                cannot be drawn on the canvas.
                            </span>
                        </p>
                    )}

                    {group.duplicateDefault && (
                        <p className="flex items-start gap-1 text-xs text-content-destructive">
                            <TriangleAlertIcon className="mt-px size-3 shrink-0" />

                            <span>More than one unconditional transition - the first declared is taken.</span>
                        </p>
                    )}

                    {!group.terminal ? (
                        <ul className="space-y-2">
                            {group.rows.map((row) => (
                                <li
                                    aria-label={`Transition ${group.nodeName} to ${row.to}`}
                                    className="flex items-start justify-between gap-2 rounded-md bg-surface-neutral-secondary p-2"
                                    key={row.index}
                                >
                                    <div className="min-w-0 flex-1 space-y-1">
                                        <div className="flex items-center gap-1">
                                            <span className="truncate text-sm">&rarr; {row.to}</span>

                                            {row.dynamic && <Badge label="dynamic" styleType="secondary-outline" />}
                                        </div>

                                        <p className="truncate text-xs text-muted-foreground">
                                            {row.unconditional ? 'Unconditional' : row.condition}
                                        </p>

                                        {row.missingTarget && (
                                            <p className="flex items-start gap-1 text-xs text-content-destructive">
                                                <TriangleAlertIcon className="mt-px size-3 shrink-0" />

                                                <span>
                                                    The graph declares no node named &quot;{row.to}&quot;, so this
                                                    transition cannot be drawn.
                                                </span>
                                            </p>
                                        )}
                                    </div>

                                    <div className="flex shrink-0 items-center gap-1">
                                        {(row.movableUp || row.movableDown) && (
                                            <>
                                                <Button
                                                    aria-label={`Move transition ${group.nodeName} to ${row.to} up`}
                                                    disabled={!row.movableUp}
                                                    icon={<ArrowUpIcon />}
                                                    onClick={() =>
                                                        saveTransitions((currentTransitions) =>
                                                            moveTransition({
                                                                direction: -1,
                                                                expectedTransition: row.transition,
                                                                index: row.index,
                                                                transitions: currentTransitions,
                                                            })
                                                        )
                                                    }
                                                    size="iconXs"
                                                    variant="ghost"
                                                />

                                                <Button
                                                    aria-label={`Move transition ${group.nodeName} to ${row.to} down`}
                                                    disabled={!row.movableDown}
                                                    icon={<ArrowDownIcon />}
                                                    onClick={() =>
                                                        saveTransitions((currentTransitions) =>
                                                            moveTransition({
                                                                direction: 1,
                                                                expectedTransition: row.transition,
                                                                index: row.index,
                                                                transitions: currentTransitions,
                                                            })
                                                        )
                                                    }
                                                    size="iconXs"
                                                    variant="ghost"
                                                />
                                            </>
                                        )}

                                        <Button
                                            aria-label={`Delete transition ${group.nodeName} to ${row.to}`}
                                            icon={<Trash2Icon />}
                                            onClick={() =>
                                                saveTransitions((currentTransitions) =>
                                                    removeTransition({
                                                        expectedTransition: row.transition,
                                                        index: row.index,
                                                        transitions: currentTransitions,
                                                    })
                                                )
                                            }
                                            size="iconXs"
                                            variant="destructiveGhost"
                                        />
                                    </div>
                                </li>
                            ))}
                        </ul>
                    ) : (
                        <p className="text-xs text-muted-foreground">
                            No outgoing transitions - the graph run ends here.
                        </p>
                    )}
                </fieldset>
            ))}
        </div>
    );
}
