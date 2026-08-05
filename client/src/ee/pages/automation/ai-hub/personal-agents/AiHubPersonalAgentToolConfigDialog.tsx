import {
    AiHubPersonalAgentToolI,
    useUpdateAiHubPersonalAgentToolConfigMutation,
} from '@/ee/pages/automation/ai-hub/personal-agents/hooks/useAiHubPersonalAgents';
import ToolConfigDialog, {ToolConfigDialogValuesI} from '@/ee/pages/automation/ai-hub/tools/dialogs/ToolConfigDialog';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';

interface AiHubPersonalAgentToolConfigDialogPropsI {
    onClose: () => void;
    open: boolean;
    /**
     * The tool template row to configure. The dialog reads {@code connectionId} + {@code parameters} from
     * here to pre-fill the form, and on submit fires {@code updateAiHubPersonalAgentToolConfig} keyed by the row
     * id. Pass {@code null} when the dialog isn't open (paired with {@code open=false}); the dialog stays
     * mounted but skips fetches.
     */
    tool: AiHubPersonalAgentToolI | null;
    workspaceId: number;
}

/**
 * Configure-tool dialog opened from the personal-agent tool list's 3-dot menu. Thin wrapper around
 * {@link ToolConfigDialog} that wires the {@code updateAiHubPersonalAgentToolConfig} mutation, mirroring the
 * structure of {@code TaskToolDialog} so users see the same Connection / Properties tab layout
 * across both flows.
 *
 * <p>
 * The dialog pre-fills with the tool template row's persisted values: {@code connectionId} as the picker's
 * initial selection, {@code parameters} merged on top of the cluster element's schema-derived defaults so
 * the user sees what they previously configured. Submit fires the update mutation with sanitized values;
 * empty strings on the parameters form are coerced to null so the persisted map doesn't carry placeholder
 * empties.
 * </p>
 */
const AiHubPersonalAgentToolConfigDialog = ({
    onClose,
    open,
    tool,
    workspaceId,
}: AiHubPersonalAgentToolConfigDialogPropsI) => {
    // Fetch the component definition so the dialog can resolve a human-readable action title for the header.
    // The persisted personal_agent_tool row only stores the operation slug; the title comes from the live
    // component definition the same way {@code AiAgentTool} resolves it. Per-row query is the same pattern as
    // TaskToolDialog uses.
    const {data: componentDefinition} = useGetComponentDefinitionQuery(
        {componentName: tool?.componentName ?? '', componentVersion: tool?.componentVersion ?? 1},
        tool != null
    );

    const action = componentDefinition?.actions?.find((entry) => entry.name === tool?.operationName);
    const updateMutation = useUpdateAiHubPersonalAgentToolConfigMutation();

    const target = tool
        ? {
              clusterElementName: tool.operationName,
              componentName: tool.componentName,
              componentVersion: tool.componentVersion,
              description: action?.description,
              title: action?.title || tool.operationName,
          }
        : null;

    const initialValues = tool
        ? {
              connectionId: tool.connectionId != null ? String(tool.connectionId) : null,
              parameters: tool.parameters,
          }
        : undefined;

    const handleSubmit = async (values: ToolConfigDialogValuesI) => {
        if (tool == null) {
            return;
        }

        await updateMutation.mutateAsync({
            input: {
                connectionId: values.connectionId,
                // Cast through unknown to satisfy the Any-scalar codegen output. Server treats as Map<String, ?>.
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                parameters: values.parameters as any,
                toolId: String(tool.id),
                workspaceId: String(workspaceId),
            },
        });

        onClose();
    };

    return (
        <ToolConfigDialog
            description={
                action?.description ??
                'Pre-bind a connection and default parameters. Each task the agent spawns starts with these.'
            }
            initialValues={initialValues}
            onClose={onClose}
            onSubmit={handleSubmit}
            open={open}
            pending={updateMutation.isPending}
            submitLabel="Save"
            target={target}
            title={`Configure ${action?.title || tool?.operationName || 'tool'}`}
            workspaceId={workspaceId}
        />
    );
};

export default AiHubPersonalAgentToolConfigDialog;
