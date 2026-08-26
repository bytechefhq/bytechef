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
    /** The `to` is an expression resolved at run time, so it names no member and cannot be missing. */
    dynamic: boolean;
    /** Position in `parameters.transitions`, which is the transition's identity everywhere. */
    index: number;
    /** True when `to` names a node the graph does not declare. */
    missingTarget: boolean;
    to: string;
    /** No condition, so the runtime takes this transition only when no conditional one matched. */
    unconditional: boolean;
}

export interface GraphTransitionGroupI {
    /**
     * True when the group's own node name is not declared by the graph — the transitions in it
     * leave a node that no longer exists.
     */
    danglingSource: boolean;
    duplicateDefault: boolean;
    nodeName: string;
    rows: Array<GraphTransitionRowI>;
}

/**
 * Groups a graph's transitions by the node they leave, in declaration order — which is the order
 * the runtime resolves them by, so the list reads as the routing table it is. Resolution is not a
 * plain top-to-bottom scan: on a node's completion the CONDITIONAL transitions are evaluated in
 * declared order and the first truthy one wins; only if none matched is the first UNCONDITIONAL one
 * taken. So declared order is priority among the conditionals, and an unconditional transition is
 * the fallback wherever it sits in the list — declaring it first does not make it fire first.
 *
 * Every declared node gets a group even with no outgoing transitions (that is what makes it
 * terminal, and the panel says so). After them come groups for `from` names the graph does not
 * declare: those transitions exist in the definition but the canvas cannot draw them at all,
 * because their edge would reference a node id React Flow does not have. This list is the only
 * surface on which they appear.
 */
export function buildGraphTransitionGroups(
    memberNames: Array<string>,
    transitions: Array<GraphTransitionType>
): Array<GraphTransitionGroupI> {
    const declaredMemberNames = new Set(memberNames);
    const duplicateDefaultNodeNames = new Set(findNodesWithDuplicateDefault(transitions));

    const undeclaredSourceNames = transitions.reduce<Array<string>>((names, transition) => {
        if (!declaredMemberNames.has(transition.from) && !names.includes(transition.from)) {
            names.push(transition.from);
        }

        return names;
    }, []);

    return [...memberNames, ...undeclaredSourceNames].map((nodeName) => ({
        danglingSource: !declaredMemberNames.has(nodeName),
        duplicateDefault: duplicateDefaultNodeNames.has(nodeName),
        nodeName,
        rows: transitionsFrom(transitions, nodeName).map(({index, transition}) => {
            const dynamic = isDynamicTransitionTarget(transition.to);

            return {
                condition: transition.condition,
                dynamic,
                index,
                missingTarget: !dynamic && !declaredMemberNames.has(transition.to),
                to: transition.to,
                unconditional: isUnconditional(transition),
            };
        }),
    }));
}

interface GraphTransitionsPanelPropsI {
    graphId: string;
}

/**
 * The properties-panel counterpart to the transition edges on the canvas: when the graph CONTAINER
 * node is selected, lists every declared transition grouped under the node it leaves, and lets that
 * routing table be reordered and pruned.
 *
 * It deliberately does NOT edit conditions — the edge popover owns that, addressing the same
 * `transitions[i].condition` sub-property. What the panel adds over the canvas is everything the
 * canvas cannot express: declaration order within a group (which IS conditional priority), the
 * duplicate-unconditional warning, terminal nodes, and transitions naming a node the graph no longer
 * declares, which React Flow drops before rendering and which would otherwise be invisible in the
 * whole editor.
 */
export default function GraphTransitionsPanel({graphId}: GraphTransitionsPanelPropsI) {
    const workflowTasks = useWorkflowDataStore((state) => state.workflow.tasks);

    const {updateWorkflowMutation} = useWorkflowEditor();

    // Read off the LIVE task rather than the panel store's snapshot of the selected node: the store
    // holds the node as it was when the panel opened, so connecting or deleting a transition on the
    // canvas would leave this list showing the routing table from before the edit. Flattened first,
    // because a graph nested inside another dispatcher is not a top-level task.
    const graphTask: WorkflowTask | undefined = useMemo(
        () => flattenDefinitionTasks(workflowTasks ?? []).find((task) => task.name === graphId),
        [graphId, workflowTasks]
    );

    const memberNames = useMemo(
        () => ((graphTask?.parameters?.nodes as Array<WorkflowTask> | undefined) ?? []).map((node) => node.name),
        [graphTask]
    );

    const transitions = useMemo(
        () => (graphTask?.parameters?.transitions as Array<GraphTransitionType> | undefined) ?? [],
        [graphTask]
    );

    const groups = useMemo(() => buildGraphTransitionGroups(memberNames, transitions), [memberNames, transitions]);

    const saveTransitions = (
        mutate: (currentTransitions: Array<GraphTransitionType>) => Array<GraphTransitionType>
    ) => {
        if (!updateWorkflowMutation) {
            return;
        }

        saveGraphTransitions(graphId, mutate, updateWorkflowMutation);
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
            {groups.map((group) => (
                <fieldset
                    aria-label={`${group.nodeName} transitions`}
                    className="space-y-2 rounded-md border border-stroke-neutral-secondary p-3"
                    key={group.nodeName}
                >
                    <div className="flex items-center justify-between gap-2">
                        <span className="truncate text-sm font-medium">{group.nodeName}</span>

                        {!group.rows.length && <Badge label="terminal" styleType="secondary-outline" />}
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

                            <span>More than one unconditional transition — the first declared is taken.</span>
                        </p>
                    )}

                    {group.rows.length ? (
                        <ul className="space-y-2">
                            {group.rows.map((row, rowIndex) => (
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
                                        {/* Order among ONE node's outgoing transitions is its
                                            conditional priority — the first whose condition holds
                                            is the one taken — so these reorder that priority. A
                                            node with a single transition has no priority to decide
                                            and both buttons would be permanently disabled, so they
                                            are left out rather than shown dead. */}

                                        {group.rows.length > 1 && (
                                            <>
                                                <Button
                                                    aria-label={`Move transition ${group.nodeName} to ${row.to} up`}
                                                    disabled={rowIndex === 0}
                                                    icon={<ArrowUpIcon />}
                                                    onClick={() =>
                                                        saveTransitions((currentTransitions) =>
                                                            moveTransition(currentTransitions, row.index, -1)
                                                        )
                                                    }
                                                    size="iconXs"
                                                    variant="ghost"
                                                />

                                                <Button
                                                    aria-label={`Move transition ${group.nodeName} to ${row.to} down`}
                                                    disabled={rowIndex === group.rows.length - 1}
                                                    icon={<ArrowDownIcon />}
                                                    onClick={() =>
                                                        saveTransitions((currentTransitions) =>
                                                            moveTransition(currentTransitions, row.index, 1)
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
                                                    removeTransition(currentTransitions, row.index)
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
                            No outgoing transitions — the graph run ends here.
                        </p>
                    )}
                </fieldset>
            ))}
        </div>
    );
}
