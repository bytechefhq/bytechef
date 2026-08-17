import useChatToolsCache from '@/ee/pages/automation/ai-hub/tools/hooks/useChatToolsCache';
import ComponentConfigDialog, {
    ComponentConfigDialogTargetI,
    ComponentConfigDialogValuesI,
} from '@/shared/components/component-config/ComponentConfigDialog';
import {AttachAiHubChatToolInput, useAttachAiHubChatToolMutation} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';

// Re-export the shared shape under the chat-flavoured name so existing consumers
// (AiHubComposer) keep their import path while sharing the underlying interface with the
// generalised dialog. Re-exporting via `export type` rather than a fresh interface because adding new
// fields here would silently diverge from the generalised dialog's expectations.
export type {ComponentConfigDialogTargetI as ChatToolDialogTargetI};

interface ChatToolDialogProps {
    chatId: string;
    onClose: () => void;
    open: boolean;
    target: ComponentConfigDialogTargetI | null;
    workspaceId: number;
}

/**
 * Thin wrapper around {@link ComponentConfigDialog} that wires the attach-chat-tool mutation. Mirrors the
 * chat-driven {@code AttachChatToolToolCallback} affordance — same persistence shape, same idempotency
 * semantics — so the LLM and the user click into the same {@code ai_hub_chat_tool} row.
 *
 * <p>
 * Submit fires {@code attachAiHubChatTool} with the dialog's sanitized values. Success invalidates the
 * chat-tools cache so the in-chat resource panel reflects the new attachment without a manual refresh.
 * The dialog itself owns the connection picker, properties form, sanitization, and create-new-connection
 * affordance — see {@link ComponentConfigDialog}.
 * </p>
 */
const ChatToolDialog = ({chatId, onClose, open, target, workspaceId}: ChatToolDialogProps) => {
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {invalidate} = useChatToolsCache();

    const attachMutation = useAttachAiHubChatToolMutation({
        onSuccess: () => {
            invalidate(chatId, workspaceId);

            onClose();
        },
    });

    const handleSubmit = async (values: ComponentConfigDialogValuesI) => {
        if (target == null) {
            return;
        }

        const input: AttachAiHubChatToolInput = {
            chatId,
            clusterElementName: target.clusterElementName,
            componentName: target.componentName,
            componentVersion: target.componentVersion,
            connectionId: values.connectionId,
            environment: environmentId ?? 1,
            // Cast through unknown to satisfy the Any-scalar codegen output. Server treats as Map<String, ?>.
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            parameters: values.parameters as any,
            workspaceId: String(workspaceId),
        };

        await attachMutation.mutateAsync({input});
    };

    return (
        <ComponentConfigDialog
            description={
                target?.description ?? 'Configure a connection and default parameters. The agent can override per call.'
            }
            onClose={onClose}
            onSubmit={handleSubmit}
            open={open}
            pending={attachMutation.isPending}
            submitLabel="Attach"
            target={target}
            title={`Attach ${target?.title || target?.clusterElementName} tool`}
            workspaceId={workspaceId}
        />
    );
};

export default ChatToolDialog;
