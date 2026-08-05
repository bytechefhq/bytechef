import ToolConfigDialog, {
    ToolConfigDialogTargetI,
    ToolConfigDialogValuesI,
} from '@/ee/pages/automation/ai-hub/tools/dialogs/ToolConfigDialog';
import useTaskToolsCache from '@/ee/pages/automation/ai-hub/tools/hooks/useTaskToolsCache';
import {AttachAiHubTaskToolInput, useAttachAiHubTaskToolMutation} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';

// Re-export the shared shape under the task-flavoured name so existing consumers
// (AiHubComposer) keep their import path while sharing the underlying interface with the
// generalised dialog. Re-exporting via `export type` rather than a fresh interface because adding new
// fields here would silently diverge from the generalised dialog's expectations.
export type {ToolConfigDialogTargetI as TaskToolDialogTargetI};

interface TaskToolDialogProps {
    taskId: string;
    onClose: () => void;
    open: boolean;
    target: ToolConfigDialogTargetI | null;
    workspaceId: number;
}

/**
 * Thin wrapper around {@link ToolConfigDialog} that wires the attach-task-tool mutation. Mirrors the
 * chat-driven {@code AttachTaskToolToolCallback} affordance — same persistence shape, same idempotency
 * semantics — so the LLM and the user click into the same {@code ai_hub_task_tool} row.
 *
 * <p>
 * Submit fires {@code attachAiHubTaskTool} with the dialog's sanitized values. Success invalidates the
 * task-tools cache so the in-chat resource panel reflects the new attachment without a manual refresh.
 * The dialog itself owns the connection picker, properties form, sanitization, and create-new-connection
 * affordance — see {@link ToolConfigDialog}.
 * </p>
 */
const TaskToolDialog = ({onClose, open, target, taskId, workspaceId}: TaskToolDialogProps) => {
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {invalidate} = useTaskToolsCache();

    const attachMutation = useAttachAiHubTaskToolMutation({
        onSuccess: () => {
            invalidate(taskId, workspaceId);

            onClose();
        },
    });

    const handleSubmit = async (values: ToolConfigDialogValuesI) => {
        if (target == null) {
            return;
        }

        const input: AttachAiHubTaskToolInput = {
            clusterElementName: target.clusterElementName,
            componentName: target.componentName,
            componentVersion: target.componentVersion,
            connectionId: values.connectionId,
            environment: environmentId ?? 1,
            // Cast through unknown to satisfy the Any-scalar codegen output. Server treats as Map<String, ?>.
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            parameters: values.parameters as any,
            taskId,
            workspaceId: String(workspaceId),
        };

        await attachMutation.mutateAsync({input});
    };

    return (
        <ToolConfigDialog
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

export default TaskToolDialog;
