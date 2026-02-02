import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {ComponentConnection, WorkflowTestConfigurationConnection} from '@/shared/middleware/platform/configuration';
import {useSaveWorkflowTestConfigurationConnectionMutation} from '@/shared/mutations/platform/workflowTestConfigurations.mutations';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {WorkflowTestConfigurationKeys} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

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

    const saveWorkflowTestConfigurationConnectionMutation = useSaveWorkflowTestConfigurationConnectionMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: WorkflowTestConfigurationKeys.workflowTestConfigurations,
            });
        },
    });

    const handleValueChange = (connectionId: number, workflowConnectionKey: string) => {
        saveWorkflowTestConfigurationConnectionMutation.mutate({
            environmentId: currentEnvironmentId,
            saveWorkflowTestConfigurationConnectionRequest: {
                connectionId,
            },
            workflowConnectionKey,
            workflowId,
            workflowNodeName,
        });
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
