import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {
    useSaveClusterElementTestConfigurationConnectionMutation,
    useSaveWorkflowTestConfigurationConnectionMutation,
} from '@/shared/middleware/graphql';
import {ComponentConnection, WorkflowTestConfigurationConnection} from '@/shared/middleware/platform/configuration';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {WorkflowTestConfigurationKeys} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

interface UsePropertyCodeEditorDialogRightPanelConnectionsSelectProps {
    componentConnection: ComponentConnection;
    workflowId: string;
    workflowNodeName: string;
    workflowTestConfigurationConnection?: WorkflowTestConfigurationConnection;
}

const usePropertyCodeEditorDialogRightPanelConnectionsSelect = ({
    componentConnection,
    workflowId,
    workflowNodeName,
    workflowTestConfigurationConnection,
}: UsePropertyCodeEditorDialogRightPanelConnectionsSelectProps) => {
    const [showNewConnectionDialog, setShowNewConnectionDialog] = useState(false);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentNode = useWorkflowNodeDetailsPanelStore((state) => state.currentNode);
    const rootClusterElementNodeData = useWorkflowEditorStore(useShallow((state) => state.rootClusterElementNodeData));

    const isClusterElement = currentNode?.clusterElementType && rootClusterElementNodeData?.workflowNodeName;

    const {
        ConnectionKeys,
        useCreateConnectionMutation,
        useGetComponentDefinitionsQuery,
        useGetConnectionTagsQuery,
        useGetConnectionsQuery,
    } = useWorkflowEditor();

    let connectionId: number | undefined;

    if (workflowTestConfigurationConnection) {
        connectionId = workflowTestConfigurationConnection.connectionId;
    }

    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName: componentConnection.componentName,
        componentVersion: componentConnection.componentVersion,
    });

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({
        connectionDefinitions: true,
    });

    /* eslint-disable @typescript-eslint/no-non-null-asserted-optional-chain */
    const {data: connections} = useGetConnectionsQuery!(
        {
            componentName: componentDefinition?.name!,
            connectionVersion: componentDefinition?.connection?.version,
        },
        !!componentDefinition
    );

    const queryClient = useQueryClient();

    const saveClusterElementConnectionMutation = useSaveClusterElementTestConfigurationConnectionMutation();
    const saveWorkflowNodeConnectionMutation = useSaveWorkflowTestConfigurationConnectionMutation();

    const handleValueChange = (connectionId: number, workflowConnectionKey: string) => {
        if (isClusterElement) {
            saveClusterElementConnectionMutation.mutate(
                {
                    clusterElementType: currentNode.clusterElementType!,
                    clusterElementWorkflowNodeName: currentNode.name,
                    connectionId,
                    environmentId: currentEnvironmentId!,
                    workflowConnectionKey,
                    workflowId,
                    workflowNodeName: rootClusterElementNodeData.workflowNodeName,
                },
                {
                    onSuccess: () => {
                        queryClient.invalidateQueries({
                            queryKey: WorkflowTestConfigurationKeys.workflowTestConfigurations,
                        });
                    },
                }
            );
        } else {
            saveWorkflowNodeConnectionMutation.mutate(
                {
                    connectionId,
                    environmentId: currentEnvironmentId!,
                    workflowConnectionKey,
                    workflowId,
                    workflowNodeName,
                },
                {
                    onSuccess: () => {
                        queryClient.invalidateQueries({
                            queryKey: WorkflowTestConfigurationKeys.workflowTestConfigurations,
                        });
                    },
                }
            );
        }
    };

    return {
        ConnectionKeys,
        componentDefinition,
        componentDefinitions,
        connectionId,
        connections,
        handleValueChange,
        setShowNewConnectionDialog,
        showNewConnectionDialog,
        useCreateConnectionMutation,
        useGetConnectionTagsQuery,
    };
};

export default usePropertyCodeEditorDialogRightPanelConnectionsSelect;
