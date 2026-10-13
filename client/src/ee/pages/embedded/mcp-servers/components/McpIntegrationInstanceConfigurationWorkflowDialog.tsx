import {Dialog, DialogTrigger} from '@/components/Dialog';
import {Input} from '@/components/Input/Input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {useGetIntegrationInstanceConfigurationsQuery} from '@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries';
import McpServerFormField from '@/shared/components/mcp-server/McpServerFormField';
import ServerWorkflowsDialogContent from '@/shared/components/server-workflows-dialog/ServerWorkflowsDialogContent';
import {
    McpServer,
    useCreateMcpIntegrationInstanceConfigurationMutation,
    useToolEligibleIntegrationInstanceConfigurationWorkflowsQuery,
    useUpdateMcpIntegrationInstanceConfigurationMutation,
} from '@/shared/middleware/graphql';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {ComponentIcon, ServerIcon} from 'lucide-react';
import {ReactNode, useCallback, useEffect, useMemo, useState} from 'react';
import {useForm} from 'react-hook-form';
import InlineSVG from 'react-inlinesvg';
import {z} from 'zod';

import McpIntegrationWorkflowsList from './McpIntegrationWorkflowsList';
import useMcpIntegrationWorkflowsForm from './hooks/useMcpIntegrationWorkflowsForm';
import {McpIntegrationInstanceConfigurationItemType} from './mcp-integration-instance-configuration-list/hooks/useMcpIntegrationInstanceConfigurationList';

const formSchema = z.object({
    integrationInstanceConfigurationId: z.string().min(1, 'Please select an integration instance configuration'),
    mcpServerId: z.string().min(1),
    selectedWorkflowIds: z.array(z.string()).min(1, 'Please select at least one workflow'),
});

interface McpIntegrationInstanceConfigurationDialogProps {
    mcpIntegrationInstanceConfiguration?: McpIntegrationInstanceConfigurationItemType;
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

    const eligibleWorkflows = useMemo(
        () =>
            eligibleWorkflowsData?.toolEligibleIntegrationInstanceConfigurationWorkflows.map((integrationWorkflow) => ({
                id: integrationWorkflow.id,
                label: integrationWorkflow.label || integrationWorkflow.id,
            })),
        [eligibleWorkflowsData]
    );

    const eligibleWorkflowIds = useMemo(
        () => eligibleWorkflows?.map((eligibleWorkflow) => eligibleWorkflow.id),
        [eligibleWorkflows]
    );

    const {
        componentName,
        form: workflowsForm,
        getWorkflowConfigurations,
        workflows,
    } = useMcpIntegrationWorkflowsForm({
        eligibleWorkflowIds,
        integrationInstanceConfigurationId: currentIntegrationInstanceConfigurationId,
    });

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

    const {control, getValues, handleSubmit, reset, setValue, trigger} = form;

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

    const handleSelectedWorkflowIdsChange = useCallback(
        (workflowIds: string[]) => setValue('selectedWorkflowIds', workflowIds),
        [setValue]
    );

    const saveMcpIntegrationInstanceConfiguration = () => {
        const formValues = getValues();

        const workflowConfigurations = getWorkflowConfigurations(formValues.selectedWorkflowIds);

        if (isEditMode) {
            updateMcpIntegrationInstanceConfigurationMutation.mutate({
                id: mcpIntegrationInstanceConfiguration.id,
                input: {
                    selectedWorkflowIds: formValues.selectedWorkflowIds,
                    workflowConfigurations,
                },
            });
        } else {
            createMcpIntegrationInstanceConfigurationMutation.mutate({
                input: {
                    integrationInstanceConfigurationId: formValues.integrationInstanceConfigurationId,
                    mcpServerId: formValues.mcpServerId,
                    selectedWorkflowIds: formValues.selectedWorkflowIds,
                    workflowConfigurations,
                },
            });
        }
    };

    const handleComplete = async () => {
        const workflowsValid = await workflowsForm.trigger();

        if (!workflowsValid) {
            return;
        }

        await handleSubmit(saveMcpIntegrationInstanceConfiguration)();
    };

    const basicStepContent = (
        <Form {...form}>
            <fieldset className="space-y-4 border-0">
                {isEditMode ? (
                    <>
                        <FormItem>
                            <FormLabel>Integration</FormLabel>

                            <Input disabled value={mcpIntegrationInstanceConfiguration.integration?.name || ''} />
                        </FormItem>

                        <FormItem>
                            <FormLabel>Integration Version</FormLabel>

                            <Input disabled value={`v${mcpIntegrationInstanceConfiguration.integrationVersion}`} />
                        </FormItem>
                    </>
                ) : (
                    <>
                        <McpServerFormField control={control} mcpServer={mcpServer} name="mcpServerId" />

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
                                                    <SelectItem key={configuration.id} value={String(configuration.id)}>
                                                        <div className="flex items-center gap-x-2">
                                                            {configuration.integration?.icon ? (
                                                                <InlineSVG
                                                                    className="size-4 flex-none"
                                                                    src={configuration.integration.icon}
                                                                />
                                                            ) : (
                                                                <ComponentIcon className="size-4 flex-none text-content-neutral-secondary" />
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
            </fieldset>
        </Form>
    );

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

            <ServerWorkflowsDialogContent
                basicStepContent={basicStepContent}
                canProceedFromBasicStep={isEditMode || !!currentIntegrationInstanceConfigurationId}
                description="Expose workflows (those with a New Workflow Call trigger) as tools of this MCP server."
                icon={<ServerIcon />}
                isEditMode={isEditMode}
                isSubmitPending={
                    createMcpIntegrationInstanceConfigurationMutation.isPending ||
                    updateMcpIntegrationInstanceConfigurationMutation.isPending
                }
                noEligibleWorkflowsMessage="No tool-eligible workflows found for this integration instance configuration. Only workflows with a New Workflow Call trigger can be added to an MCP server."
                onComplete={handleComplete}
                selectedWorkflowIds={selectedWorkflowIds}
                title={isEditMode ? 'Edit Workflows' : 'Add Workflows'}
                validateBasicStep={() => trigger(['integrationInstanceConfigurationId', 'mcpServerId'])}
                workflowCount={workflows?.length}
                workflowsList={
                    workflows && (
                        <McpIntegrationWorkflowsList
                            componentName={componentName ?? ''}
                            form={workflowsForm}
                            onSelectedWorkflowIdsChange={handleSelectedWorkflowIdsChange}
                            selectedWorkflowIds={selectedWorkflowIds}
                            workflows={workflows}
                        />
                    )
                }
            />
        </Dialog>
    );
};

export default McpIntegrationInstanceConfigurationWorkflowDialog;
