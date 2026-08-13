import {Connection} from '@/shared/middleware/automation/configuration';
import {
    McpComponent,
    useAuthoritiesQuery,
    useCreateMcpComponentWithToolsMutation,
    useMcpToolsByComponentIdQuery,
    useUpdateMcpComponentWithToolsMutation,
} from '@/shared/middleware/graphql';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {getRoleLabel} from '@/shared/util/role-utils';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo, useState} from 'react';

import {SelectedToolType} from './useMcpComponentDialogToolSelectionStep';

export type StepType = 'components' | 'tools';

interface UseMcpComponentDialogProps {
    mcpComponent?: McpComponent;
    mcpServerId: string;
    open?: boolean;
    onOpenChange?: (open: boolean) => void;
}

const useMcpComponentDialog = ({mcpComponent, mcpServerId, onOpenChange, open}: UseMcpComponentDialogProps) => {
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
    const [selectedTools, setSelectedTools] = useState<SelectedToolType[]>([]);
    const [selectedConnection, setSelectedConnection] = useState<Connection | null>(null);
    const [requiredAuthorities, setRequiredAuthorities] = useState<string[]>(mcpComponent?.requiredAuthorities ?? []);

    // The authority list is the same one the user-management dialogs offer, so an MCP component can only require an
    // authority that actually exists. The query is ADMIN-only server-side; for a non-admin it simply yields no options
    // rather than failing the dialog, and any authority already saved on the component stays selected regardless.
    const {data: authoritiesData, isLoading: authoritiesLoading} = useAuthoritiesQuery({});

    const authorityOptions = useMemo(() => {
        const authorities = new Set([...(authoritiesData?.authorities ?? []), ...requiredAuthorities]);

        return Array.from(authorities)
            .map((authority) => ({label: getRoleLabel(authority), value: authority}))
            .sort((option, otherOption) => option.label.localeCompare(otherOption.label));
    }, [authoritiesData, requiredAuthorities]);

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
        queryClient.invalidateQueries({queryKey: ['workspaceMcpServers']});
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
        setRequiredAuthorities(mcpComponent?.requiredAuthorities ?? []);

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
            setRequiredAuthorities(mcpComponent?.requiredAuthorities ?? []);
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
                        requiredAuthorities,
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
                        requiredAuthorities,
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

            return;
        }

        setCurrentStep('components');
        setSelectedComponent(null);
        setSelectedTools([]);
        setSelectedConnection(null);
    };

    const handleOpenChange = (newOpen: boolean) => {
        if (onOpenChange) {
            onOpenChange(newOpen);
        }

        if (newOpen) {
            return;
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
        setRequiredAuthorities(mcpComponent?.requiredAuthorities ?? []);

        if (!mcpComponent) {
            setSelectedTools([]);
            setSelectedConnection(null);
        }
    };

    return {
        authoritiesLoading,
        authorityOptions,
        currentStep,
        existingTools,
        handleBack,
        handleClose,
        handleComponentSelect,
        handleOpenChange,
        handleSave,
        requiredAuthorities,
        selectedComponent,
        selectedConnection,
        selectedTools,
        setRequiredAuthorities,
        setSelectedConnection,
        setSelectedTools,
    };
};

export default useMcpComponentDialog;
