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
import IntegrationInstanceConfigurationDialogBasicStepIntegrationVersionsSelect from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogBasicStepIntegrationVersionsSelect';
import IntegrationInstanceConfigurationDialogBasicStepIntegrationsComboBox from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogBasicStepIntegrationsComboBox';
import {
    McpIntegration,
    McpServer,
    useCreateMcpIntegrationMutation,
    useToolEligibleIntegrationVersionWorkflowsQuery,
    useUpdateMcpIntegrationMutation,
} from '@/shared/middleware/graphql';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useMemo, useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

const formSchema = z.object({
    integrationId: z.number().min(1, 'Please select an integration'),
    integrationVersion: z.number().min(1, 'Please select a version'),
    mcpServerId: z.string().min(1),
    selectedWorkflowIds: z.array(z.string()).min(1, 'Please select at least one workflow'),
});

interface McpIntegrationDialogProps {
    mcpIntegration?: McpIntegration;
    mcpServer?: McpServer;
    onClose?: () => void;
    triggerNode?: ReactNode;
}

const McpIntegrationWorkflowDialog = ({mcpIntegration, mcpServer, onClose, triggerNode}: McpIntegrationDialogProps) => {
    const isEditMode = !!mcpIntegration?.id;

    const [isOpen, setIsOpen] = useState(!triggerNode);
    const [currentIntegrationId, setCurrentIntegrationId] = useState<number | undefined>(
        isEditMode ? Number(mcpIntegration.integration?.id) : undefined
    );
    const [currentIntegrationVersion, setCurrentIntegrationVersion] = useState<number | undefined>(
        isEditMode ? (mcpIntegration.integrationVersion ?? undefined) : undefined
    );

    const {data: eligibleWorkflowsData} = useToolEligibleIntegrationVersionWorkflowsQuery(
        {
            integrationId: String(currentIntegrationId || 0),
            integrationVersion: currentIntegrationVersion || 0,
        },
        {enabled: !!(currentIntegrationId && currentIntegrationVersion)}
    );

    const eligibleWorkflows = eligibleWorkflowsData?.toolEligibleIntegrationVersionWorkflows;

    const existingWorkflowLabels = useMemo(() => {
        const mcpIntegrationWorkflows = mcpIntegration?.mcpIntegrationWorkflows ?? [];

        return new Set(
            mcpIntegrationWorkflows
                .filter((workflow) => workflow?.workflow?.label != null)
                .map((workflow) => workflow!.workflow!.label!)
        );
    }, [mcpIntegration?.mcpIntegrationWorkflows]);

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
            integrationId: isEditMode ? Number(mcpIntegration.integration?.id) : undefined,
            integrationVersion: isEditMode ? (mcpIntegration.integrationVersion ?? undefined) : undefined,
            mcpServerId: mcpIntegration?.mcpServerId || mcpServer?.id || '',
            selectedWorkflowIds: [],
        },
        resolver: zodResolver(formSchema),
    });

    const {control, getValues, handleSubmit, reset, resetField, setValue} = form;

    const selectedWorkflowIds = form.watch('selectedWorkflowIds');

    // Pre-populate selected workflow IDs once eligible workflows load in edit mode
    useMemo(() => {
        if (isEditMode && initialSelectedWorkflowIds.length > 0 && selectedWorkflowIds.length === 0) {
            setValue('selectedWorkflowIds', initialSelectedWorkflowIds);
        }
    }, [initialSelectedWorkflowIds, isEditMode, selectedWorkflowIds.length, setValue]);

    const queryClient = useQueryClient();

    const onCreateSuccess = () => {
        queryClient.invalidateQueries({
            queryKey: ['mcpIntegrationsByServerId'],
        });

        closeDialog();
    };

    const createMcpIntegrationMutation = useCreateMcpIntegrationMutation({onSuccess: onCreateSuccess});

    const updateMcpIntegrationMutation = useUpdateMcpIntegrationMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ['mcpIntegrationsByServerId'],
            });

            closeDialog();
        },
    });

    const closeDialog = () => {
        setIsOpen(false);

        if (onClose) {
            onClose();
        }

        reset();
    };

    function saveMcpIntegration() {
        const formValues = getValues();

        if (isEditMode) {
            updateMcpIntegrationMutation.mutate({
                id: mcpIntegration.id,
                input: {
                    selectedWorkflowIds: formValues.selectedWorkflowIds,
                },
            });
        } else {
            createMcpIntegrationMutation.mutate({
                input: {
                    integrationId: formValues.integrationId.toString(),
                    integrationVersion: formValues.integrationVersion,
                    mcpServerId: formValues.mcpServerId,
                    selectedWorkflowIds: formValues.selectedWorkflowIds,
                },
            });
        }
    }

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
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(saveMcpIntegration)}>
                        {isEditMode && (
                            <>
                                <FormItem>
                                    <FormLabel>Integration</FormLabel>

                                    <Input disabled value={mcpIntegration.integration?.name || ''} />
                                </FormItem>

                                <FormItem>
                                    <FormLabel>Integration Version</FormLabel>

                                    <Input disabled value={`v${mcpIntegration.integrationVersion}`} />
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
                                    name="integrationId"
                                    render={({field}) => (
                                        <FormItem>
                                            <FormLabel>Integration</FormLabel>

                                            <FormControl>
                                                <IntegrationInstanceConfigurationDialogBasicStepIntegrationsComboBox
                                                    onBlur={field.onBlur}
                                                    onChange={(item) => {
                                                        if (item) {
                                                            setValue('integrationId', item.value as number);
                                                            resetField('integrationVersion');
                                                            setValue('selectedWorkflowIds', []);

                                                            setCurrentIntegrationId(item.value as number);
                                                            setCurrentIntegrationVersion(undefined);
                                                        }
                                                    }}
                                                    value={field.value}
                                                />
                                            </FormControl>

                                            <FormMessage />
                                        </FormItem>
                                    )}
                                    shouldUnregister={false}
                                />

                                {currentIntegrationId && (
                                    <FormField
                                        control={control}
                                        name="integrationVersion"
                                        render={({field}) => (
                                            <FormItem>
                                                <FormLabel>Integration Version</FormLabel>

                                                <FormControl>
                                                    <IntegrationInstanceConfigurationDialogBasicStepIntegrationVersionsSelect
                                                        integrationId={currentIntegrationId}
                                                        integrationVersion={currentIntegrationVersion}
                                                        onChange={(value) => {
                                                            field.onChange(value);
                                                            setCurrentIntegrationVersion(value);
                                                            setValue('selectedWorkflowIds', []);
                                                        }}
                                                    />
                                                </FormControl>

                                                <FormMessage />
                                            </FormItem>
                                        )}
                                        shouldUnregister={false}
                                    />
                                )}
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

export default McpIntegrationWorkflowDialog;
