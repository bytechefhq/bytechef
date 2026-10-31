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

const isIntegrationLocked = (mcpComponent?: McpComponent, integrationUnavailable?: boolean) =>
    !!mcpComponent?.integrationId && !integrationUnavailable;

const getInitialStep = (mcpComponent?: McpComponent, integrationUnavailable?: boolean): StepType =>
    isIntegrationLocked(mcpComponent, integrationUnavailable) ? 'tools' : 'components';

const getInitialSelectedComponent = (
    mcpComponent?: McpComponent,
    integrationUnavailable?: boolean
): McpIntegrationComponentOptionI | null =>
    mcpComponent?.integrationId && isIntegrationLocked(mcpComponent, integrationUnavailable)
        ? {
              integrationId: mcpComponent.integrationId,
              name: mcpComponent.componentName,
              title: mcpComponent.title || mcpComponent.componentName,
              version: mcpComponent.componentVersion,
          }
        : null;

const useMcpComponentDialog = ({
    integrationUnavailable,
    mcpComponent,
    mcpServerId,
    onOpenChange,
    open,
}: {
    integrationUnavailable?: boolean;
    mcpComponent?: McpComponent;
    mcpServerId: string;
    open?: boolean;
    onOpenChange?: (open: boolean) => void;
}) => {
    const [currentStep, setCurrentStep] = useState<StepType>(getInitialStep(mcpComponent, integrationUnavailable));
    const [selectedComponent, setSelectedComponent] = useState<McpIntegrationComponentOptionI | null>(
        getInitialSelectedComponent(mcpComponent, integrationUnavailable)
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

        setCurrentStep(getInitialStep(mcpComponent, integrationUnavailable));
        setSelectedComponent(getInitialSelectedComponent(mcpComponent, integrationUnavailable));

        if (!mcpComponent) {
            setSelectedTools([]);
        }
    };

    const handleSaveSuccess = () => {
        if (onOpenChange) {
            onOpenChange(false);
        }

        setCurrentStep(getInitialStep(mcpComponent, integrationUnavailable));

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
                    integrationId: isIntegrationLocked(mcpComponent, integrationUnavailable)
                        ? undefined
                        : selectedComponent.integrationId,
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
        if (isIntegrationLocked(mcpComponent, integrationUnavailable)) {
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
            setCurrentStep(getInitialStep(mcpComponent, integrationUnavailable));
            setSelectedComponent(getInitialSelectedComponent(mcpComponent, integrationUnavailable));

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
