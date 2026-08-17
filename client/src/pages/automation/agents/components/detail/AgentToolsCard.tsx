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

// skillsTool is never picked by hand: AiAgentWorkflowGenerator emits it automatically whenever the agent has
// SKILL elements and the `skills` built-in is on, so offering it here would let a user add a second,
// conflicting copy of a tool the generator already owns.
const SKILLS_TOOL_NAME = 'skillsTool';

const AgentToolsCard = ({agentId, elements}: AgentToolsCardProps) => {
    const [open, setOpen] = useState(false);
    const [componentName, setComponentName] = useState('');
    const [actionName, setActionName] = useState('');
    const [configuredElement, setConfiguredElement] = useState<AiAgentElement | null>(null);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const toolElements = elements.filter((element) => element.kind === 'TOOL');

    const {data: components = []} = useGetComponentDefinitionsQuery({actionDefinitions: true}, open);

    // Cluster roots (aiAgent, the approval components, …) are configured through their child cluster elements
    // rather than through their own properties, so the tool config dialog has nothing to show for them and an
    // agent cannot use one as a plain tool. They stay available in the workflow canvas's cluster-element editor,
    // which is the surface built to configure them.
    // The tools an agent can actually be given, straight from the aiAgent root's TOOLS cluster elements —
    // the same source the simple editor's picker reads. A component's ACTIONS are not its tools: a component
    // declares a tool per action explicitly (googleMail: 11 actions, 11 TOOLS elements), and a component that
    // declares none offers no tools at all. Listing actions instead produced picks whose tool definition does
    // not exist, which the config dialog then failed to load.
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

    // updateAiAgentElement replaces a TOOL row's ENTIRE parameters map (parameters-replace, not a
    // patch — see AiAgentFacadeImpl.updateAgentElement), so every toggle sends the full existing
    // parameters object back with only requiresApproval added or removed, never a partial diff.
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

    // connectionId rides on the element row itself; the tool's own parameters nest under `parameters`, the same
    // shape the configure path writes back, so a tool added with properties reads identically to one edited later.
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

            {/* The element row's parameters map holds the tool's identity (componentName/componentVersion/
                actionName) alongside a nested `parameters` map holding the tool's own inputs. The dialog edits
                only the nested map plus the connection; updateAiAgentElement replaces the WHOLE map, so the
                identity keys and requiresApproval have to ride along or they are dropped -- same reason the
                requiresApproval toggle above sends the full object back. */}

            {/* Adding runs through the same dialog editing does, with the component/tool panes in its picker
                slot: a tool added blind and configured afterwards is two trips for one decision, and its
                connection is usually the point of adding it. */}

            {open && currentWorkspaceId != null && (
                <ComponentConfigDialog
                    description="Choose a component and tool, then set the connection and properties it uses."
                    onClose={closeAddDialog}
                    onSubmit={handleSave}
                    open
                    pending={addAiAgentElementMutation.isPending}
                    picker={
                        <>
                            {/* Two comboboxes rather than the two-pane browser this replaced: the panes ate the
                                height the Connection and Properties tabs need, and a combobox still filters,
                                which is what the 190+ component catalog actually requires. */}

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
                        // Addressed as a TOOLS cluster element named by the operation. Components that register
                        // no cluster element of their own are served one by ClusterElementDefinitionService, which
                        // projects the matching action as a tool -- the client never deals in actions here.
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
