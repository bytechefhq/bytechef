import Button from '@/components/Button/Button';
import {Checkbox} from '@/components/ui/checkbox';
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
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {useGetIntegrationInstanceConfigurationsQuery} from '@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {
    McpIntegrationInstanceConfiguration,
    McpServer,
    useCreateMcpIntegrationInstanceConfigurationMutation,
    useToolEligibleIntegrationInstanceConfigurationWorkflowsQuery,
    useUpdateMcpIntegrationInstanceConfigurationMutation,
} from '@/shared/middleware/graphql';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {ComponentIcon} from 'lucide-react';
import {ReactNode, useEffect, useMemo, useState} from 'react';
import {useForm} from 'react-hook-form';
import InlineSVG from 'react-inlinesvg';
import {z} from 'zod';

const formSchema = z.object({
    integrationInstanceConfigurationId: z.string().min(1, 'Please select an integration instance configuration'),
    mcpServerId: z.string().min(1),
    selectedWorkflowIds: z.array(z.string()).min(1, 'Please select at least one workflow'),
});

interface McpIntegrationInstanceConfigurationDialogProps {
    mcpIntegrationInstanceConfiguration?: McpIntegrationInstanceConfiguration;
    mcpServer?: McpServer;
    onClose?: () => void;
    triggerNode?: ReactNode;
}

const McpIntegrationInstanceConfigurationWorkflowDialog = ({
    mcpIntegrationInstanceConfiguration,
    mcpServer,
    onClose,
    triggerNode,
}: McpIntegrationInstanceConfigurationDialogProps) => {
    const isEditMode = !!mcpIntegrationInstanceConfiguration?.id;

    const [isOpen, setIsOpen] = useState(!triggerNode);
    const [currentIntegrationInstanceConfigurationId, setCurrentIntegrationInstanceConfigurationId] = useState<
        string | undefined
    >(isEditMode ? mcpIntegrationInstanceConfiguration.integrationInstanceConfigurationId : undefined);

    const {data: integrationInstanceConfigurations = []} = useGetIntegrationInstanceConfigurationsQuery({});

    const {data: eligibleWorkflowsData} = useToolEligibleIntegrationInstanceConfigurationWorkflowsQuery(
        {
            integrationInstanceConfigurationId: currentIntegrationInstanceConfigurationId || '0',
        },
        {enabled: !!currentIntegrationInstanceConfigurationId}
    );

    const eligibleWorkflows = eligibleWorkflowsData?.toolEligibleIntegrationInstanceConfigurationWorkflows;

    const existingWorkflowLabels = useMemo(() => {
        const mcpIntegrationInstanceConfigurationWorkflows =
            mcpIntegrationInstanceConfiguration?.mcpIntegrationInstanceConfigurationWorkflows ?? [];

        return new Set(
            mcpIntegrationInstanceConfigurationWorkflows
                .filter((workflow) => workflow?.workflow?.label != null)
                .map((workflow) => workflow!.workflow!.label!)
        );
    }, [mcpIntegrationInstanceConfiguration?.mcpIntegrationInstanceConfigurationWorkflows]);

    const initialSelectedWorkflowIds = useMemo(() => {
        if (!isEditMode || !eligibleWorkflows) {
            return [];
        }

        return eligibleWorkflows
            .filter((eligibleWorkflow) => existingWorkflowLabels.has(eligibleWorkflow.label))
            .map((eligibleWorkflow) => eligibleWorkflow.id);
    }, [eligibleWorkflows, existingWorkflowLabels, isEditMode]);

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            integrationInstanceConfigurationId: isEditMode
                ? mcpIntegrationInstanceConfiguration.integrationInstanceConfigurationId
                : undefined,
            mcpServerId: mcpIntegrationInstanceConfiguration?.mcpServerId || mcpServer?.id || '',
            selectedWorkflowIds: [],
        },
        resolver: zodResolver(formSchema),
    });

    const {control, getValues, handleSubmit, reset, setValue} = form;

    const selectedWorkflowIds = form.watch('selectedWorkflowIds');

    const queryClient = useQueryClient();

    const invalidateAndClose = () => {
        queryClient.invalidateQueries({queryKey: ['mcpIntegrationInstanceConfigurationsByServerId']});
        queryClient.invalidateQueries({queryKey: ['mcpIntegrationInstanceConfigurations']});
        queryClient.invalidateQueries({queryKey: ['embeddedMcpServers']});

        closeDialog();
    };

    const createMcpIntegrationInstanceConfigurationMutation = useCreateMcpIntegrationInstanceConfigurationMutation({
        onSuccess: invalidateAndClose,
    });

    const updateMcpIntegrationInstanceConfigurationMutation = useUpdateMcpIntegrationInstanceConfigurationMutation({
        onSuccess: invalidateAndClose,
    });

    const closeDialog = () => {
        setIsOpen(false);

        if (onClose) {
            onClose();
        }

        reset();
    };

    const saveMcpIntegrationInstanceConfiguration = () => {
        const formValues = getValues();

        if (isEditMode) {
            updateMcpIntegrationInstanceConfigurationMutation.mutate({
                id: mcpIntegrationInstanceConfiguration.id,
                input: {
                    selectedWorkflowIds: formValues.selectedWorkflowIds,
                },
            });
        } else {
            createMcpIntegrationInstanceConfigurationMutation.mutate({
                input: {
                    integrationInstanceConfigurationId: formValues.integrationInstanceConfigurationId,
                    mcpServerId: formValues.mcpServerId,
                    selectedWorkflowIds: formValues.selectedWorkflowIds,
                },
            });
        }
    };

    // Pre-populate selected workflow IDs once eligible workflows load in edit mode
    useEffect(() => {
        if (isEditMode && initialSelectedWorkflowIds.length > 0 && selectedWorkflowIds.length === 0) {
            setValue('selectedWorkflowIds', initialSelectedWorkflowIds);
        }
    }, [initialSelectedWorkflowIds, isEditMode, selectedWorkflowIds.length, setValue]);

    return (
        <Dialog
            onOpenChange={(isOpen) => {
                if (isOpen) {
                    setIsOpen(isOpen);
                } else {
                    closeDialog();
                }
            }}
            open={isOpen}
        >
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            <DialogContent>
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>{`${isEditMode ? 'Edit' : 'Select'}`} Workflows</DialogTitle>

                        <DialogDescription>
                            {isEditMode
                                ? 'Edit the MCP server workflow configuration.'
                                : 'Select workflows to add to the MCP server.'}
                        </DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <Form {...form}>
                    <form
                        className="flex flex-col gap-4"
                        onSubmit={handleSubmit(saveMcpIntegrationInstanceConfiguration)}
                    >
                        {isEditMode && (
                            <>
                                <FormItem>
                                    <FormLabel>Integration</FormLabel>

                                    <Input
                                        disabled
                                        value={mcpIntegrationInstanceConfiguration.integration?.name || ''}
                                    />
                                </FormItem>

                                <FormItem>
                                    <FormLabel>Integration Version</FormLabel>

                                    <Input
                                        disabled
                                        value={`v${mcpIntegrationInstanceConfiguration.integrationVersion}`}
                                    />
                                </FormItem>
                            </>
                        )}

                        {!isEditMode && (
                            <>
                                <FormField
                                    control={control}
                                    name="mcpServerId"
                                    render={({field}) => (
                                        <FormItem>
                                            <FormLabel>MCP Server</FormLabel>

                                            <FormControl>
                                                <Input
                                                    {...field}
                                                    disabled={!!mcpServer}
                                                    placeholder={mcpServer ? mcpServer.name : 'Select MCP Server'}
                                                    value={mcpServer ? mcpServer.name : field.value}
                                                />
                                            </FormControl>

                                            <FormMessage />
                                        </FormItem>
                                    )}
                                />

                                <FormField
                                    control={control}
                                    name="integrationInstanceConfigurationId"
                                    render={({field}) => (
                                        <FormItem>
                                            <FormLabel>Integration Instance Configuration</FormLabel>

                                            <FormControl>
                                                <Select
                                                    onValueChange={(value) => {
                                                        field.onChange(value);

                                                        setCurrentIntegrationInstanceConfigurationId(value);

                                                        setValue('selectedWorkflowIds', []);
                                                    }}
                                                    value={field.value}
                                                >
                                                    <SelectTrigger>
                                                        <SelectValue placeholder="Select a configuration..." />
                                                    </SelectTrigger>

                                                    <SelectContent>
                                                        {integrationInstanceConfigurations.map((configuration) => (
                                                            <SelectItem
                                                                key={configuration.id}
                                                                value={String(configuration.id)}
                                                            >
                                                                <div className="flex items-center gap-x-2">
                                                                    {configuration.integration?.icon ? (
                                                                        <InlineSVG
                                                                            className="size-4 flex-none"
                                                                            src={configuration.integration.icon}
                                                                        />
                                                                    ) : (
                                                                        <ComponentIcon className="size-4 flex-none text-gray-500" />
                                                                    )}

                                                                    <span>
                                                                        {`${configuration.name} (v${configuration.integrationVersion})`}
                                                                    </span>
                                                                </div>
                                                            </SelectItem>
                                                        ))}
                                                    </SelectContent>
                                                </Select>
                                            </FormControl>

                                            <FormMessage />
                                        </FormItem>
                                    )}
                                    shouldUnregister={false}
                                />
                            </>
                        )}

                        {eligibleWorkflows && eligibleWorkflows.length > 0 && (
                            <FormField
                                control={control}
                                name="selectedWorkflowIds"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Select Workflows</FormLabel>

                                        <div className="space-y-2">
                                            {eligibleWorkflows.map((integrationWorkflow) => (
                                                <div
                                                    className="flex items-center space-x-2"
                                                    key={integrationWorkflow.id}
                                                >
                                                    <Checkbox
                                                        checked={field.value?.includes(integrationWorkflow.id || '')}
                                                        onCheckedChange={(checked) => {
                                                            const currentValues = field.value || [];

                                                            if (checked) {
                                                                field.onChange([
                                                                    ...currentValues,
                                                                    integrationWorkflow.id,
                                                                ]);
                                                            } else {
                                                                field.onChange(
                                                                    currentValues.filter(
                                                                        (workflowId) =>
                                                                            workflowId !== integrationWorkflow.id
                                                                    )
                                                                );
                                                            }
                                                        }}
                                                    />

                                                    <label className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">
                                                        {integrationWorkflow.label || integrationWorkflow.id}
                                                    </label>
                                                </div>
                                            ))}
                                        </div>

                                        <FormMessage />
                                    </FormItem>
                                )}
                                shouldUnregister={false}
                            />
                        )}

                        <DialogFooter>
                            <DialogClose asChild>
                                <Button label="Cancel" type="button" variant="outline" />
                            </DialogClose>

                            <Button label={isEditMode ? 'Update' : 'Add'} type="submit" />
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default McpIntegrationInstanceConfigurationWorkflowDialog;
