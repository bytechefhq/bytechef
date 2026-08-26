import Button from '@/components/Button/Button';
import RequiredMark from '@/components/RequiredMark';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Label} from '@/components/ui/label';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {CanvasPropertyEditorProvider} from '@/pages/platform/workflow-editor/components/properties/CanvasPropertyEditorContext';
import Property from '@/pages/platform/workflow-editor/components/properties/Property';
import PropertyInputTypeSwitch from '@/pages/platform/workflow-editor/components/properties/components/PropertyInputTypeSwitch';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {flattenDefinitionTasks} from '@/pages/platform/workflow-editor/utils/flattenDefinitionTasks';
import {GRAPH_TRANSITION_EDITOR_ATTRIBUTE} from '@/pages/platform/workflow-editor/utils/graph/graphTransitionDeleteKey';
import {
    isDynamicTransitionTarget,
    removeTransition,
    updateTransition,
} from '@/pages/platform/workflow-editor/utils/graph/graphTransitionMutations';
import {saveGraphTransitions} from '@/pages/platform/workflow-editor/utils/graph/saveGraphParameters';
import {GRAPH_TRANSITION_EDGE_TYPE} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {useGetTaskDispatcherDefinitionQuery} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';
import {GraphTransitionType, NodeDataType, PropertyAllType} from '@/shared/types';
import {CircleQuestionMarkIcon, Trash2Icon, XIcon} from 'lucide-react';
import {useEffect, useId, useMemo, useRef, useState} from 'react';

/**
 * The shared property editor sizes its label for a details panel, which in a popover this size reads
 * as a heading rather than a field name — and puts the two fields on different type scales, since
 * the member list is labelled by this component. Scoped to the popover rather than changed in the
 * editor, where the larger label is right.
 */
const PROPERTY_LABEL_CLASSNAME = '[&_label]:text-xs [&_label]:font-normal';

interface GraphTransitionPopoverPropsI {
    graphId: string;
    index: number;
}

/**
 * The inline editor for one `parameters.transitions[]` entry, anchored on its edge while that edge
 * is selected: where the transition goes, under what condition, and a way to delete it.
 *
 * `to` and the delete both go through `saveGraphTransitions`, the same graph-scoped save every other
 * canvas edit uses. The condition instead reuses the shared `<Property>` editor — that is what gives
 * a graph condition the same formula-mode mentions input every other expression field in the editor
 * has, rather than a bare text box that cannot resolve a data pill.
 *
 * That reuse is why this component points the details panel's current node at the graph while it is
 * open, and keeps it pointed at a FRESH copy of the graph's parameters. `<Property>` both saves
 * through `saveProperty` and resolves its displayed value from `currentNode.parameters`, so a stale
 * snapshot is not merely a wrong save target: after a transition is deleted elsewhere, the
 * surviving transitions shift index and the popover would show a condition that no longer exists.
 * Pointing the store at the graph is also what the selection means: a transition is a parameter of
 * the graph container, not of either endpoint.
 *
 * Because that pointing is a single global slot, the condition editor is offered only while this is
 * the sole selected transition edge. Under a box multi-select every selected edge mounts its own
 * editor, and two of them from different graphs would each repoint the same slot — so they all step
 * back and say so, rather than one silently writing against the other's graph.
 */
export default function GraphTransitionPopover({graphId, index}: GraphTransitionPopoverPropsI) {
    // `undefined` means "follow the saved value"; a boolean is the user having switched the To field
    // by hand, which must survive the transition still holding its previous kind of target.
    const [expressionModeOverride, setExpressionModeOverride] = useState<boolean | undefined>(undefined);
    // What `currentNode` held before this editor repointed it, restored on unmount so deselecting an
    // edge does not leave the details panel permanently parked on the graph.
    const displacedCurrentNodeRef = useRef<{node: NodeDataType | undefined} | undefined>(undefined);

    const toFieldId = useId();

    const setCurrentNode = useWorkflowNodeDetailsPanelStore((state) => state.setCurrentNode);
    /**
     * Whether the panel store has actually been pointed at this graph yet.
     *
     * `<Property>` resolves the value it displays from `currentNode.parameters` ONCE, on mount, and
     * never again for the same path — and a child's mount effect runs before its parent's, so a
     * `<Property>` rendered in the same commit as the effect below would resolve against whichever
     * node the panel was showing a moment ago. It found no transitions there and showed an empty
     * condition, with the expression sitting in plain view on the edge beside it, until the page was
     * reloaded. Waiting one commit costs nothing and is what makes the value appear.
     */
    const currentNodePointsAtGraph = useWorkflowNodeDetailsPanelStore(
        (state) => state.currentNode?.workflowNodeName === graphId
    );
    const workflowTasks = useWorkflowDataStore((state) => state.workflow.tasks);
    const selectedTransitionEdgeCount = useWorkflowDataStore(
        (state) => state.edges.filter((edge) => edge.selected && edge.type === GRAPH_TRANSITION_EDGE_TYPE).length
    );

    const {updateWorkflowMutation} = useWorkflowEditor();

    const {data: taskDispatcherDefinition} = useGetTaskDispatcherDefinitionQuery({
        taskDispatcherName: 'graph',
        taskDispatcherVersion: 1,
    });

    // Read off the LIVE task, not the panel store's snapshot: an edit made elsewhere on the canvas
    // must be reflected here while the popover stays open. Flattened first, because a graph nested
    // inside another dispatcher is not a top-level task.
    const graphTask: WorkflowTask | undefined = useMemo(
        () => flattenDefinitionTasks(workflowTasks ?? []).find((task) => task.name === graphId),
        [graphId, workflowTasks]
    );

    const memberNames = useMemo(
        () => ((graphTask?.parameters?.nodes as Array<WorkflowTask> | undefined) ?? []).map((node) => node.name),
        [graphTask]
    );

    const transition = ((graphTask?.parameters?.transitions as Array<GraphTransitionType> | undefined) ?? [])[index] as
        GraphTransitionType | undefined;

    const conditionProperty = useMemo<PropertyAllType | undefined>(() => {
        const properties = taskDispatcherDefinition?.properties as Array<PropertyAllType> | undefined;

        const transitionsProperty = properties?.find((property) => property.name === 'transitions');

        const transitionItemProperty = transitionsProperty?.items?.[0] as PropertyAllType | undefined;

        return transitionItemProperty?.properties?.find((property) => property.name === 'condition');
    }, [taskDispatcherDefinition]);

    // The definition declares `to` as a plain string, because the members it may name are known
    // only on the canvas. Formula mode is what the popover offers ON TOP of that list, so the
    // control type is set here rather than server-side, where it would force every graph's target
    // to be written as an expression.
    const toProperty = useMemo<PropertyAllType | undefined>(() => {
        const properties = taskDispatcherDefinition?.properties as Array<PropertyAllType> | undefined;

        const transitionsProperty = properties?.find((property) => property.name === 'transitions');

        const transitionItemProperty = transitionsProperty?.items?.[0] as PropertyAllType | undefined;

        const declaredToProperty = transitionItemProperty?.properties?.find((property) => property.name === 'to');

        return declaredToProperty && {...declaredToProperty, controlType: 'FORMULA_MODE'};
    }, [taskDispatcherDefinition]);

    /**
     * What the condition's data pills are resolved against: the member this transition ENTERS.
     *
     * The condition decides whether that member is entered, and is evaluated in the context the
     * member would run in — so it offers exactly what editing that member offers, which is what puts
     * the transition's own source among the pills. Undefined for a target written as an expression,
     * where there is no member to resolve against; the panel then falls back to the graph container,
     * which is what every transition had before.
     */
    const dataPillAnchorNodeName =
        transition && !isDynamicTransitionTarget(transition.to) && memberNames.includes(transition.to)
            ? transition.to
            : undefined;

    const expressionMode = expressionModeOverride ?? (transition ? isDynamicTransitionTarget(transition.to) : false);

    const isSoleSelectedTransition = selectedTransitionEdgeCount === 1;

    // Both editors save through, and read from, the graph the store has been pointed at — but only
    // the FIRST time it gets there. Re-reading it every render made the gate a live condition, so a
    // `currentNode` cleared for even one commit unmounted the editor mid-edit: the caret went, and
    // the editor's unmount effect flushed its pending save into a store that no longer named a node,
    // which dropped the keystrokes it was flushing. Latching keeps the editor mounted for as long as
    // this popover lives; when the popover really closes it takes the editor with it anyway.
    const [currentNodeReachedGraph, setCurrentNodeReachedGraph] = useState(false);

    const propertyEditorReady = isSoleSelectedTransition && currentNodeReachedGraph;

    // Keyed on `graphTask` identity, not on whether the store already names this graph: the store
    // holds a COPY of the parameters, and nothing in the save path refreshes it (`saveGraphParameters`
    // persists through `saveWorkflowDefinition`, which never touches the panel store). Re-pointing
    // only when the name differs would leave the first snapshot in place for the rest of the
    // session, so a transition deleted elsewhere would shift the indexes underneath a later-opened
    // editor and show it a condition belonging to a transition that no longer exists.
    useEffect(() => {
        if (currentNodePointsAtGraph) {
            setCurrentNodeReachedGraph(true);
        }
    }, [currentNodePointsAtGraph]);

    useEffect(() => {
        if (!graphTask || !isSoleSelectedTransition) {
            return;
        }

        // Deliberately NOT held off while the condition has focus, tempting as that is to quieten the
        // re-render on every debounced save: `<Property>` reads its displayed value from this
        // snapshot, so freezing it shows a blank condition whose expression is sitting right there on
        // the edge. Keeping the editor's own document intact is `PropertyMentionsInputEditor`'s job,
        // and it refuses to replace one the user is working in.
        const graphNode = useWorkflowDataStore.getState().nodes.find((node) => node.id === graphId);

        if (!graphNode) {
            return;
        }

        if (!displacedCurrentNodeRef.current) {
            displacedCurrentNodeRef.current = {node: useWorkflowNodeDetailsPanelStore.getState().currentNode};
        }

        const graphParameters = graphTask.parameters ?? {};

        // Everything the details panel derives from the node other than the parameters — label,
        // display conditions, the metadata `saveProperty` writes back — is kept when the store is
        // already on this graph, so a refresh replaces the stale parameters and nothing else.
        setCurrentNode((previousCurrentNode) => ({
            ...(graphNode.data as NodeDataType),
            ...(previousCurrentNode?.workflowNodeName === graphId ? previousCurrentNode : {}),
            dataPillAnchorNodeName,
            parameters: graphParameters,
        }));
    }, [dataPillAnchorNodeName, graphId, graphTask, isSoleSelectedTransition, setCurrentNode]);

    useEffect(
        () => () => {
            if (displacedCurrentNodeRef.current) {
                setCurrentNode(displacedCurrentNodeRef.current.node);
            }
        },
        [setCurrentNode]
    );

    if (!transition) {
        return null;
    }

    function saveTransitions(mutate: (currentTransitions: Array<GraphTransitionType>) => Array<GraphTransitionType>) {
        if (!updateWorkflowMutation) {
            return;
        }

        saveGraphTransitions(graphId, mutate, updateWorkflowMutation);
    }

    // The popover is anchored on the edge's selection rather than on state of its own, so closing it
    // means deselecting that edge. Hiding it any other way would leave the canvas still showing the
    // transition as selected, with no way to bring the editor back without deselecting first.
    function handleClose() {
        const {edges, setEdges} = useWorkflowDataStore.getState();

        setEdges(
            edges.map((edge) => (edge.id === `${graphId}-transition-${index}` ? {...edge, selected: false} : edge))
        );
    }

    function handleToValueChange(value: string) {
        saveTransitions((currentTransitions) => updateTransition(currentTransitions, index, {to: value}));
    }

    // The same Dynamic switch every other property field carries, rather than a control of this
    // popover's own: picking a member and writing an expression is the constant/dynamic choice the
    // rest of the editor already names that way, and a bespoke icon made it look like a different
    // kind of decision.
    const toModeToggle = (
        <PropertyInputTypeSwitch
            handleClick={() => setExpressionModeOverride(!expressionMode)}
            mentionInput={expressionMode}
        />
    );

    return (
        <CanvasPropertyEditorProvider value>
            <div
                {...{[GRAPH_TRANSITION_EDITOR_ATTRIBUTE]: ''}}
                aria-label={`Transition ${transition.from} to ${transition.to}`}
                className="w-[25rem] space-y-3 rounded-md border border-stroke-neutral-secondary bg-background p-3 shadow-lg"
            >
                <div className="flex items-center justify-between gap-2">
                    <span className="min-w-0 truncate text-sm font-medium">
                        {transition.from} &rarr; {transition.to}
                    </span>

                    <div className="flex shrink-0 items-center gap-1">
                        <Button
                            aria-label="Delete transition"
                            icon={<Trash2Icon />}
                            onClick={() =>
                                saveTransitions((currentTransitions) => removeTransition(currentTransitions, index))
                            }
                            size="iconXs"
                            variant="destructiveGhost"
                        />

                        <Button
                            aria-label="Close"
                            icon={<XIcon />}
                            onClick={handleClose}
                            size="iconXs"
                            variant="ghost"
                        />
                    </div>
                </div>

                <fieldset className="space-y-1 border-0">
                    {/* Picking a member and writing an expression are two ways of saying where the
                        transition goes, so the choice belongs on a switch beside the field. Offered as a
                        dropdown entry it read as a third destination, and hid the fact that a target
                        expression gets the same editor a condition does.

                        In expression mode it rides in the shared editor's own header slot rather than on
                        a row of its own, so it sits beside that editor's expand button instead of
                        stacking a second lone icon above it. */}

                    {/* The label row belongs to the member list only. In expression mode the shared
                        editor prints its own label and carries the toggle in its header slot, so both
                        controls sit on one line instead of the toggle stacking above them. */}

                    {/* Same height as the shared editor's own header, which carries an icon button
                        beside the toggle in expression mode. Without it the row changed height as the
                        switch was flipped and everything below it jumped. */}

                    {!expressionMode && (
                        <div className="flex min-h-8 items-center justify-between gap-2">
                            <div className="flex items-center gap-1">
                                {/* The shared editor marks the expression side required off the same
                                    declaration; carrying it here keeps the field looking equally
                                    required whichever way it is being written. */}

                                {/* `gap-0` because Label is a flex row with a gap of its own, which
                                    would otherwise push the required mark a space away from the word —
                                    the shared editor's label neutralises it the same way, and the mark
                                    carries the small margin it wants itself. */}

                                <Label className="gap-0 text-xs" htmlFor={toFieldId}>
                                    To{toProperty?.required && <RequiredMark />}
                                </Label>

                                {/* The expression side gets this from the shared editor. Carrying it on
                                    the member list too means the field is described the same either way,
                                    rather than losing its description exactly where it is least obvious
                                    — a list of node names says nothing about naming one indirectly. */}

                                {toProperty?.description && (
                                    <Tooltip>
                                        <TooltipTrigger>
                                            <CircleQuestionMarkIcon className="size-4 text-muted-foreground" />
                                        </TooltipTrigger>

                                        <TooltipContent className="max-w-tooltip-sm">
                                            {toProperty.description}
                                        </TooltipContent>
                                    </Tooltip>
                                )}
                            </div>

                            {toModeToggle}
                        </div>
                    )}

                    {expressionMode ? (
                        toProperty &&
                        (isSoleSelectedTransition ? (
                            propertyEditorReady && (
                                <Property
                                    customClassName={PROPERTY_LABEL_CLASSNAME}
                                    deletePropertyButton={toModeToggle}
                                    path={`transitions[${index}].to`}
                                    property={toProperty}
                                />
                            )
                        ) : (
                            <div className="flex items-center justify-between gap-2">
                                <p className="text-xs text-muted-foreground">
                                    Select a single transition to write its target as an expression.
                                </p>

                                {toModeToggle}
                            </div>
                        ))
                    ) : (
                        <>
                            <Select onValueChange={handleToValueChange} value={transition.to}>
                                <SelectTrigger aria-label="To" className="h-8" id={toFieldId}>
                                    <SelectValue placeholder="Choose a node..." />
                                </SelectTrigger>

                                <SelectContent>
                                    {memberNames.map((memberName) => (
                                        <SelectItem key={memberName} value={memberName}>
                                            {memberName}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </>
                    )}
                </fieldset>

                {conditionProperty &&
                    (isSoleSelectedTransition ? (
                        propertyEditorReady && (
                            <Property
                                customClassName={PROPERTY_LABEL_CLASSNAME}
                                path={`transitions[${index}].condition`}
                                property={conditionProperty}
                            />
                        )
                    ) : (
                        <p className="text-xs text-muted-foreground">
                            Select a single transition to edit its condition.
                        </p>
                    ))}
            </div>
        </CanvasPropertyEditorProvider>
    );
}
