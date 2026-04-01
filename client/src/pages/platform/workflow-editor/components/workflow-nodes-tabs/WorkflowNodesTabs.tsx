import {Tabs, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {getClusterElementsLabel} from '@/pages/platform/cluster-element-editor/utils/clusterElementsUtils';
import {ComponentDefinitionBasic, TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {ClickedDefinitionType, UpdateWorkflowMutationType} from '@/shared/types';
import {ClipboardPasteIcon, ClipboardXIcon} from 'lucide-react';
import {useEffect, useMemo, useRef, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

import {useComponentFiltering} from '../../hooks/useComponentFiltering';
import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../../stores/useWorkflowEditorStore';
import getTaskDispatcherContext from '../../utils/getTaskDispatcherContext';
import pasteNode from '../../utils/pasteNode';
import ComponentsFilter from '../filters/ComponentsFilter';
import WorkflowNodesTabContent from './WorkflowNodesTabContent';

type DefinitionType = (ComponentDefinitionBasic | TaskDispatcherDefinition) & {
    taskDispatcher: boolean;
    trigger: boolean;
};

interface WorkflowNodesTabsProps {
    actionComponentDefinitions: Array<ComponentDefinitionBasic>;
    clusterElementComponentDefinitions?: Array<ComponentDefinitionBasic>;
    clusterElementType?: string;
    edgeId?: string;
    hideActionComponents?: boolean;
    hideClusterElementComponents?: boolean;
    hideTaskDispatchers?: boolean;
    hideTriggerComponents?: boolean;
    itemsDraggable?: boolean;
    onItemClick?: (clickedItem: ClickedDefinitionType) => void;
    onPasteClose?: () => void;
    selectedComponentName?: string;
    showPaste?: boolean;
    sourceNodeId?: string;
    taskDispatcherDefinitions: Array<TaskDispatcherDefinition>;
    triggerComponentDefinitions: Array<ComponentDefinitionBasic>;
    updateWorkflowMutation?: UpdateWorkflowMutationType;
}

const WorkflowNodesTabs = ({
    actionComponentDefinitions,
    clusterElementComponentDefinitions,
    clusterElementType,
    edgeId,
    hideActionComponents = false,
    hideClusterElementComponents = false,
    hideTaskDispatchers = false,
    hideTriggerComponents = false,
    itemsDraggable = false,
    onItemClick,
    onPasteClose,
    selectedComponentName,
    showPaste = false,
    sourceNodeId,
    taskDispatcherDefinitions,
    triggerComponentDefinitions,
    updateWorkflowMutation,
}: WorkflowNodesTabsProps) => {
    const [activeTab, setActiveTab] = useState(
        !hideActionComponents ? 'components' : !hideClusterElementComponents ? 'clusterElements' : 'triggers'
    );

    const [pasteDismissed, setPasteDismissed] = useState(false);

    const {copiedNode, copiedWorkflowId} = useWorkflowEditorStore(
        useShallow((state) => ({
            copiedNode: state.copiedNode,
            copiedWorkflowId: state.copiedWorkflowId,
        }))
    );

    const {edges, nodes, workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            edges: state.edges,
            nodes: state.nodes,
            workflow: state.workflow,
        }))
    );

    const canPaste = showPaste && !!copiedNode && copiedWorkflowId === workflow.id;

    const handlePasteClick = () => {
        if (!canPaste || !updateWorkflowMutation) {
            return;
        }

        const nodeSourceName = sourceNodeId || edgeId?.split('=>')[0];

        if (!nodeSourceName) {
            return;
        }

        const edge = edges.find((currentEdge) => currentEdge.id === edgeId);
        const sourceNode = nodes.find((currentNode) => currentNode.id === sourceNodeId);

        const taskDispatcherContext = getTaskDispatcherContext({
            edge,
            node: edge?.type === 'workflow' ? undefined : sourceNode,
            nodes,
        });

        pasteNode({
            nodeSourceName,
            taskDispatcherContext,
            updateWorkflowMutation,
        });

        if (onPasteClose) {
            onPasteClose();
        }
    };

    const ff_1057 = useFeatureFlagsStore()('ff-1057');

    const actionFiltering = useComponentFiltering({
        componentDefinitions: actionComponentDefinitions,
    });

    const triggerFiltering = useComponentFiltering({
        componentDefinitions: triggerComponentDefinitions,
    });

    const previousActionComponentsCountRef = useRef(actionComponentDefinitions.length);
    const previousTriggerComponentsCountRef = useRef(triggerComponentDefinitions.length);

    useEffect(() => {
        const currentCount = actionComponentDefinitions.length;

        if (previousActionComponentsCountRef.current === currentCount) {
            return;
        }

        previousActionComponentsCountRef.current = currentCount;

        if (actionFiltering.filterState.selectedCategories.length > 0) {
            actionFiltering.setActiveView(actionFiltering.filterState.activeView);
        } else if (actionFiltering.filterState.activeView === 'filtered') {
            actionFiltering.setActiveView('all');
        }
    }, [actionComponentDefinitions.length, actionFiltering]);

    useEffect(() => {
        const currentCount = triggerComponentDefinitions.length;

        if (previousTriggerComponentsCountRef.current === currentCount) {
            return;
        }

        previousTriggerComponentsCountRef.current = currentCount;

        if (triggerFiltering.filterState.selectedCategories.length > 0) {
            triggerFiltering.setActiveView(triggerFiltering.filterState.activeView);
        } else if (triggerFiltering.filterState.activeView === 'filtered') {
            triggerFiltering.setActiveView('all');
        }
    }, [triggerComponentDefinitions.length, triggerFiltering]);

    const availableTriggers = useMemo(() => {
        return triggerFiltering.filteredComponents.map(
            (triggerDefinition: ComponentDefinitionBasic) =>
                ({
                    ...triggerDefinition,
                    trigger: true,
                }) as DefinitionType
        );
    }, [triggerFiltering.filteredComponents]);

    const availableTaskDispatchers = useMemo(() => {
        let availableTaskDispatchers;

        if (ff_1057) {
            availableTaskDispatchers = taskDispatcherDefinitions;
        } else {
            availableTaskDispatchers = taskDispatcherDefinitions.filter(
                (taskDispatcherDefinition) =>
                    taskDispatcherDefinition.name === 'branch' ||
                    taskDispatcherDefinition.name === 'condition' ||
                    taskDispatcherDefinition.name === 'loop'
            );
        }

        return availableTaskDispatchers.map(
            (dispatcher) =>
                ({
                    ...dispatcher,
                    taskDispatcher: true,
                }) as DefinitionType
        );
    }, [ff_1057, taskDispatcherDefinitions]);

    const availableClusterElements = useMemo(
        () =>
            clusterElementComponentDefinitions?.map((clusterElementDefinition) => ({
                ...clusterElementDefinition,
                clusterElement: true,
                taskDispatcher: false,
                trigger: false,
            })),
        [clusterElementComponentDefinitions]
    );

    const tabContentConfigs = useMemo(
        () => ({
            clusterElements: {
                emptyMessage: 'No cluster element components found.',
                items: availableClusterElements,
            },
            components: {
                emptyMessage:
                    actionFiltering.filterState.activeView === 'all'
                        ? 'No action components found.'
                        : 'No filtered components found.',
                items: actionFiltering.filteredComponents,
            },
            taskDispatchers: {
                emptyMessage: 'No flow controls found.',
                items: availableTaskDispatchers,
            },
            triggers: {
                emptyMessage:
                    triggerFiltering.filterState.activeView === 'all'
                        ? 'No trigger components found.'
                        : 'No filtered components found.',
                items: availableTriggers,
            },
        }),
        [
            availableTriggers,
            actionFiltering.filteredComponents,
            actionFiltering.filterState.activeView,
            availableTaskDispatchers,
            availableClusterElements,
            triggerFiltering.filterState.activeView,
        ]
    );

    return (
        <Tabs className="flex h-full flex-col" onValueChange={setActiveTab} value={activeTab}>
            <div className="px-2">
                <TabsList className="my-2 flex w-full justify-between bg-surface-neutral-secondary">
                    {!hideTriggerComponents && (
                        <TabsTrigger className="w-full data-[state=active]:shadow-none" value="triggers">
                            Triggers
                        </TabsTrigger>
                    )}

                    {!hideActionComponents && (
                        <TabsTrigger className="w-full data-[state=active]:shadow-none" value="components">
                            Actions
                        </TabsTrigger>
                    )}

                    {!hideTaskDispatchers && (
                        <TabsTrigger className="w-full data-[state=active]:shadow-none" value="taskDispatchers">
                            Flows
                        </TabsTrigger>
                    )}

                    {!hideClusterElementComponents && (
                        <TabsTrigger className="w-full data-[state=active]:shadow-none" value="clusterElements">
                            {clusterElementType ? getClusterElementsLabel(clusterElementType) : 'Cluster Elements'}
                        </TabsTrigger>
                    )}
                </TabsList>
            </div>

            {activeTab === 'triggers' && (
                <ComponentsFilter
                    componentDefinitions={triggerComponentDefinitions}
                    deselectAllCategories={triggerFiltering.deselectAllCategories}
                    filterConfig={{
                        label: 'triggers',
                        tooltip: 'Filter triggers by category',
                    }}
                    filterState={triggerFiltering.filterState}
                    filteredCategories={triggerFiltering.filteredCategories}
                    filteredComponents={triggerFiltering.filteredComponents}
                    setActiveView={triggerFiltering.setActiveView}
                    setSearchValue={triggerFiltering.setSearchValue}
                    toggleCategory={triggerFiltering.toggleCategory}
                />
            )}

            {activeTab === 'components' && (
                <ComponentsFilter
                    componentDefinitions={actionComponentDefinitions}
                    deselectAllCategories={actionFiltering.deselectAllCategories}
                    filterConfig={{
                        label: 'actions',
                        tooltip: 'Filter actions by category',
                    }}
                    filterState={actionFiltering.filterState}
                    filteredCategories={actionFiltering.filteredCategories}
                    filteredComponents={actionFiltering.filteredComponents}
                    setActiveView={actionFiltering.setActiveView}
                    setSearchValue={actionFiltering.setSearchValue}
                    toggleCategory={actionFiltering.toggleCategory}
                />
            )}

            {canPaste && !pasteDismissed && (
                <div className="px-3 py-2">
                    <div className="flex w-full overflow-hidden rounded-md border-2 border-stroke-brand-primary bg-surface-neutral-primary">
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <div
                                    className="group/paste flex h-9 min-w-0 flex-1 cursor-pointer items-center gap-2 px-2 hover:bg-surface-brand-secondary active:bg-surface-brand-secondary"
                                    onClick={handlePasteClick}
                                >
                                    <ClipboardPasteIcon className="size-4 shrink-0 text-content-neutral-primary group-active/paste:text-content-brand-primary" />

                                    <span className="text-sm font-medium text-content-neutral-primary group-active/paste:text-content-brand-primary">
                                        Paste
                                    </span>

                                    {copiedNode?.icon && (
                                        <span className="flex size-5 shrink-0 items-center justify-center [&_svg]:size-5">
                                            {copiedNode.icon}
                                        </span>
                                    )}

                                    <span className="min-w-0 flex-1 truncate text-sm text-content-neutral-primary group-active/paste:text-content-brand-primary">
                                        <span className="font-medium">
                                            {copiedNode?.label || copiedNode?.componentName}
                                        </span>

                                        {copiedNode?.operationName && (
                                            <span className="font-normal text-content-neutral-secondary group-active/paste:text-content-brand-primary">
                                                {` (${copiedNode.operationName})`}
                                            </span>
                                        )}
                                    </span>
                                </div>
                            </TooltipTrigger>

                            <TooltipContent side="top">
                                {[
                                    copiedNode?.label || copiedNode?.componentName,
                                    copiedNode?.operationName ? `(${copiedNode.operationName})` : null,
                                ]
                                    .filter(Boolean)
                                    .join(' ')}
                            </TooltipContent>
                        </Tooltip>

                        <button
                            className="group/discard flex min-w-9 cursor-pointer items-center justify-center self-stretch hover:bg-surface-brand-secondary active:bg-surface-brand-secondary"
                            onClick={(e) => {
                                e.stopPropagation();
                                setPasteDismissed(true);
                            }}
                            type="button"
                        >
                            <ClipboardXIcon className="size-4 text-content-neutral-primary opacity-50 group-hover/discard:opacity-100 group-active/discard:text-content-brand-primary group-active/discard:opacity-100" />
                        </button>
                    </div>
                </div>
            )}

            {!hideClusterElementComponents && (
                <WorkflowNodesTabContent
                    emptyMessage={tabContentConfigs.clusterElements.emptyMessage}
                    items={tabContentConfigs.clusterElements.items}
                    itemsDraggable={itemsDraggable}
                    onItemClick={onItemClick}
                    selectedComponentName={selectedComponentName}
                    tabValue="clusterElements"
                />
            )}

            {!hideTriggerComponents && (
                <WorkflowNodesTabContent
                    emptyMessage={tabContentConfigs.triggers.emptyMessage}
                    items={tabContentConfigs.triggers.items}
                    itemsDraggable={itemsDraggable}
                    onItemClick={onItemClick}
                    selectedComponentName={selectedComponentName}
                    tabValue="triggers"
                />
            )}

            {!hideActionComponents && (
                <WorkflowNodesTabContent
                    emptyMessage={tabContentConfigs.components.emptyMessage}
                    items={tabContentConfigs.components.items}
                    itemsDraggable={itemsDraggable}
                    onItemClick={onItemClick}
                    selectedComponentName={selectedComponentName}
                    tabValue="components"
                />
            )}

            {!hideTaskDispatchers && (
                <WorkflowNodesTabContent
                    emptyMessage={tabContentConfigs.taskDispatchers.emptyMessage}
                    items={tabContentConfigs.taskDispatchers.items}
                    itemsDraggable={itemsDraggable}
                    onItemClick={onItemClick}
                    selectedComponentName={selectedComponentName}
                    tabValue="taskDispatchers"
                />
            )}
        </Tabs>
    );
};

export default WorkflowNodesTabs;
