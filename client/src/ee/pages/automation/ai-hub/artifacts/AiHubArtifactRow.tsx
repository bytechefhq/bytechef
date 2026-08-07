import {getArtifactIcon} from '@/ee/pages/automation/ai-hub/artifacts/artifactIcons';
import {
    handleArtifactQuickOpen,
    isArtifactClickable,
    isArtifactRemovable,
} from '@/ee/pages/automation/ai-hub/artifacts/artifactOpen';
import {AiHubTaskArtifactI} from '@/ee/pages/automation/ai-hub/tasks/api/tasks.api';
import {useDeleteAiHubTaskArtifactMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {XIcon} from 'lucide-react';
import {type MouseEvent} from 'react';
import {toast} from 'sonner';

interface AiHubArtifactRowPropsI {
    artifact: AiHubTaskArtifactI;
    workspaceId: number;
}

const AiHubArtifactRow = ({artifact, workspaceId}: AiHubArtifactRowPropsI) => {
    const queryClient = useQueryClient();

    const clickable = isArtifactClickable(artifact);
    const removable = isArtifactRemovable(artifact);

    const deleteMutation = useDeleteAiHubTaskArtifactMutation({
        onError: (error) => {
            const message = error instanceof Error ? error.message : String(error);

            toast.error(`Failed to remove ${artifact.artifactName}: ${message}`);
        },
        // The artifact list query is keyed by ['aiHubTasks', 'artifacts', taskId, workspaceId] (see
        // AiHubTasksKeys.artifacts) — invalidate by that prefix so the row disappears without needing the
        // exact key shape react-query built it under. Matching on both segments avoids needlessly
        // refetching the task list/messages, which share the 'aiHubTasks' root. Mirrors
        // useRecordReferencedArtifacts' invalidation strategy.
        onSuccess: () => {
            queryClient.invalidateQueries({
                predicate: (query) => {
                    const key = query.queryKey;

                    if (!Array.isArray(key) || key.length < 2) {
                        return false;
                    }

                    return key[0] === 'aiHubTasks' && key[1] === 'artifacts';
                },
            });
        },
    });

    const handleRemove = (event: MouseEvent<HTMLButtonElement>) => {
        // The remove button sits inside the quick-open button when the row is clickable; stop the click
        // from bubbling so removing doesn't also open the artifact.
        event.stopPropagation();

        deleteMutation.mutate({
            input: {
                artifactId: String(artifact.id),
                workspaceId: String(workspaceId),
            },
        });
    };

    const content = (
        <div className="flex min-w-0 flex-1 items-center gap-1.5">
            {getArtifactIcon(artifact.kind)}

            <span className="min-w-0 flex-1 truncate text-xs text-foreground">{artifact.artifactName}</span>

            {removable && (
                <button
                    aria-label={`Remove ${artifact.artifactName}`}
                    className="shrink-0 rounded p-0.5 text-muted-foreground opacity-0 group-hover:opacity-100 hover:bg-muted hover:text-foreground"
                    disabled={deleteMutation.isPending}
                    onClick={handleRemove}
                    type="button"
                >
                    <XIcon className="size-3" />
                </button>
            )}
        </div>
    );

    if (clickable) {
        return (
            <button
                className="group flex w-full items-center gap-1.5 rounded px-1.5 py-1 text-left hover:bg-accent"
                onClick={() => void handleArtifactQuickOpen(artifact)}
                type="button"
            >
                {content}
            </button>
        );
    }

    return <div className="group flex items-center gap-1.5 px-1.5 py-1">{content}</div>;
};

export default AiHubArtifactRow;
