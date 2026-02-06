import {connectionFormSchema} from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/PropertyCodeEditorDialogRightPanelConnectionsPopover';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {useConnectionNoteStore} from '@/pages/platform/workflow-editor/stores/useConnectionNoteStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {ComponentConnection, Workflow} from '@/shared/middleware/platform/configuration';
import {useGetWorkflowTestConfigurationConnectionsQuery} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {ClusterElementItemType, ClusterElementsType, WorkflowDefinitionType, WorkflowTaskType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import {z} from 'zod';
import {useShallow} from 'zustand/react/shallow';

const SPACE = 4;

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
        workflowNodeName,
    });

    const handleOnSubmit = (values: z.infer<typeof connectionFormSchema>) => {
        if (!workflow?.definition) {
            return;
        }

        let workflowDefinition: WorkflowDefinitionType = JSON.parse(workflow?.definition);

        if (isClusterElement) {
            const parentTaskName = rootClusterElementNodeData?.workflowNodeName;
            const clusterElementType = currentNode?.clusterElementType as keyof ClusterElementsType;

            const parentTask = workflowDefinition.tasks?.find((task) => task.name === parentTaskName);

            const clusterElementValue = parentTask?.clusterElements?.[clusterElementType];

            if (!clusterElementValue || Array.isArray(clusterElementValue)) {
                return;
            }

            workflowDefinition = {
                ...workflowDefinition,
                tasks: workflowDefinition.tasks!.map((task) => {
                    if (task.name === parentTaskName) {
                        const clusterElement = task.clusterElements![clusterElementType] as ClusterElementItemType;

                        return {
                            ...task,
                            clusterElements: {
                                ...task.clusterElements,
                                [clusterElementType]: {
                                    ...clusterElement,
                                    connections: {
                                        ...(clusterElement.connections ?? {}),
                                        [values.name]: {
                                            componentName: values.componentName,
                                            componentVersion: values.componentVersion,
                                        },
                                    },
                                },
                            },
                        } as WorkflowTaskType;
                    }

                    return task;
                }),
            };
        } else {
            const scriptWorkflowTask = workflowDefinition.tasks?.find((task) => task.name === workflowNodeName);

            if (!scriptWorkflowTask) {
                return;
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

        updateWorkflowMutation!.mutate(
            {
                id: workflow.id!,
                workflow: {
                    definition: JSON.stringify(workflowDefinition, null, SPACE),
                    version: workflow.version,
                },
            },
            {
                onSuccess: () => {
                    if (isClusterElement) {
                        queryClient.invalidateQueries({queryKey: ['clusterElementComponentConnections']});
                    } else {
                        queryClient.invalidateQueries({queryKey: ['workflowNodeComponentConnections']});
                    }
                },
            }
        );
    };

    const handleOnRemoveClick = (workflowConnectionKey: string) => {
        if (!workflow?.definition) {
            return;
        }

        let workflowDefinition: WorkflowDefinitionType = JSON.parse(workflow?.definition);

        if (isClusterElement) {
            const parentTaskName = rootClusterElementNodeData?.workflowNodeName;
            const clusterElementType = currentNode?.clusterElementType as keyof ClusterElementsType;

            const parentTask = workflowDefinition.tasks?.find((task) => task.name === parentTaskName);

            const clusterElementValue = parentTask?.clusterElements?.[clusterElementType];

            if (!clusterElementValue || Array.isArray(clusterElementValue)) {
                return;
            }

            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            const {[workflowConnectionKey]: removed, ...remainingConnections} = clusterElementValue.connections ?? {};

            workflowDefinition = {
                ...workflowDefinition,
                tasks: workflowDefinition.tasks!.map((task) => {
                    if (task.name === parentTaskName) {
                        const clusterElement = task.clusterElements![clusterElementType] as ClusterElementItemType;

                        return {
                            ...task,
                            clusterElements: {
                                ...task.clusterElements,
                                [clusterElementType]: {
                                    ...clusterElement,
                                    connections: remainingConnections,
                                },
                            },
                        } as WorkflowTaskType;
                    }

                    return task;
                }),
            };
        } else {
            const scriptWorkflowTask = workflowDefinition.tasks?.find((task) => task.name === workflowNodeName);

            if (!scriptWorkflowTask) {
                return;
            }

            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            const {[workflowConnectionKey]: removed, ...remainingConnections} = scriptWorkflowTask.connections ?? {};

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

        updateWorkflowMutation!.mutate(
            {
                id: workflow.id!,
                workflow: {
                    definition: JSON.stringify(workflowDefinition, null, SPACE),
                    version: workflow.version,
                },
            },
            {
                onSuccess: () => {
                    if (isClusterElement) {
                        queryClient.invalidateQueries({queryKey: ['clusterElementComponentConnections']});
                    } else {
                        queryClient.invalidateQueries({queryKey: ['workflowNodeComponentConnections']});
                    }
                },
            }
        );
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
