import Button from '@/components/Button/Button';
import ComboBox from '@/components/ComboBox/ComboBox';
import {Label} from '@/components/ui/label';
import AgentSection from '@/pages/automation/agents/components/detail/AgentSection';
import AgentToolRow from '@/pages/automation/agents/components/detail/AgentToolRow';
import invalidateAgentQueries from '@/pages/automation/agents/utils/invalidateAgentQueries';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import ComponentConfigDialog from '@/shared/components/component-config/ComponentConfigDialog';
import {
    AiAgentElement,
    useAddAiAgentElementMutation,
    useDeleteAiAgentElementMutation,
    useUpdateAiAgentElementMutation,
} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useGetRootComponentClusterElementDefinitions} from '@/shared/queries/platform/clusterElementDefinitions.queries';
import {useQueryClient} from '@tanstack/react-query';
import {PlusIcon} from 'lucide-react';
import {useMemo, useState} from 'react';
import {toast} from 'sonner';

interface AgentToolsCardProps {
    agentId: string;
    elements: AiAgentElement[];
}

const SKILLS_TOOL_NAME = 'skillsTool';

const AgentToolsCard = ({agentId, elements}: AgentToolsCardProps) => {
    const [open, setOpen] = useState(false);
    const [componentName, setComponentName] = useState('');
    const [actionName, setActionName] = useState('');
    const [configuredElement, setConfiguredElement] = useState<AiAgentElement | null>(null);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const toolElements = elements.filter((element) => element.kind === 'TOOL');

    const {data: components = []} = useGetComponentDefinitionsQuery({actionDefinitions: true}, open);

    const {data: agentTools = []} = useGetRootComponentClusterElementDefinitions(
        {clusterElementType: 'tools', rootComponentName: 'aiAgent', rootComponentVersion: 1},
        open
    );

    const toolComponentNames = useMemo(() => new Set(agentTools.map((tool) => tool.componentName)), [agentTools]);

    const toolComponents = useMemo(
        () => components.filter((component) => toolComponentNames.has(component.name) && !component.clusterRoot),
        [components, toolComponentNames]
    );

    const selectedComponent = toolComponents.find((component) => component.name === componentName);

    const selectableTools = useMemo(
        () => agentTools.filter((tool) => tool.componentName === componentName && tool.name !== SKILLS_TOOL_NAME),
        [agentTools, componentName]
    );

    const componentItems = useMemo(
        () =>
            toolComponents.map((component) => ({
                icon: component.icon,
                label: component.title || component.name,
                value: component.name,
            })),
        [toolComponents]
    );

    const toolItems = useMemo(
        () =>
            selectableTools.map((tool) => ({
                description: tool.description,
                label: tool.title || tool.name,
                value: tool.name,
            })),
        [selectableTools]
    );

    const queryClient = useQueryClient();

    const addAiAgentElementMutation = useAddAiAgentElementMutation({
        onError: (error) => toast.error(error instanceof Error ? error.message : 'Failed to add the tool.'),
        onSuccess: () => {
            invalidateAgentQueries(queryClient);

            setOpen(false);
            setComponentName('');
            setActionName('');
        },
    });

    const deleteAiAgentElementMutation = useDeleteAiAgentElementMutation({
        onError: (error) => toast.error(error instanceof Error ? error.message : 'Failed to remove the tool.'),
        onSuccess: () => invalidateAgentQueries(queryClient),
    });

    const updateAiAgentElementMutation = useUpdateAiAgentElementMutation({
        onError: (error) => toast.error(error instanceof Error ? error.message : 'Failed to update the tool.'),
        onSuccess: () => invalidateAgentQueries(queryClient),
    });

    const isBusy = updateAiAgentElementMutation.isPending;

    const handleRequiresApprovalToggle = (element: AiAgentElement, checked: boolean) => {
        const parameters = {...(element.parameters ?? {})} as Record<string, unknown>;

        if (checked) {
            parameters.requiresApproval = true;
        } else {
            delete parameters.requiresApproval;
        }

        updateAiAgentElementMutation.mutate({input: {id: element.id, parameters}});
    };

    const handleComponentChange = (value: string) => {
        setComponentName(value);
        setActionName('');
    };

    const closeAddDialog = () => {
        setOpen(false);
        setComponentName('');
        setActionName('');
    };

    const handleSave = ({
        connectionId,
        parameters,
    }: {
        connectionId: string | null;
        parameters: Record<string, unknown>;
    }) => {
        if (!selectedComponent || !actionName) {
            return;
        }

        addAiAgentElementMutation.mutate({
            input: {
                agentId,
                connectionId,
                kind: 'TOOL',
                parameters: {
                    actionName,
                    componentName: selectedComponent.name,
                    componentVersion: selectedComponent.version,
                    parameters,
                },
            },
        });
    };

    return (
        <AgentSection
            action={
                <Button
                    icon={<PlusIcon />}
                    label="Add tool"
                    onClick={() => setOpen(true)}
                    size="sm"
                    variant="outline"
                />
            }
            title="Tools"
        >
            {toolElements.length === 0 ? (
                <p className="text-sm text-muted-foreground">No tools added yet.</p>
            ) : (
                <ul className="space-y-2">
                    {toolElements.map((element) => (
                        <AgentToolRow
                            element={element}
                            isBusy={isBusy}
                            key={element.id}
                            onConfigure={setConfiguredElement}
                            onDelete={(deletedElement) => deleteAiAgentElementMutation.mutate({id: deletedElement.id})}
                            onRequiresApprovalToggle={handleRequiresApprovalToggle}
                        />
                    ))}
                </ul>
            )}

            {open && currentWorkspaceId != null && (
                <ComponentConfigDialog
                    description="Choose a component and tool, then set the connection and properties it uses."
                    onClose={closeAddDialog}
                    onSubmit={handleSave}
                    open
                    pending={addAiAgentElementMutation.isPending}
                    picker={
                        <>
                            <div className="space-y-1">
                                <Label htmlFor="agent-tool-component">Component</Label>

                                <ComboBox
                                    ariaLabel="Component"
                                    items={componentItems}
                                    maxHeight
                                    name="agentToolComponent"
                                    onChange={(item) => handleComponentChange(String(item?.value ?? ''))}
                                    value={componentName}
                                />
                            </div>

                            <div className="space-y-1">
                                <Label htmlFor="agent-tool-name">Tool</Label>

                                <ComboBox
                                    ariaLabel="Tool"
                                    disabled={!componentName}
                                    emptyMessage="No tools for this component."
                                    items={toolItems}
                                    maxHeight
                                    name="agentToolName"
                                    onChange={(item) => setActionName(String(item?.value ?? ''))}
                                    value={actionName}
                                />
                            </div>
                        </>
                    }
                    submitLabel="Add"
                    target={
                        selectedComponent && actionName
                            ? {
                                  clusterElementName: actionName,
                                  componentName: selectedComponent.name,
                                  componentVersion: selectedComponent.version,
                                  title: selectedComponent.title || selectedComponent.name,
                              }
                            : null
                    }
                    title="Add Tool"
                    workspaceId={Number(currentWorkspaceId)}
                />
            )}

            {configuredElement && currentWorkspaceId != null && (
                <ComponentConfigDialog
                    initialValues={{
                        connectionId: configuredElement.connectionId ?? null,
                        parameters: (configuredElement.parameters?.parameters ?? {}) as Record<string, unknown>,
                    }}
                    onClose={() => setConfiguredElement(null)}
                    onSubmit={async ({connectionId: nextConnectionId, parameters}) => {
                        const elementParameters = {...(configuredElement.parameters ?? {})} as Record<string, unknown>;

                        elementParameters.parameters = parameters;

                        await updateAiAgentElementMutation.mutateAsync({
                            input: {
                                connectionId: nextConnectionId,
                                id: configuredElement.id,
                                parameters: elementParameters,
                            },
                        });

                        setConfiguredElement(null);
                    }}
                    open
                    pending={updateAiAgentElementMutation.isPending}
                    target={{
                        clusterElementName: String(configuredElement.parameters?.actionName ?? ''),
                        componentName: String(configuredElement.parameters?.componentName ?? ''),
                        componentVersion: Number(configuredElement.parameters?.componentVersion ?? 1),
                    }}
                    workspaceId={Number(currentWorkspaceId)}
                />
            )}
        </AgentSection>
    );
};

export default AgentToolsCard;
