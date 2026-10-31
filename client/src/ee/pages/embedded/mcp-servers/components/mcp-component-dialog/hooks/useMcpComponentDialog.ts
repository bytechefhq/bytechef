import {
    ComponentDefinition,
    McpComponent,
    useCreateEmbeddedMcpComponentMutation,
    useEmbeddedMcpToolsByComponentIdQuery,
    useUpdateEmbeddedMcpComponentMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

import {SelectedToolI} from './useMcpComponentDialogToolSelectionStep';

export type StepType = 'components' | 'tools';

export interface McpIntegrationComponentOptionI extends ComponentDefinition {
    id?: string;
    integrationId?: string;
    version: number | null;
}

const getInitialStep = (mcpComponent?: McpComponent): StepType =>
    mcpComponent?.integrationId ? 'tools' : 'components';

const getInitialSelectedComponent = (mcpComponent?: McpComponent): McpIntegrationComponentOptionI | null =>
    mcpComponent?.integrationId
        ? {
              integrationId: mcpComponent.integrationId,
              name: mcpComponent.componentName,
              title: mcpComponent.title || mcpComponent.componentName,
              version: mcpComponent.componentVersion,
          }
        : null;

const useMcpComponentDialog = ({
    mcpComponent,
    mcpServerId,
    onOpenChange,
    open,
}: {
    mcpComponent?: McpComponent;
    mcpServerId: string;
    open?: boolean;
    onOpenChange?: (open: boolean) => void;
}) => {
    const [currentStep, setCurrentStep] = useState<StepType>(getInitialStep(mcpComponent));
    const [selectedComponent, setSelectedComponent] = useState<McpIntegrationComponentOptionI | null>(
        getInitialSelectedComponent(mcpComponent)
    );
    const [selectedTools, setSelectedTools] = useState<SelectedToolI[]>([]);

    const {data: existingTools} = useEmbeddedMcpToolsByComponentIdQuery(
        {
            mcpComponentId: mcpComponent?.id?.toString() || '',
        },
        {
            enabled: !!mcpComponent?.id && open,
        }
    );

    const queryClient = useQueryClient();

    const invalidateMcpQueries = () => {
        queryClient.invalidateQueries({queryKey: ['embeddedMcpComponentsByServerId']});
        queryClient.invalidateQueries({queryKey: ['embeddedMcpServers']});
    };

    const createEmbeddedMcpComponentMutation = useCreateEmbeddedMcpComponentMutation({
        onSuccess: invalidateMcpQueries,
    });

    const updateEmbeddedMcpComponentMutation = useUpdateEmbeddedMcpComponentMutation({
        onSuccess: invalidateMcpQueries,
    });

    const handleComponentSelect = (component: McpIntegrationComponentOptionI) => {
        setSelectedComponent(component);
        setSelectedTools([]);
        setCurrentStep('tools');
    };

    const handleClose = () => {
        if (onOpenChange) {
            onOpenChange(false);
        }

        setCurrentStep(getInitialStep(mcpComponent));
        setSelectedComponent(getInitialSelectedComponent(mcpComponent));

        if (!mcpComponent) {
            setSelectedTools([]);
        }
    };

    const handleSaveSuccess = () => {
        if (onOpenChange) {
            onOpenChange(false);
        }

        setCurrentStep(getInitialStep(mcpComponent));

        if (!mcpComponent) {
            setSelectedComponent(null);
        }

        setSelectedTools([]);
    };

    const handleSave = () => {
        if (!selectedComponent?.integrationId) {
            return;
        }

        if (mcpComponent?.id) {
            updateEmbeddedMcpComponentMutation.mutate(
                {
                    id: mcpComponent.id.toString(),
                    input: {
                        componentName: selectedComponent.name,
                        componentVersion: selectedComponent.version ?? 1,
                        mcpServerId,
                        tools: selectedTools.map((tool) => ({
                            name: tool.name,
                            parameters: {},
                        })),
                        version: mcpComponent.version,
                    },
                    integrationId: mcpComponent.integrationId ? undefined : selectedComponent.integrationId,
                },
                {onSuccess: handleSaveSuccess}
            );
        } else {
            createEmbeddedMcpComponentMutation.mutate(
                {
                    input: {
                        componentName: selectedComponent.name,
                        componentVersion: selectedComponent.version ?? 1,
                        mcpServerId,
                        tools: selectedTools.map((tool) => ({
                            name: tool.name,
                            parameters: {},
                        })),
                    },
                    integrationId: selectedComponent.integrationId,
                },
                {onSuccess: handleSaveSuccess}
            );
        }
    };

    const handleBack = () => {
        if (mcpComponent?.integrationId) {
            handleClose();
        } else {
            setCurrentStep('components');
            setSelectedComponent(null);
            setSelectedTools([]);
        }
    };

    const handleOpenChange = (newOpen: boolean) => {
        if (onOpenChange) {
            onOpenChange(newOpen);
        }

        if (!newOpen) {
            setCurrentStep(getInitialStep(mcpComponent));
            setSelectedComponent(getInitialSelectedComponent(mcpComponent));

            if (!mcpComponent) {
                setSelectedTools([]);
            }
        }
    };

    return {
        currentStep,
        existingTools,
        handleBack,
        handleClose,
        handleComponentSelect,
        handleOpenChange,
        handleSave,
        selectedComponent,
        selectedTools,
        setSelectedTools,
    };
};

export default useMcpComponentDialog;
