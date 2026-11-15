import {Dialog, DialogTrigger} from '@/components/Dialog';
import {Input} from '@/components/Input/Input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Label} from '@/components/ui/label';
import {Workflow} from '@/ee/shared/middleware/embedded/configuration';
import useMcpWorkflowToolForms, {
    SavedToolParametersByWorkflowIdType,
} from '@/pages/platform/mcp-servers/hooks/useMcpWorkflowToolForms';
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
import {ReactNode, useCallback, useMemo, useState} from 'react';

import McpIntegrationWorkflowsList from '../McpIntegrationWorkflowsList';
import McpAutomationWorkflowToolFields from './McpAutomationWorkflowToolFields';
import {McpAutomationProjectConfigurationItemType} from './hooks/useMcpAutomationProjectConfigurationList';
import {fetchToolEligibleAutomationWorkflowProperties} from './mcpAutomationWorkflowToolProperties';

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

    const savedToolParametersByWorkflowUuid: SavedToolParametersByWorkflowIdType = useMemo(
        () =>
            new Map(
                (mcpAutomationProjectConfiguration?.workflows ?? []).map((workflow) => [
                    workflow.workflowUuid,
                    workflow.parameters &&
                    typeof workflow.parameters === 'object' &&
                    !Array.isArray(workflow.parameters)
                        ? (workflow.parameters as Record<string, unknown>)
                        : undefined,
                ])
            ),
        [mcpAutomationProjectConfiguration?.workflows]
    );

    const loadWorkflowProperties = useCallback(
        (workflowUuid: string) =>
            fetchToolEligibleAutomationWorkflowProperties(queryClient, {
                automationProjectConfigurationId: configurationId!,
                workflowUuid,
            }),
        [configurationId, queryClient]
    );

    const toolForms = useMcpWorkflowToolForms({
        loadWorkflowProperties,
        savedParametersByWorkflowId: savedToolParametersByWorkflowUuid,
        seedKey: configurationId ?? '',
        selectedWorkflowIds: selectedWorkflowUuids,
    });

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

    const handleComplete = async () => {
        const toolParameters = await toolForms.buildToolParameters(selectedWorkflowUuids);

        const workflowConfigurations = toolParameters.map((toolParameter) => ({
            parameters: toolParameter.parameters,
            workflowUuid: toolParameter.workflowId,
        }));

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
                isSubmitPending={createMutation.isPending || updateMutation.isPending || toolForms.isAwaitingSeeding}
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
                            renderSelectedWorkflowExtra={(workflow) =>
                                configurationId && workflow.id ? (
                                    <div className="mt-2">
                                        <McpAutomationWorkflowToolFields
                                            automationProjectConfigurationId={configurationId}
                                            toolForms={toolForms}
                                            workflowUuid={workflow.id}
                                        />
                                    </div>
                                ) : null
                            }
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
