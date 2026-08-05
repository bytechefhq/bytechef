import Badge from '@/components/Badge/Badge';
import useTaskToolsCache from '@/ee/pages/automation/ai-hub/tools/hooks/useTaskToolsCache';
import {useAiHubTaskToolsQuery, useRemoveAiHubTaskToolMutation} from '@/shared/middleware/graphql';
import {WrenchIcon, XIcon} from 'lucide-react';

interface TaskToolChipsPropsI {
    taskId: string;
    workspaceId: number;
}

/**
 * Renders one chip per tool currently attached to the task. Sibling to the existing referenced-
 * resources chip list (files/workflows/data tables/knowledge bases) but sourced from the persisted
 * {@code ai_hub_task_tool} table — so tools added by the LLM via {@code AttachTaskToolToolCallback}
 * appear immediately on next render, and tools removed via the X button also remove the LLM's binding.
 *
 * <p>Returns null when the task has no attached tools so the parent's "hasChips" check stays
 * truthy only when there's something to show — no empty container takes up flex space.</p>
 */
const TaskToolChips = ({taskId, workspaceId}: TaskToolChipsPropsI) => {
    const {invalidate} = useTaskToolsCache();

    const {data} = useAiHubTaskToolsQuery({
        taskId,
        workspaceId: String(workspaceId),
    });

    const removeMutation = useRemoveAiHubTaskToolMutation({
        onSuccess: () => invalidate(taskId, workspaceId),
    });

    const tools = data?.aiHubTaskTools ?? [];

    if (tools.length === 0) {
        return null;
    }

    // Owns its own flex-wrap row so the parent can render this component unconditionally without
    // needing to gate on tool count — the chip list renders if and only if there are tools, and
    // sits cleanly below the referenced-resources row when both lists have content.
    return (
        <div className="flex flex-wrap gap-1.5 px-3 pt-2" data-testid="task-tool-chips">
            {tools.map((tool) => (
                <Badge
                    className="flex items-center gap-1 bg-surface-warning-secondary px-2 py-0.5 text-xs font-normal text-content-warning-primary"
                    data-testid="task-tool-chip"
                    key={`tool-${tool.taskToolId}`}
                    styleType="outline-outline"
                >
                    <WrenchIcon className="size-3" />

                    <span className="font-medium">{tool.componentName}:</span>

                    <span>{tool.clusterElementName}</span>

                    <button
                        aria-label={`Remove ${tool.componentName} ${tool.clusterElementName}`}
                        className="ml-0.5 rounded hover:opacity-75"
                        // Optimistic-feeling UX: the cache invalidation in the mutation's onSuccess refetches
                        // the list, but the user gets immediate dismissal feedback because react-query's
                        // pending-mutation state pairs with the disabled prop below.
                        disabled={removeMutation.isPending}
                        onClick={() =>
                            removeMutation.mutate({
                                taskToolId: tool.taskToolId,
                                workspaceId: String(workspaceId),
                            })
                        }
                        type="button"
                    >
                        <XIcon className="size-3" />
                    </button>
                </Badge>
            ))}
        </div>
    );
};

export default TaskToolChips;
