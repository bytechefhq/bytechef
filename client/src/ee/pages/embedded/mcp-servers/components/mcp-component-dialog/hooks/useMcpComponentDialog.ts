import {
    ComponentDefinition,
    McpComponent,
    useCreateMcpComponentWithToolsMutation,
    useMcpToolsByComponentIdQuery,
    useUpdateMcpComponentWithToolsMutation,
} from '@/shared/middleware/graphql';
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
    const [selectedComponent, setSelectedComponent] = useState<ComponentDefinition | null>(
        mcpComponent
            ? ({
                  name: mcpComponent.componentName,
                  title: mcpComponent.title || mcpComponent.componentName,
                  version: mcpComponent.componentVersion,
              } as ComponentDefinition)
            : null
    );
    const [selectedTools, setSelectedTools] = useState<SelectedToolI[]>([]);
    const [requiredAuthorities, setRequiredAuthorities] = useState<string[]>(mcpComponent?.requiredAuthorities ?? []);
    const [requiredAuthorityInput, setRequiredAuthorityInput] = useState('');

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
        queryClient.invalidateQueries({queryKey: ['embeddedMcpServers']});
    };

    const createMcpComponentWithToolsMutation = useCreateMcpComponentWithToolsMutation({
        onSuccess: invalidateMcpQueries,
    });

    const updateMcpComponentWithToolsMutation = useUpdateMcpComponentWithToolsMutation({
        onSuccess: invalidateMcpQueries,
    });

    const handleComponentSelect = (component: ComponentDefinition) => {
        setSelectedComponent(component);
        setSelectedTools([]);
        setCurrentStep('tools');
    };

    const handleAddRequiredAuthority = () => {
        const trimmed = requiredAuthorityInput.trim();

        if (trimmed && !requiredAuthorities.includes(trimmed)) {
            setRequiredAuthorities([...requiredAuthorities, trimmed]);
            setRequiredAuthorityInput('');
        }
    };

    const handleRemoveRequiredAuthority = (requiredAuthority: string) => {
        setRequiredAuthorities(
            requiredAuthorities.filter((existingAuthority) => existingAuthority !== requiredAuthority)
        );
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
                  } as ComponentDefinition)
                : null
        );
        setRequiredAuthorities(mcpComponent?.requiredAuthorities ?? []);
        setRequiredAuthorityInput('');

        if (!mcpComponent) {
            setSelectedTools([]);
        }
    };

    const handleSaveSuccess = () => {
        if (onOpenChange) {
            onOpenChange(false);
        }

        setCurrentStep(mcpComponent ? 'tools' : 'components');

        if (!mcpComponent) {
            setSelectedComponent(null);
        }

        setSelectedTools([]);
        setRequiredAuthorities(mcpComponent?.requiredAuthorities ?? []);
        setRequiredAuthorityInput('');
    };

    const handleSave = () => {
        if (!selectedComponent) {
            return;
        }

        if (mcpComponent?.id) {
            updateMcpComponentWithToolsMutation.mutate(
                {
                    id: mcpComponent.id.toString(),
                    input: {
                        componentName: selectedComponent.name,
                        componentVersion: selectedComponent.version ?? 1,
                        mcpServerId,
                        requiredAuthorities,
                        tools: selectedTools.map((tool) => ({
                            name: tool.name,
                            parameters: {},
                        })),
                        version: mcpComponent.version,
                    },
                },
                {onSuccess: handleSaveSuccess}
            );
        } else {
            createMcpComponentWithToolsMutation.mutate(
                {
                    input: {
                        componentName: selectedComponent.name,
                        componentVersion: selectedComponent.version ?? 1,
                        mcpServerId,
                        requiredAuthorities,
                        tools: selectedTools.map((tool) => ({
                            name: tool.name,
                            parameters: {},
                        })),
                    },
                },
                {onSuccess: handleSaveSuccess}
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
                      } as ComponentDefinition)
                    : null
            );
            setRequiredAuthorities(mcpComponent?.requiredAuthorities ?? []);
            setRequiredAuthorityInput('');

            if (!mcpComponent) {
                setSelectedTools([]);
            }
        }
    };

    return {
        currentStep,
        existingTools,
        handleAddRequiredAuthority,
        handleBack,
        handleClose,
        handleComponentSelect,
        handleOpenChange,
        handleRemoveRequiredAuthority,
        handleSave,
        requiredAuthorities,
        requiredAuthorityInput,
        selectedComponent,
        selectedTools,
        setRequiredAuthorityInput,
        setSelectedTools,
    };
};

export default useMcpComponentDialog;
