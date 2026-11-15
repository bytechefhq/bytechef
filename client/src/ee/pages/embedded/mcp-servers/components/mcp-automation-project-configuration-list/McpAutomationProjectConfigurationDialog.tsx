import {Dialog, DialogTrigger} from '@/components/Dialog';
import {Input} from '@/components/Input/Input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Label} from '@/components/ui/label';
import {Workflow} from '@/ee/shared/middleware/embedded/configuration';
import ServerWorkflowsDialogContent from '@/shared/components/server-workflows-dialog/ServerWorkflowsDialogContent';
import {
    McpServer,
    useAutomationProjectConfigurationsQuery,
    useAutomationProjectsQuery,
    useCreateMcpAutomationProjectConfigurationMutation,
    useMcpAutomationProjectConfigurationToolWorkflowUuidsQuery,
    useUpdateMcpAutomationProjectConfigurationMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ServerIcon} from 'lucide-react';
import {ReactNode, useMemo, useState} from 'react';

import McpIntegrationWorkflowsList from '../McpIntegrationWorkflowsList';
import {McpAutomationProjectConfigurationItemType} from './hooks/useMcpAutomationProjectConfigurationList';
import {buildMcpAutomationToolParameters, getMcpAutomationToolText} from './mcpAutomationToolParameters';

interface McpAutomationProjectConfigurationDialogProps {
    mcpAutomationProjectConfiguration?: McpAutomationProjectConfigurationItemType;
    mcpServer: McpServer;
    onClose?: () => void;
    triggerNode?: ReactNode;
}

const McpAutomationProjectConfigurationDialog = ({
    mcpAutomationProjectConfiguration,
    mcpServer,
    onClose,
    triggerNode,
}: McpAutomationProjectConfigurationDialogProps) => {
    const isEditMode = !!mcpAutomationProjectConfiguration?.id;

    const [isOpen, setIsOpen] = useState(!triggerNode);
    const [projectId, setProjectId] = useState<string | undefined>(mcpAutomationProjectConfiguration?.projectId);
    const [configurationId, setConfigurationId] = useState<string | undefined>(
        mcpAutomationProjectConfiguration?.automationProjectConfigurationId
    );
    const [selectedWorkflowUuids, setSelectedWorkflowUuids] = useState<string[]>(
        () => mcpAutomationProjectConfiguration?.workflows.map((workflow) => workflow.workflowUuid) ?? []
    );

    const [parametersByWorkflowUuid, setParametersByWorkflowUuid] = useState<Record<string, Record<string, unknown>>>(
        () =>
            Object.fromEntries(
                (mcpAutomationProjectConfiguration?.workflows ?? []).map((workflow) => [
                    workflow.workflowUuid,
                    (workflow.parameters as Record<string, unknown> | null) ?? {},
                ])
            )
    );

    const queryClient = useQueryClient();

    const {data: projectsData} = useAutomationProjectsQuery({environmentId: mcpServer.environmentId});

    const {data: configurationsData} = useAutomationProjectConfigurationsQuery(
        {environmentId: mcpServer.environmentId, projectId: projectId ?? '0'},
        {enabled: !!projectId}
    );

    const {data: toolWorkflowUuidsData} = useMcpAutomationProjectConfigurationToolWorkflowUuidsQuery(
        {automationProjectConfigurationId: configurationId ?? '0'},
        {enabled: !!configurationId}
    );

    const projects = useMemo(
        () =>
            (projectsData?.automationProjects ?? []).filter(
                (project) => project.published && project.configurationCount > 0
            ),
        [projectsData]
    );

    const configurations = useMemo(
        () =>
            (configurationsData?.automationProjectConfigurations ?? []).filter(
                (configuration) => configuration.environmentId === mcpServer.environmentId
            ),
        [configurationsData, mcpServer.environmentId]
    );

    const selectedConfiguration = configurations.find((configuration) => configuration.id === configurationId);

    const workflows = useMemo(() => {
        if (!selectedConfiguration || !toolWorkflowUuidsData) {
            return undefined;
        }

        const toolWorkflowUuids = new Set(toolWorkflowUuidsData.mcpAutomationProjectConfigurationToolWorkflowUuids);

        return selectedConfiguration.workflows
            .filter((workflow) => toolWorkflowUuids.has(workflow.workflowUuid))
            .map((workflow) => ({id: workflow.workflowUuid, label: workflow.label}) as Workflow);
    }, [selectedConfiguration, toolWorkflowUuidsData]);

    const enabledWorkflowUuids = useMemo(
        () =>
            (selectedConfiguration?.workflows ?? [])
                .filter((workflow) => workflow.enabled)
                .map((workflow) => workflow.workflowUuid),
        [selectedConfiguration]
    );

    const closeDialog = () => {
        setIsOpen(false);

        if (onClose) {
            onClose();
        }
    };

    const invalidateAndClose = () => {
        void queryClient.invalidateQueries({queryKey: ['mcpAutomationProjectConfigurationsByServerId']});
        void queryClient.invalidateQueries({queryKey: ['embeddedMcpServers']});

        closeDialog();
    };

    const createMutation = useCreateMcpAutomationProjectConfigurationMutation({onSuccess: invalidateAndClose});
    const updateMutation = useUpdateMcpAutomationProjectConfigurationMutation({onSuccess: invalidateAndClose});

    const handleToolTextChange = (workflowUuid: string, key: 'toolDescription' | 'toolName', value: string) =>
        setParametersByWorkflowUuid((current) => ({
            ...current,
            [workflowUuid]: buildMcpAutomationToolParameters(current[workflowUuid], key, value),
        }));

    const workflowConfigurations = selectedWorkflowUuids.map((workflowUuid) => {
        const parameters = parametersByWorkflowUuid[workflowUuid];

        return parameters && Object.keys(parameters).length > 0 ? {parameters, workflowUuid} : {workflowUuid};
    });

    const handleComplete = () => {
        if (isEditMode) {
            updateMutation.mutate({
                id: mcpAutomationProjectConfiguration.id,
                input: {workflowConfigurations, workflowUuids: selectedWorkflowUuids},
            });
        } else {
            createMutation.mutate({
                input: {
                    automationProjectConfigurationId: configurationId!,
                    mcpServerId: mcpServer.id,
                    workflowConfigurations,
                    workflowUuids: selectedWorkflowUuids,
                },
            });
        }
    };

    const basicStepContent = (
        <fieldset className="space-y-4 border-0">
            {isEditMode ? (
                <div className="space-y-2">
                    <Label>Automation Configuration</Label>

                    <Input disabled value={mcpAutomationProjectConfiguration.automationProjectConfigurationName} />
                </div>
            ) : (
                <>
                    <div className="space-y-2">
                        <Label>Automation</Label>

                        <Select
                            onValueChange={(value) => {
                                setProjectId(value);
                                setConfigurationId(undefined);
                                setSelectedWorkflowUuids([]);
                            }}
                            value={projectId}
                        >
                            <SelectTrigger>
                                <SelectValue placeholder="Select an automation..." />
                            </SelectTrigger>

                            <SelectContent>
                                {projects.map((project) => (
                                    <SelectItem key={project.id} value={project.id}>
                                        {project.name}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>

                    <div className="space-y-2">
                        <Label>Automation Configuration</Label>

                        <Select
                            disabled={!projectId}
                            onValueChange={(value) => {
                                setConfigurationId(value);
                                setSelectedWorkflowUuids([]);
                            }}
                            value={configurationId}
                        >
                            <SelectTrigger>
                                <SelectValue placeholder="Select a configuration..." />
                            </SelectTrigger>

                            <SelectContent>
                                {configurations.map((configuration) => (
                                    <SelectItem key={configuration.id} value={configuration.id}>
                                        {configuration.name}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>
                </>
            )}
        </fieldset>
    );

    return (
        <Dialog
            onOpenChange={(open) => {
                if (open) {
                    setIsOpen(open);
                } else {
                    closeDialog();
                }
            }}
            open={isOpen}
        >
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            <ServerWorkflowsDialogContent
                basicStepContent={basicStepContent}
                canProceedFromBasicStep={isEditMode || !!configurationId}
                description="Expose workflows (those with a New Workflow Call trigger) of an automation configuration as tools of this MCP server."
                icon={<ServerIcon />}
                isEditMode={isEditMode}
                isSubmitPending={createMutation.isPending || updateMutation.isPending}
                noEligibleWorkflowsMessage="This automation configuration has no workflows that can become tools. Only workflows that are turned on and have a New Workflow Call trigger can be added to an MCP server."
                onComplete={handleComplete}
                selectedWorkflowIds={selectedWorkflowUuids}
                title={isEditMode ? 'Edit Automation Workflows' : 'Add Automation Workflows'}
                validateBasicStep={() => isEditMode || !!configurationId}
                workflowCount={workflows?.length}
                workflowsList={
                    workflows && (
                        <McpIntegrationWorkflowsList
                            enabledWorkflowIds={enabledWorkflowUuids}
                            onSelectedWorkflowIdsChange={setSelectedWorkflowUuids}
                            renderSelectedWorkflowExtra={(workflow) => (
                                <fieldset className="mt-2 space-y-2 border-0 p-0">
                                    <Input
                                        aria-label={`${workflow.label || workflow.id} tool name`}
                                        onChange={(event) =>
                                            handleToolTextChange(workflow.id!, 'toolName', event.target.value)
                                        }
                                        placeholder="Tool name (defaults to the workflow name)"
                                        value={getMcpAutomationToolText(
                                            parametersByWorkflowUuid[workflow.id!],
                                            'toolName'
                                        )}
                                    />

                                    <Input
                                        aria-label={`${workflow.label || workflow.id} tool description`}
                                        onChange={(event) =>
                                            handleToolTextChange(workflow.id!, 'toolDescription', event.target.value)
                                        }
                                        placeholder="Tool description (defaults to the workflow description)"
                                        value={getMcpAutomationToolText(
                                            parametersByWorkflowUuid[workflow.id!],
                                            'toolDescription'
                                        )}
                                    />
                                </fieldset>
                            )}
                            selectedWorkflowIds={selectedWorkflowUuids}
                            workflows={workflows}
                        />
                    )
                }
            />
        </Dialog>
    );
};

export default McpAutomationProjectConfigurationDialog;
