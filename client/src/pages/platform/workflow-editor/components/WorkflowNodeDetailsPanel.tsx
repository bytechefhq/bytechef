import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import {ScrollArea} from '@/components/ui/scroll-area';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Skeleton} from '@/components/ui/skeleton';
import {Tooltip, TooltipContent, TooltipPortal, TooltipTrigger} from '@/components/ui/tooltip';
import CurrentOperationSelect from '@/pages/platform/workflow-editor/components/CurrentOperationSelect';
import DescriptionTab from '@/pages/platform/workflow-editor/components/node-details-tabs/DescriptionTab';
import ConnectionTab from '@/pages/platform/workflow-editor/components/node-details-tabs/connection-tab/ConnectionTab';
import OutputTab from '@/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputTab';
import Properties from '@/pages/platform/workflow-editor/components/properties/Properties';
import useCopilotLayoutShifted from '@/shared/components/copilot/hooks/useCopilotLayoutShifted';
import {
    ActionDefinition,
    ComponentDefinitionBasic,
    WorkflowNodeOutput,
} from '@/shared/middleware/platform/configuration';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {ExternalLinkIcon, InfoIcon, XIcon} from 'lucide-react';
import {ReactNode} from 'react';
import InlineSVG from 'react-inlinesvg';
import {Link} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

import {getClusterElementsLabel} from '../../cluster-element-editor/utils/clusterElementsUtils';
import getNodeOperationDescription from '../utils/getNodeOperationDescription';
import {DescriptionTabSkeleton, FieldsetSkeleton, PropertiesTabSkeleton} from './WorkflowEditorSkeletons';
import useWorkflowNodeDetailsPanel from './hooks/useWorkflowNodeDetailsPanel';

interface WorkflowNodeDetailsPanelProps {
    className?: string;
    closeButton?: ReactNode;
    onClose?: () => void;
    panelOpen?: boolean;
    previousComponentDefinitions: Array<ComponentDefinitionBasic>;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflowNodeOutputs: WorkflowNodeOutput[];
}

const WorkflowNodeDetailsPanel = ({
    className,
    closeButton,
    onClose,
    panelOpen,
    previousComponentDefinitions,
    updateWorkflowMutation,
    workflowNodeOutputs,
}: WorkflowNodeDetailsPanelProps) => {
    const copilotLayoutShifted = useCopilotLayoutShifted();
    const {
        activeDisplayConditionsQuery,
        activeTab,
        currentActionDefinition,
        currentComponentDefinition,
        currentNode,
        currentOperationName,
        currentOperationProperties,
        currentTaskDispatcherDefinition,
        currentTriggerDefinition,
        currentWorkflowNode,
        currentWorkflowNodeConnections,
        currentWorkflowNodeOperations,
        filteredClusterElementOperations,
        getNodeVersion,
        handleOperationSelectChange,
        handlePanelClose,
        nodeDefinition,
        nodeTabs,
        operationDataMissing,
        outputDefined,
        outputFunctionDefined,
        rootClusterElementNodeData,
        setActiveTab,
        tabDataExists,
        workflow,
        workflowNodeDetailsPanelOpen,
        workflowTestConfigurationConnections,
    } = useWorkflowNodeDetailsPanel({
        previousComponentDefinitions,
        updateWorkflowMutation,
        workflowNodeOutputs,
    });

    if (!(panelOpen ?? workflowNodeDetailsPanelOpen)) {
        return <></>;
    }

    const documentationUrl =
        currentActionDefinition?.help?.learnMoreUrl ?? currentTriggerDefinition?.help?.learnMoreUrl;

    return (
        <div
            className={twMerge(
                'absolute top-2 bottom-6 z-10 w-screen max-w-workflow-node-details-panel-width overflow-hidden rounded-md border border-stroke-neutral-secondary bg-background',
                copilotLayoutShifted ? 'right-[57px]' : 'right-[69px]',
                !className && 'animate-[slideInFromRight_300ms_ease-out]',
                className
            )}
        >
            <div
                aria-label={`${currentNode?.workflowNodeName} component configuration panel`}
                className="h-full"
                key={`${currentNode?.workflowNodeName}-${currentNode?.operationName}`}
            >
                {currentNode?.workflowNodeName && currentWorkflowNode && (
                    <div className="flex h-full flex-col divide-y divide-muted bg-background">
                        <header className="flex items-center justify-between p-4 text-lg font-medium">
                            <div className="flex min-w-0 flex-1 items-center gap-2">
                                {currentWorkflowNode.icon && (
                                    <InlineSVG
                                        className="size-8 shrink-0"
                                        loader={<LoadingIcon className="mr-2 ml-0 size-6" />}
                                        src={currentWorkflowNode.icon}
                                    />
                                )}

                                <div className="flex min-w-0 flex-1 flex-col items-start">
                                    <div className="flex max-w-full min-w-0 items-center gap-2">
                                        {(currentNode?.label?.length ?? 0) > 32 ? (
                                            <Tooltip>
                                                <TooltipTrigger asChild>
                                                    <span className="min-w-0 flex-1 truncate text-lg font-semibold">
                                                        {currentNode?.label}
                                                    </span>
                                                </TooltipTrigger>

                                                <TooltipPortal>
                                                    <TooltipContent className="max-w-md break-all" side="bottom">
                                                        {currentNode?.label}
                                                    </TooltipContent>
                                                </TooltipPortal>
                                            </Tooltip>
                                        ) : (
                                            <span className="min-w-0 flex-1 truncate text-lg font-semibold">
                                                {currentNode?.label}
                                            </span>
                                        )}

                                        {currentWorkflowNode.description && (
                                            <Tooltip>
                                                <TooltipTrigger>
                                                    <InfoIcon className="size-4" />
                                                </TooltipTrigger>

                                                <TooltipPortal>
                                                    <TooltipContent className="max-w-md" side="bottom">
                                                        {currentComponentDefinition
                                                            ? currentComponentDefinition.description
                                                            : currentTaskDispatcherDefinition?.description}
                                                    </TooltipContent>
                                                </TooltipPortal>
                                            </Tooltip>
                                        )}
                                    </div>

                                    <span className="w-full truncate text-sm text-muted-foreground">
                                        ({currentNode?.workflowNodeName})
                                    </span>
                                </div>
                            </div>

                            {closeButton ? (
                                closeButton
                            ) : (
                                <Button
                                    aria-label="Close the node details dialog"
                                    icon={<XIcon aria-hidden="true" />}
                                    onClick={onClose || handlePanelClose}
                                    size="icon"
                                    variant="ghost"
                                />
                            )}
                        </header>

                        <main className="flex h-full flex-col overflow-hidden">
                            {!!currentWorkflowNodeOperations?.length && operationDataMissing && (
                                <FieldsetSkeleton bottomBorder label="Actions" />
                            )}

                            {currentWorkflowNodeOperations && !operationDataMissing && (
                                <CurrentOperationSelect
                                    clusterElementLabel={
                                        currentNode.clusterElementType &&
                                        getClusterElementsLabel(currentNode.clusterElementType)
                                    }
                                    description={getNodeOperationDescription({
                                        actionDescription: currentActionDefinition?.description,
                                        clusterElementOperations: filteredClusterElementOperations,
                                        currentNode,
                                        currentOperationName,
                                        rootClusterElementWorkflowNodeName:
                                            rootClusterElementNodeData?.workflowNodeName,
                                        triggerDescription: currentTriggerDefinition?.description,
                                    })}
                                    handleValueChange={handleOperationSelectChange}
                                    operations={
                                        (currentNode?.trigger
                                            ? currentComponentDefinition?.triggers
                                            : !!currentNode?.clusterElementType &&
                                                currentNode?.workflowNodeName !==
                                                    rootClusterElementNodeData?.workflowNodeName
                                              ? filteredClusterElementOperations
                                              : currentComponentDefinition?.actions)!
                                    }
                                    triggerSelect={currentNode?.trigger}
                                    value={currentOperationName}
                                />
                            )}

                            {tabDataExists && (
                                <div className="flex justify-center">
                                    {nodeTabs.map((tab) => (
                                        <Button
                                            className={twMerge(
                                                'grow justify-center rounded-none border-0 border-b border-border bg-content-onsurface-primary py-5 text-sm font-medium whitespace-nowrap text-content-neutral-secondary hover:border-stroke-brand-primary hover:text-content-brand-primary focus:border-stroke-brand-primary focus:text-content-brand-primary focus:outline-hidden',
                                                activeTab === tab?.name &&
                                                    'border-stroke-brand-primary text-content-brand-primary hover:text-content-brand-primary'
                                            )}
                                            key={tab.name}
                                            label={tab.label}
                                            name={tab.name}
                                            onClick={() => setActiveTab(tab.name)}
                                            variant="ghost"
                                        />
                                    ))}
                                </div>
                            )}

                            {currentNode.componentName !== 'manual' && !tabDataExists && (
                                <div className="flex justify-center space-x-2 border-b border-border p-2">
                                    {Array.from({length: 4}).map((_, index) => (
                                        <Skeleton className="h-6 w-1/4" key={index} />
                                    ))}
                                </div>
                            )}

                            <ScrollArea className="h-full max-w-workflow-node-details-panel-width bg-surface-main [&_[data-radix-scroll-area-viewport]>div]:block! [&>div]:relative">
                                <div className="size-full max-w-workflow-node-details-panel-width">
                                    {activeTab === 'description' &&
                                        (nodeDefinition ? (
                                            <DescriptionTab
                                                key={`${currentNode?.componentName}-${currentNode?.type}_description`}
                                                nodeDefinition={nodeDefinition}
                                                updateWorkflowMutation={updateWorkflowMutation}
                                            />
                                        ) : (
                                            <DescriptionTabSkeleton />
                                        ))}

                                    {activeTab === 'connection' &&
                                        (currentWorkflowNodeConnections.length > 0 ||
                                            currentComponentDefinition?.connection !== undefined) &&
                                        currentNode &&
                                        currentComponentDefinition && (
                                            <ConnectionTab
                                                componentConnections={currentWorkflowNodeConnections}
                                                currentComponentDefinition={currentComponentDefinition}
                                                key={`${currentNode?.workflowNodeName}_connection`}
                                                updateWorkflowMutation={updateWorkflowMutation}
                                                workflowId={workflow.id!}
                                                workflowNodeName={currentNode?.workflowNodeName}
                                                workflowTestConfigurationConnections={
                                                    workflowTestConfigurationConnections
                                                }
                                            />
                                        )}

                                    {activeTab === 'properties' &&
                                        (!operationDataMissing && currentOperationProperties?.length ? (
                                            <Properties
                                                customClassName="p-4"
                                                displayConditionsQuery={activeDisplayConditionsQuery}
                                                key={`${currentNode?.componentName}-${currentNode?.type}_${currentOperationName}_properties`}
                                                operationName={currentOperationName}
                                                properties={currentOperationProperties}
                                            />
                                        ) : (
                                            <PropertiesTabSkeleton />
                                        ))}

                                    {activeTab === 'output' && (
                                        <OutputTab
                                            clusterElementType={currentNode?.clusterElementType}
                                            connectionMissing={
                                                (currentComponentDefinition?.connectionRequired ?? false) &&
                                                !workflowTestConfigurationConnections?.length
                                            }
                                            currentNode={currentNode}
                                            currentOperationProperties={currentOperationProperties}
                                            key={`${currentNode?.componentName}-${currentNode?.type}_output`}
                                            outputDefined={outputDefined}
                                            outputFunctionDefined={outputFunctionDefined}
                                            parentWorkflowNodeName={rootClusterElementNodeData?.workflowNodeName}
                                            resumePerformFunctionDefined={
                                                (currentActionDefinition as ActionDefinition)
                                                    ?.resumePerformFunctionDefined
                                            }
                                            variablePropertiesDefined={
                                                currentTaskDispatcherDefinition?.variablePropertiesDefined
                                            }
                                            workflowId={workflow.id!}
                                        />
                                    )}
                                </div>
                            </ScrollArea>
                        </main>

                        <footer className="z-50 mt-auto flex items-center justify-between bg-background px-4 py-2">
                            <Select defaultValue={getNodeVersion(currentWorkflowNode)}>
                                <SelectTrigger className="w-auto border-none shadow-none">
                                    <SelectValue placeholder="Choose version..." />
                                </SelectTrigger>

                                <SelectContent>
                                    <SelectItem value="1">v1</SelectItem>
                                </SelectContent>
                            </Select>

                            {documentationUrl && (
                                <Link target="_blank" to={documentationUrl}>
                                    <Button size="sm" variant="ghost">
                                        Documentation <ExternalLinkIcon />
                                    </Button>
                                </Link>
                            )}
                        </footer>
                    </div>
                )}
            </div>
        </div>
    );
};

export default WorkflowNodeDetailsPanel;
