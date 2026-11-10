import Button from '@/components/Button/Button';
import RequiredMark from '@/components/RequiredMark';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Label} from '@/components/ui/label';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {CanvasPropertyEditorProvider} from '@/pages/platform/workflow-editor/components/properties/CanvasPropertyEditorContext';
import Property from '@/pages/platform/workflow-editor/components/properties/Property';
import PropertyFormulaSwitch from '@/pages/platform/workflow-editor/components/properties/components/PropertyFormulaSwitch';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {flattenDefinitionTasks} from '@/pages/platform/workflow-editor/utils/flattenDefinitionTasks';
import {GRAPH_TRANSITION_EDITOR_ATTRIBUTE} from '@/pages/platform/workflow-editor/utils/graph/graphTransitionDeleteKey';
import {
    type GraphTransitionFieldType,
    isDynamicTransitionTarget,
    removeTransition,
    updateTransition,
    updateTransitionField,
} from '@/pages/platform/workflow-editor/utils/graph/graphTransitionMutations';
import {saveGraphTransitions} from '@/pages/platform/workflow-editor/utils/graph/saveGraphParameters';
import {registerPropertySaveOverride} from '@/pages/platform/workflow-editor/utils/propertySaveOverrides';
import {GRAPH_TRANSITION_EDGE_TYPE} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {useGetTaskDispatcherDefinitionQuery} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';
import {GraphTransitionType, NodeDataType, PropertyAllType} from '@/shared/types';
import {CircleQuestionMarkIcon, Trash2Icon, XIcon} from 'lucide-react';
import {useEffect, useId, useMemo, useRef, useState} from 'react';

const PROPERTY_LABEL_CLASSNAME = '[&_label]:text-xs [&_label]:font-normal';

interface GraphTransitionPopoverProps {
    graphId: string;
    index: number;
}

export default function GraphTransitionPopover({graphId, index}: GraphTransitionPopoverProps) {
    const [expressionModeOverride, setExpressionModeOverride] = useState<boolean | undefined>(undefined);
    const [currentNodeReachedGraph, setCurrentNodeReachedGraph] = useState(false);

    const displacedCurrentNodeRef = useRef<{node: NodeDataType | undefined} | undefined>(undefined);
    const savedFieldValuesRef = useRef<{
        index: number;
        values: Record<GraphTransitionFieldType, Array<string | undefined>>;
    }>({index, values: {condition: [], to: []}});

    const setCurrentNode = useWorkflowNodeDetailsPanelStore((state) => state.setCurrentNode);
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

    const toFieldId = useId();

    const graphTask: WorkflowTask | undefined = useMemo(() => {
        const definitionTasks = workflowTasks ?? [];

        return flattenDefinitionTasks(definitionTasks).find((task) => task.name === graphId);
    }, [graphId, workflowTasks]);

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

    const toProperty = useMemo<PropertyAllType | undefined>(() => {
        const properties = taskDispatcherDefinition?.properties as Array<PropertyAllType> | undefined;

        const transitionsProperty = properties?.find((property) => property.name === 'transitions');

        const transitionItemProperty = transitionsProperty?.items?.[0] as PropertyAllType | undefined;

        const declaredToProperty = transitionItemProperty?.properties?.find((property) => property.name === 'to');

        return declaredToProperty && {...declaredToProperty, controlType: 'FORMULA_MODE'};
    }, [taskDispatcherDefinition]);

    const dataPillAnchorNodeName = transition && memberNames.includes(transition.from) ? transition.from : undefined;

    const expressionMode = expressionModeOverride ?? (transition ? isDynamicTransitionTarget(transition.to) : false);

    const isSoleSelectedTransition = selectedTransitionEdgeCount === 1;

    const propertyEditorReady = isSoleSelectedTransition && currentNodeReachedGraph;

    function saveTransitions(mutate: (currentTransitions: Array<GraphTransitionType>) => Array<GraphTransitionType>) {
        if (!updateWorkflowMutation) {
            return;
        }

        saveGraphTransitions({graphId, mutate, updateWorkflowMutation});
    }

    function handleClose() {
        const {edges, setEdges} = useWorkflowDataStore.getState();

        const transitionEdgeId = `${graphId}-transition-${index}`;

        const deselectedEdges = edges.map((edge) => (edge.id === transitionEdgeId ? {...edge, selected: false} : edge));

        setEdges(deselectedEdges);
    }

    function handleToValueChange(value: string) {
        if (!transition) {
            return;
        }

        saveTransitions((currentTransitions) =>
            updateTransition({
                expectedTransition: transition,
                index,
                patch: {to: value},
                transitions: currentTransitions,
            })
        );
    }

    function handleDeleteClick() {
        if (!transition) {
            return;
        }

        saveTransitions((currentTransitions) =>
            removeTransition({expectedTransition: transition, index, transitions: currentTransitions})
        );
    }

    function handleToModeToggleClick() {
        const toggledExpressionMode = !expressionMode;

        setExpressionModeOverride(toggledExpressionMode);
    }

    const toModeToggle = <PropertyFormulaSwitch formulaMode={expressionMode} handleClick={handleToModeToggleClick} />;

    useEffect(() => {
        if (!transition || !updateWorkflowMutation) {
            return;
        }

        const expectedTransition = transition;
        const transitionFields: Array<GraphTransitionFieldType> = ['condition', 'to'];

        const unregisterPropertySaveOverrides = transitionFields.map((field) =>
            registerPropertySaveOverride({
                path: `transitions[${index}].${field}`,
                save: (value) => {
                    if (savedFieldValuesRef.current.index !== index) {
                        savedFieldValuesRef.current = {index, values: {condition: [], to: []}};
                    }

                    const savedFieldValues = savedFieldValuesRef.current.values[field];
                    const knownFieldValues = [...savedFieldValues];
                    const fieldValue = value === undefined || value === null ? undefined : String(value);

                    savedFieldValues.push(fieldValue);

                    saveGraphTransitions({
                        graphId,
                        mutate: (currentTransitions) =>
                            updateTransitionField({
                                expectedTransition,
                                field,
                                index,
                                knownFieldValues,
                                transitions: currentTransitions,
                                value: fieldValue,
                            }),
                        updateWorkflowMutation,
                    });
                },
                workflowNodeName: graphId,
            })
        );

        return () => {
            for (const unregisterPropertySaveOverride of unregisterPropertySaveOverrides) {
                unregisterPropertySaveOverride();
            }
        };
    }, [graphId, index, transition, updateWorkflowMutation]);

    useEffect(() => {
        if (currentNodePointsAtGraph) {
            setCurrentNodeReachedGraph(true);
        }
    }, [currentNodePointsAtGraph]);

    useEffect(() => {
        if (!graphTask || !isSoleSelectedTransition) {
            return;
        }

        const graphNode = useWorkflowDataStore.getState().nodes.find((node) => node.id === graphId);

        if (!graphNode) {
            return;
        }

        if (!displacedCurrentNodeRef.current) {
            displacedCurrentNodeRef.current = {node: useWorkflowNodeDetailsPanelStore.getState().currentNode};
        }

        const graphParameters = graphTask.parameters ?? {};

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
                            onClick={handleDeleteClick}
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
                    {!expressionMode && (
                        <div className="flex min-h-8 items-center justify-between gap-2">
                            <div className="flex items-center gap-1">
                                <Label className="gap-0 text-xs" htmlFor={toFieldId}>
                                    To{toProperty?.required && <RequiredMark />}
                                </Label>

                                {toProperty?.description && (
                                    <Tooltip>
                                        <TooltipTrigger>
                                            <CircleQuestionMarkIcon className="size-4 text-muted-foreground" />
                                        </TooltipTrigger>

                                        <TooltipContent>{toProperty.description}</TooltipContent>
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
