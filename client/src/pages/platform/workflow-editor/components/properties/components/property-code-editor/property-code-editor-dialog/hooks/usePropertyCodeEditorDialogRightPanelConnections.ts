import {connectionFormSchema} from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/PropertyCodeEditorDialogRightPanelConnectionsPopover';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {useConnectionNoteStore} from '@/pages/platform/workflow-editor/stores/useConnectionNoteStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import saveWorkflowDefinitionUpdate from '@/pages/platform/workflow-editor/utils/saveWorkflowDefinitionUpdate';
import {ComponentConnection, Workflow} from '@/shared/middleware/platform/configuration';
import {useGetWorkflowTestConfigurationConnectionsQuery} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {ClusterElementItemType, ClusterElementsType, WorkflowDefinitionType, WorkflowTaskType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import {z} from 'zod';
import {useShallow} from 'zustand/react/shallow';

interface UsePropertyCodeEditorDialogRightPanelConnectionsPropsI {
    componentConnections: ComponentConnection[];
    workflow: Workflow;
    workflowNodeName: string;
}

export const usePropertyCodeEditorDialogRightPanelConnections = ({
    componentConnections,
    workflow,
    workflowNodeName,
}: UsePropertyCodeEditorDialogRightPanelConnectionsPropsI) => {
    const [showNewConnectionDialog, setShowNewConnectionDialog] = useState(false);

    const queryClient = useQueryClient();

    const {setShowConnectionNote, showConnectionNote} = useConnectionNoteStore(
        useShallow((state) => ({
            setShowConnectionNote: state.setShowConnectionNote,
            showConnectionNote: state.showConnectionNote,
        }))
    );

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentNode = useWorkflowNodeDetailsPanelStore((state) => state.currentNode);
    const rootClusterElementNodeData = useWorkflowEditorStore(useShallow((state) => state.rootClusterElementNodeData));

    const isClusterElement = currentNode?.clusterElementType && rootClusterElementNodeData?.workflowNodeName;

    const {
        ConnectionKeys,
        updateWorkflowMutation,
        useCreateConnectionMutation,
        useGetComponentDefinitionsQuery,
        useGetConnectionTagsQuery,
    } = useWorkflowEditor();

    const handleCloseConnectionNote = () => setShowConnectionNote(false);

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({});

    const {data: workflowTestConfigurationConnections} = useGetWorkflowTestConfigurationConnectionsQuery({
        environmentId: currentEnvironmentId,
        workflowId: workflow.id!,
        workflowNodeName: isClusterElement
            ? (rootClusterElementNodeData?.workflowNodeName as string)
            : workflowNodeName,
    });

    const saveConnections = (
        updateDefinition: (workflowDefinition: WorkflowDefinitionType) => WorkflowDefinitionType | undefined
    ) => {
        saveWorkflowDefinitionUpdate({
            onSuccess: () => {
                if (isClusterElement) {
                    queryClient.invalidateQueries({queryKey: ['clusterElementComponentConnections']});
                } else {
                    queryClient.invalidateQueries({queryKey: ['workflowNodeComponentConnections']});
                }
            },
            updateDefinition,
            updateWorkflowMutation: updateWorkflowMutation!,
        });
    };

    const handleOnSubmit = (values: z.infer<typeof connectionFormSchema>) => {
        if (!workflow?.definition) {
            return;
        }

        saveConnections((freshWorkflowDefinition) => {
            let workflowDefinition = freshWorkflowDefinition;

            if (isClusterElement) {
                const parentTaskName = rootClusterElementNodeData?.workflowNodeName;
                const clusterElementType = currentNode?.clusterElementType as keyof ClusterElementsType;

                const parentTask = workflowDefinition.tasks?.find((task) => task.name === parentTaskName);

                const clusterElementValue = parentTask?.clusterElements?.[clusterElementType];

                if (!clusterElementValue) {
                    return undefined;
                }

                const addConnection = (clusterElement: ClusterElementItemType): ClusterElementItemType => ({
                    ...clusterElement,
                    connections: {
                        ...(clusterElement.connections ?? {}),
                        [values.name]: {
                            componentName: values.componentName,
                            componentVersion: values.componentVersion,
                        },
                    },
                });

                workflowDefinition = {
                    ...workflowDefinition,
                    tasks: workflowDefinition.tasks!.map((task) => {
                        if (task.name !== parentTaskName) {
                            return task;
                        }

                        const taskClusterElementValue = task.clusterElements![clusterElementType];

                        return {
                            ...task,
                            clusterElements: {
                                ...task.clusterElements,
                                [clusterElementType]: Array.isArray(taskClusterElementValue)
                                    ? taskClusterElementValue.map((clusterElement) =>
                                          clusterElement.name === workflowNodeName
                                              ? addConnection(clusterElement)
                                              : clusterElement
                                      )
                                    : addConnection(taskClusterElementValue as ClusterElementItemType),
                            },
                        } as WorkflowTaskType;
                    }),
                };
            } else {
                const scriptWorkflowTask = workflowDefinition.tasks?.find((task) => task.name === workflowNodeName);

                if (!scriptWorkflowTask) {
                    return undefined;
                }

                workflowDefinition = {
                    ...workflowDefinition,
                    tasks: workflowDefinition.tasks!.map((task) => {
                        if (task.name === workflowNodeName) {
                            return {
                                ...scriptWorkflowTask,
                                connections: {
                                    ...(scriptWorkflowTask.connections ?? {}),
                                    [values.name]: {
                                        componentName: values.componentName,
                                        componentVersion: values.componentVersion,
                                    },
                                },
                            } as WorkflowTaskType;
                        }

                        return task;
                    }),
                };
            }

            return workflowDefinition;
        });
    };

    const handleOnRemoveClick = (workflowConnectionKey: string) => {
        if (!workflow?.definition) {
            return;
        }

        saveConnections((freshWorkflowDefinition) => {
            let workflowDefinition = freshWorkflowDefinition;

            if (isClusterElement) {
                const parentTaskName = rootClusterElementNodeData?.workflowNodeName;
                const clusterElementType = currentNode?.clusterElementType as keyof ClusterElementsType;

                const parentTask = workflowDefinition.tasks?.find((task) => task.name === parentTaskName);

                const clusterElementValue = parentTask?.clusterElements?.[clusterElementType];

                if (!clusterElementValue) {
                    return undefined;
                }

                const removeConnection = (clusterElement: ClusterElementItemType): ClusterElementItemType => {
                    // eslint-disable-next-line @typescript-eslint/no-unused-vars
                    const {[workflowConnectionKey]: removed, ...remainingConnections} =
                        clusterElement.connections ?? {};

                    return {
                        ...clusterElement,
                        connections: remainingConnections,
                    };
                };

                workflowDefinition = {
                    ...workflowDefinition,
                    tasks: workflowDefinition.tasks!.map((task) => {
                        if (task.name !== parentTaskName) {
                            return task;
                        }

                        const taskClusterElementValue = task.clusterElements![clusterElementType];

                        return {
                            ...task,
                            clusterElements: {
                                ...task.clusterElements,
                                [clusterElementType]: Array.isArray(taskClusterElementValue)
                                    ? taskClusterElementValue.map((clusterElement) =>
                                          clusterElement.name === workflowNodeName
                                              ? removeConnection(clusterElement)
                                              : clusterElement
                                      )
                                    : removeConnection(taskClusterElementValue as ClusterElementItemType),
                            },
                        } as WorkflowTaskType;
                    }),
                };
            } else {
                const scriptWorkflowTask = workflowDefinition.tasks?.find((task) => task.name === workflowNodeName);

                if (!scriptWorkflowTask) {
                    return undefined;
                }

                // eslint-disable-next-line @typescript-eslint/no-unused-vars
                const {[workflowConnectionKey]: removed, ...remainingConnections} =
                    scriptWorkflowTask.connections ?? {};

                workflowDefinition = {
                    ...workflowDefinition,
                    tasks: workflowDefinition.tasks!.map((task) => {
                        if (task.name === workflowNodeName) {
                            return {
                                ...scriptWorkflowTask,
                                connections: remainingConnections,
                            } as WorkflowTaskType;
                        }

                        return task;
                    }),
                };
            }

            return workflowDefinition;
        });
    };

    return {
        ConnectionKeys,
        componentConnections,
        componentDefinitions,
        handleCloseConnectionNote,
        handleOnRemoveClick,
        handleOnSubmit,
        setShowNewConnectionDialog,
        showConnectionNote,
        showNewConnectionDialog,
        useCreateConnectionMutation,
        useGetConnectionTagsQuery,
        workflowTestConfigurationConnections,
    };
};
