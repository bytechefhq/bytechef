import {Connection} from '@/shared/middleware/automation/configuration';
import {
    McpComponent,
    useCreateMcpComponentWithToolsMutation,
    useMcpToolsByComponentIdQuery,
    useUpdateMcpComponentWithToolsMutation,
} from '@/shared/middleware/graphql';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

import {SelectedToolI} from './useMcpComponentDialogToolSelectionStep';

export type StepType = 'components' | 'tools';

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
    const [currentStep, setCurrentStep] = useState<StepType>(mcpComponent ? 'tools' : 'components');
    const [selectedComponent, setSelectedComponent] = useState<ComponentDefinitionBasic | null>(
        mcpComponent
            ? ({
                  name: mcpComponent.componentName,
                  title: mcpComponent.title || mcpComponent.componentName,
                  version: mcpComponent.componentVersion,
              } as ComponentDefinitionBasic)
            : null
    );
    const [selectedTools, setSelectedTools] = useState<SelectedToolI[]>([]);
    const [selectedConnection, setSelectedConnection] = useState<Connection | null>(null);

    const {data: existingTools} = useMcpToolsByComponentIdQuery(
        {
            mcpComponentId: mcpComponent?.id?.toString() || '',
        },
        {
            enabled: !!mcpComponent?.id && open,
        }
    );

    const queryClient = useQueryClient();

    const invalidateMcpQueries = () => {
        queryClient.invalidateQueries({queryKey: ['mcpComponentsByServerId']});
        queryClient.invalidateQueries({queryKey: ['mcpComponents']});
        queryClient.invalidateQueries({queryKey: ['mcpServers']});
    };

    const createMcpComponentWithToolsMutation = useCreateMcpComponentWithToolsMutation({
        onSuccess: invalidateMcpQueries,
    });

    const updateMcpComponentWithToolsMutation = useUpdateMcpComponentWithToolsMutation({
        onSuccess: invalidateMcpQueries,
    });

    const handleComponentSelect = (component: ComponentDefinitionBasic) => {
        setSelectedComponent(component);
        setSelectedTools([]);
        setSelectedConnection(null);
        setCurrentStep('tools');
    };

    const handleClose = () => {
        if (onOpenChange) {
            onOpenChange(false);
        }

        setCurrentStep(mcpComponent ? 'tools' : 'components');
        setSelectedComponent(
            mcpComponent
                ? ({
                      name: mcpComponent.componentName,
                      title: mcpComponent.title || mcpComponent.componentName,
                      version: mcpComponent.componentVersion,
                  } as ComponentDefinitionBasic)
                : null
        );

        if (!mcpComponent) {
            setSelectedTools([]);
            setSelectedConnection(null);
        }
    };

    const handleSave = () => {
        if (!selectedComponent) {
            return;
        }

        const onMutationSuccess = () => {
            if (onOpenChange) {
                onOpenChange(false);
            }

            setCurrentStep(mcpComponent ? 'tools' : 'components');

            if (!mcpComponent) {
                setSelectedComponent(null);
            }

            setSelectedTools([]);
            setSelectedConnection(null);
        };

        if (mcpComponent?.id) {
            updateMcpComponentWithToolsMutation.mutate(
                {
                    id: mcpComponent.id.toString(),
                    input: {
                        componentName: selectedComponent.name,
                        componentVersion: selectedComponent.version,
                        connectionId: selectedConnection?.id?.toString() || undefined,
                        mcpServerId,
                        tools: selectedTools.map((tool) => ({
                            name: tool.name,
                            parameters: {},
                        })),
                        version: mcpComponent.version,
                    },
                },
                {onSuccess: onMutationSuccess}
            );
        } else {
            createMcpComponentWithToolsMutation.mutate(
                {
                    input: {
                        componentName: selectedComponent.name,
                        componentVersion: selectedComponent.version,
                        connectionId: selectedConnection?.id?.toString() || undefined,
                        mcpServerId,
                        tools: selectedTools.map((tool) => ({
                            name: tool.name,
                            parameters: {},
                        })),
                    },
                },
                {onSuccess: onMutationSuccess}
            );
        }
    };

    const handleBack = () => {
        if (mcpComponent) {
            handleClose();
        } else {
            setCurrentStep('components');
            setSelectedComponent(null);
            setSelectedTools([]);
            setSelectedConnection(null);
        }
    };

    const handleOpenChange = (newOpen: boolean) => {
        if (onOpenChange) {
            onOpenChange(newOpen);
        }

        if (!newOpen) {
            setCurrentStep(mcpComponent ? 'tools' : 'components');
            setSelectedComponent(
                mcpComponent
                    ? ({
                          name: mcpComponent.componentName,
                          title: mcpComponent.title || mcpComponent.componentName,
                          version: mcpComponent.componentVersion,
                      } as ComponentDefinitionBasic)
                    : null
            );

            if (!mcpComponent) {
                setSelectedTools([]);
                setSelectedConnection(null);
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
        selectedConnection,
        selectedTools,
        setSelectedConnection,
        setSelectedTools,
    };
};

export default useMcpComponentDialog;
