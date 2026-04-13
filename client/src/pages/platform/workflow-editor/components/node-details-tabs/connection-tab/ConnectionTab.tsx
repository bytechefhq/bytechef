import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ConnectionTabConnectionFieldset from '@/pages/platform/workflow-editor/components/node-details-tabs/connection-tab/ConnectionTabConnectionFieldset';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {getTask} from '@/pages/platform/workflow-editor/utils/getTask';
import saveTaskDispatcherSubtaskFieldChange from '@/pages/platform/workflow-editor/utils/saveTaskDispatcherSubtaskFieldChange';
import saveWorkflowDefinition from '@/pages/platform/workflow-editor/utils/saveWorkflowDefinition';
import {
    ComponentConnection,
    ComponentDefinition,
    WorkflowTestConfigurationConnection,
} from '@/shared/middleware/platform/configuration';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {TooltipPortal} from '@radix-ui/react-tooltip';
import {CircleQuestionMarkIcon, XIcon} from 'lucide-react';
import {ChangeEvent} from 'react';
import {twMerge} from 'tailwind-merge';
import {useDebouncedCallback} from 'use-debounce';
import {useShallow} from 'zustand/react/shallow';

import {useConnectionNoteStore} from '../../../stores/useConnectionNoteStore';

type ConnectionTabPropsType = {
    className?: string;
    componentConnections: Array<ComponentConnection>;
    currentComponentDefinition?: ComponentDefinition;
    updateWorkflowMutation?: UpdateWorkflowMutationType;
    workflowId: string;
    workflowNodeName: string;
    workflowTestConfigurationConnections?: Array<WorkflowTestConfigurationConnection>;
};

const ConnectionTab = ({
    className,
    componentConnections,
    currentComponentDefinition,
    updateWorkflowMutation,
    workflowId,
    workflowNodeName,
    workflowTestConfigurationConnections,
}: ConnectionTabPropsType) => {
    const {setShowConnectionNote, showConnectionNote} = useConnectionNoteStore(
        useShallow((state) => ({
            setShowConnectionNote: state.setShowConnectionNote,
            showConnectionNote: state.showConnectionNote,
        }))
    );

    const {currentComponent, currentNode, setCurrentComponent, setCurrentNode} = useWorkflowNodeDetailsPanelStore(
        useShallow((state) => ({
            currentComponent: state.currentComponent,
            currentNode: state.currentNode,
            setCurrentComponent: state.setCurrentComponent,
            setCurrentNode: state.setCurrentNode,
        }))
    );

    const {nodes, workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            nodes: state.nodes,
            workflow: state.workflow,
        }))
    );

    const workflowTask = currentNode?.workflowNodeName
        ? getTask({
              tasks: workflow.tasks || [],
              workflowNodeName: currentNode.workflowNodeName,
          })
        : undefined;

    const handleMaxRetriesChange = useDebouncedCallback((event: ChangeEvent<HTMLInputElement>) => {
        if (!currentNode || !updateWorkflowMutation) {
            return;
        }

        const rawValue = event.target.value;

        if (rawValue === '') {
            return;
        }

        const maxRetries = parseInt(rawValue, 10);

        if (isNaN(maxRetries) || maxRetries < 0) {
            return;
        }

        if (
            currentNode.conditionData ||
            currentNode.loopData ||
            currentNode.branchData ||
            currentNode.parallelData ||
            currentNode.eachData ||
            currentNode.forkJoinData ||
            currentNode.onErrorData
        ) {
            saveTaskDispatcherSubtaskFieldChange({
                currentComponentDefinition: currentComponentDefinition as ComponentDefinition,
                currentNodeIndex: nodes.findIndex((node) => node.data.name === currentNode.workflowNodeName),
                fieldUpdate: {
                    field: 'maxRetries',
                    value: maxRetries,
                },
                updateWorkflowMutation,
            });

            return;
        }

        saveWorkflowDefinition({
            decorative: true,
            nodeData: {
                ...currentNode,
                maxRetries,
                name: currentNode.workflowNodeName,
                version: currentComponentDefinition?.version ?? 1,
            },
            onSuccess: () => {
                setCurrentComponent({
                    ...currentComponent,
                    componentName: currentNode.componentName,
                    maxRetries,
                    workflowNodeName: currentNode.workflowNodeName,
                });

                setCurrentNode({
                    ...currentNode,
                    maxRetries,
                });
            },
            updateWorkflowMutation,
        });
    }, 600);

    return (
        <div className={twMerge('flex h-full flex-col gap-6 overflow-y-auto overflow-x-hidden p-4', className)}>
            {componentConnections.map((componentConnection) => {
                const workflowTestConfigurationConnection = workflowTestConfigurationConnections?.find(
                    (testConfigConnection) => testConfigConnection.workflowConnectionKey === componentConnection.key
                );

                return (
                    <ConnectionTabConnectionFieldset
                        componentConnection={componentConnection}
                        componentConnectionsCount={componentConnections.length}
                        currentComponentDefinition={currentComponentDefinition}
                        key={componentConnection.key}
                        workflowId={workflowId}
                        workflowNodeName={workflowNodeName}
                        workflowTestConfigurationConnection={workflowTestConfigurationConnection}
                    />
                );
            })}

            {showConnectionNote && (
                <div className="flex flex-col rounded-md bg-amber-100 p-4 text-gray-800">
                    <div className="flex items-center pb-2">
                        <span className="font-medium">Note</span>

                        <button
                            className="ml-auto p-0"
                            onClick={() => setShowConnectionNote(false)}
                            title="Close the note"
                        >
                            <XIcon aria-hidden="true" className="size-4 cursor-pointer" />
                        </button>
                    </div>

                    <p className="text-sm text-gray-800">
                        The selected connections are used for testing purposes only.
                    </p>
                </div>
            )}

            <fieldset className="space-y-1 border-0">
                <div className="flex items-center gap-1">
                    <Label>Max Retries</Label>

                    <Tooltip>
                        <TooltipTrigger>
                            <CircleQuestionMarkIcon className="ml-1 size-4 text-muted-foreground" />
                        </TooltipTrigger>

                        <TooltipPortal>
                            <TooltipContent className="max-w-md">
                                Number of retry attempts in case of connectivity errors
                            </TooltipContent>
                        </TooltipPortal>
                    </Tooltip>
                </div>

                <Input
                    className="bg-white"
                    defaultValue={workflowTask?.maxRetries ?? ''}
                    key={`${currentNode?.componentName}-${workflowTask?.type}_maxRetries`}
                    min={0}
                    name="maxRetries"
                    onChange={handleMaxRetriesChange}
                    placeholder="0"
                    step={1}
                    type="number"
                />
            </fieldset>
        </div>
    );
};

export default ConnectionTab;
