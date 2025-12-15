import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {ScrollArea} from '@/components/ui/scroll-area';
import {Connection} from '@/shared/middleware/automation/configuration';
import {
    McpComponent,
    useCreateMcpComponentWithToolsMutation,
    useMcpToolsByComponentIdQuery,
    useUpdateMcpComponentWithToolsMutation,
} from '@/shared/middleware/graphql';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useState} from 'react';

import McpComponentDialogComponentSelectionStep from './McpComponentDialogComponentSelectionStep';
import McpComponentDialogToolSelectionStep from './McpComponentDialogToolSelectionStep';

type StepType = 'components' | 'tools';

interface SelectedToolI {
    name: string;
    componentName: string;
    componentVersion: number;
    title?: string;
    description?: string;
}

const McpComponentDialog = ({
    mcpComponent,
    mcpServerId,
    onOpenChange,
    open,
    triggerNode,
}: {
    mcpComponent?: McpComponent;
    mcpServerId: string;
    triggerNode?: ReactNode;
    open?: boolean;
    onOpenChange?: (open: boolean) => void;
}) => {
    const [currentStep, setCurrentStep] = useState<StepType>(mcpComponent ? 'tools' : 'components');
    const [selectedComponent, setSelectedComponent] = useState<ComponentDefinitionBasic | null>(
        mcpComponent
            ? ({
                  name: mcpComponent.componentName,
                  title: mcpComponent.componentName,
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

    const createMcpComponentWithToolsMutation = useCreateMcpComponentWithToolsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ['mcpComponentsByServerId'],
            });
        },
    });

    const updateMcpComponentWithToolsMutation = useUpdateMcpComponentWithToolsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ['mcpComponentsByServerId'],
            });
        },
    });

    const handleComponentSelect = (component: ComponentDefinitionBasic) => {
        setSelectedComponent(component);
        setSelectedTools([]);
        setSelectedConnection(null);
        setCurrentStep('tools');
    };

    const handleSave = () => {
        if (!selectedComponent) {
            return;
        }

        try {
            if (mcpComponent?.id) {
                updateMcpComponentWithToolsMutation.mutate({
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
                });
            } else {
                createMcpComponentWithToolsMutation.mutate({
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
                });
            }

            queryClient.invalidateQueries({
                queryKey: ['mcpComponents'],
            });
            queryClient.invalidateQueries({
                queryKey: ['mcpServers'],
            });
            queryClient.invalidateQueries({
                queryKey: ['mcpComponentsByServerId'],
            });

            if (onOpenChange) {
                onOpenChange(false);
            }

            setCurrentStep(mcpComponent ? 'tools' : 'components');

            if (!mcpComponent) {
                setSelectedComponent(null);
            }

            setSelectedTools([]);
            setSelectedConnection(null);
        } catch (error) {
            console.error('Error saving MCP component and tools:', error);
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
                          title: mcpComponent.componentName,
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

    const handleClose = () => {
        if (onOpenChange) {
            onOpenChange(false);
        }

        setCurrentStep(mcpComponent ? 'tools' : 'components');
        setSelectedComponent(
            mcpComponent
                ? ({
                      name: mcpComponent.componentName,
                      title: mcpComponent.componentName,
                      version: mcpComponent.componentVersion,
                  } as ComponentDefinitionBasic)
                : null
        );

        if (!mcpComponent) {
            setSelectedTools([]);
            setSelectedConnection(null);
        }
    };

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            <DialogContent className="max-h-workflow-execution-content-height sm:max-w-output-tab-sample-data-dialog-width">
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>
                            {currentStep === 'components'
                                ? 'Select Component'
                                : mcpComponent
                                  ? `Edit Tools for ${selectedComponent?.title || selectedComponent?.name}`
                                  : `Select Tools from ${selectedComponent?.title || selectedComponent?.name}`}
                        </DialogTitle>

                        <DialogDescription>
                            {currentStep === 'components'
                                ? 'Choose a component to add to your MCP server.'
                                : mcpComponent
                                  ? 'Modify the tools enabled for this component.'
                                  : 'Select the tools you want to enable for this component.'}
                        </DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <ScrollArea className="max-h-[60vh]">
                    {currentStep === 'components' && (
                        <McpComponentDialogComponentSelectionStep
                            onComponentSelect={handleComponentSelect}
                            open={open ?? true}
                        />
                    )}

                    {currentStep === 'tools' && (
                        <McpComponentDialogToolSelectionStep
                            existingTools={existingTools}
                            mcpComponent={mcpComponent}
                            onConnectionChange={setSelectedConnection}
                            onToolsChange={setSelectedTools}
                            open={open ?? true}
                            selectedComponent={selectedComponent}
                            selectedConnection={selectedConnection}
                            selectedTools={selectedTools}
                        />
                    )}
                </ScrollArea>

                <DialogFooter>
                    {currentStep === 'tools' && (
                        <div className="flex w-full justify-between">
                            <div className="text-sm text-muted-foreground">
                                {selectedTools.length} tool{selectedTools.length !== 1 ? 's' : ''} selected
                            </div>

                            <div className="flex space-x-2">
                                <DialogClose asChild>
                                    <Button label="Cancel" type="button" variant="outline" />
                                </DialogClose>

                                {currentStep === 'tools' && !mcpComponent && (
                                    <Button label="Back" onClick={handleBack} variant="outline" />
                                )}

                                <Button
                                    disabled={selectedTools.length === 0}
                                    label={mcpComponent ? 'Update' : 'Save'}
                                    onClick={handleSave}
                                />
                            </div>
                        </div>
                    )}
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default McpComponentDialog;
