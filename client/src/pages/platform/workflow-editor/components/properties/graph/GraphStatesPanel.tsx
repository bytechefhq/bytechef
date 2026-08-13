import Property from '@/pages/platform/workflow-editor/components/properties/Property';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import extractNextTargets from '@/pages/platform/workflow-editor/utils/extractNextTargets';
import {flattenDefinitionTasks} from '@/pages/platform/workflow-editor/utils/flattenDefinitionTasks';
import {TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';
import {GraphNodeType, PropertyAllType} from '@/shared/types';
import {useMemo} from 'react';

import GraphNextNodeSuggestions from './GraphNextNodeSuggestions';
import GraphTransitionBadges from './GraphTransitionBadges';

interface GraphStatesPanelPropsI {
    taskDispatcherDefinition?: TaskDispatcherDefinition;
}

/**
 * The properties-panel counterpart to the graph node name chips (see `GraphNodeLabel`): when
 * the graph CONTAINER node is selected, lists every declared node with its (read-only here —
 * the chips own renames) name and a `next`-expression editor.
 *
 * `nodes` is a `taskProperties` entry (`ARRAY_BUILDER` of `OBJECT_BUILDER`, its `tasks` field
 * typed `TASK`), not a `properties` entry — the generic `Properties`/`ArrayProperty` renderer
 * that `WorkflowNodeDetailsPanel` otherwise feeds `currentOperationProperties` from
 * `taskDispatcherDefinition.properties` would try to render every task inside every node as a
 * generic object field, which is unusable. This panel is a deliberate replacement for that
 * dead end: `nodes` is never handed to the generic renderer, and only the single `next`
 * sub-property is reused (via a bare `<Property>` instance addressed by an explicit
 * `nodes[i].next` path) since that field alone is safe and useful to edit generically.
 *
 * `next` is a CONTAINER parameter (`parameters.nodes[i].next` on the graph task itself, not a
 * subtask's own parameters), so editing it goes through the same path-addressed
 * `updateWorkflowNodeParameter` save `Property`/`useProperty` already use for every other
 * top-level parameter of the currently-selected node (`saveProperty`) — no task-dispatcher
 * subtask routing is involved.
 *
 * Each node's `next` editor is paired with a `GraphNextNodeSuggestions` row offering every
 * declared node name (including the node's own — self-loops are legal) as a one-click quoted
 * literal. See that component's doc comment for why this is an adjacent affordance rather than
 * threaded into `PropertyMentionsInput`'s `$` data-pill mention popup. A chip click REPLACES the
 * whole `next` value, so the row is disabled (`disabled={dynamic}`, reusing the same
 * `extractNextTargets` classification `GraphTransitionBadges` renders from) whenever `next` is
 * anything other than a bare literal — a hand-written ternary must not be silently clobbered by
 * a stray click. Empty `next` and a dangling bare literal are both bare-literal cases, so the row
 * stays enabled for those (a dangling literal is a one-click repair, not a clobber).
 *
 * A dangling/unresolvable `next` target already surfaces as a warning badge via
 * `GraphTransitionBadges` (see its doc comment), which is this feature's static-analysis warning
 * surface — no additional warning UI is added here.
 */
export default function GraphStatesPanel({taskDispatcherDefinition}: GraphStatesPanelPropsI) {
    const currentNode = useWorkflowNodeDetailsPanelStore((state) => state.currentNode);
    const workflowTasks = useWorkflowDataStore((state) => state.workflow.tasks);

    // Read off the LIVE task, not `currentNode.parameters`. The panel store holds the node as it was
    // when the panel opened, so adding or removing a node on the canvas left this list showing the
    // graph's membership from before the edit until the panel was closed and reopened. The snapshot
    // stays the fallback: a node being configured before its first save has no task yet.
    //
    // Flattened first — a graph nested inside another dispatcher is not a top-level task, and looking
    // only there would silently fall back to the stale snapshot for exactly those graphs.
    const graphNodes: Array<GraphNodeType> = useMemo(() => {
        const currentTask = flattenDefinitionTasks(workflowTasks ?? []).find(
            (task) => task.name === currentNode?.workflowNodeName
        );

        const taskNodes = currentTask?.parameters?.nodes as Array<GraphNodeType> | undefined;

        return taskNodes ?? (currentNode?.parameters?.nodes as Array<GraphNodeType> | undefined) ?? [];
    }, [currentNode?.parameters?.nodes, currentNode?.workflowNodeName, workflowTasks]);

    const declaredNodeNames = useMemo(() => graphNodes.map((graphNode) => graphNode.name), [graphNodes]);

    const nextPropertyDefinition = useMemo<PropertyAllType | undefined>(() => {
        const taskProperties = taskDispatcherDefinition?.taskProperties as Array<PropertyAllType> | undefined;

        const nodesTaskProperty = taskProperties?.find((property) => property.name === 'nodes');

        const nodeItemProperty = nodesTaskProperty?.items?.[0] as PropertyAllType | undefined;

        return nodeItemProperty?.properties?.find((property) => property.name === 'next');
    }, [taskDispatcherDefinition]);

    if (!graphNodes.length) {
        return (
            <p className="p-4 text-sm text-muted-foreground">
                This graph has no nodes yet. Add a node from the canvas to configure its transitions here.
            </p>
        );
    }

    return (
        <ul aria-label="Graph node states" className="space-y-4 p-4">
            {graphNodes.map((graphNode, nodeIndex) => {
                const {dangling, dynamic, targets} = extractNextTargets(graphNode.next, declaredNodeNames);

                return (
                    <li
                        aria-label={`${graphNode.name} state`}
                        className="space-y-2 rounded-md border border-stroke-neutral-secondary p-3"
                        key={`${graphNode.name}_${nodeIndex}`}
                    >
                        <div className="flex items-center justify-between gap-2">
                            <span className="text-sm font-medium">{graphNode.name}</span>

                            <GraphTransitionBadges dangling={dangling} dynamic={dynamic} targets={targets} />
                        </div>

                        {nextPropertyDefinition && (
                            <Property
                                key={`${graphNode.name}_next`}
                                path={`nodes[${nodeIndex}].next`}
                                property={nextPropertyDefinition}
                            />
                        )}

                        <GraphNextNodeSuggestions
                            disabled={dynamic}
                            nodeNames={declaredNodeNames}
                            path={`nodes[${nodeIndex}].next`}
                        />

                        {!graphNode.next && (
                            <p className="text-xs text-muted-foreground">
                                No next expression — this node is terminal, the graph run ends here.
                            </p>
                        )}
                    </li>
                );
            })}
        </ul>
    );
}
