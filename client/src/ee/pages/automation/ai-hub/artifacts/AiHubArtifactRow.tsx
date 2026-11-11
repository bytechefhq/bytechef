import {getArtifactIcon} from '@/ee/pages/automation/ai-hub/artifacts/artifactIcons';
import {
    handleArtifactQuickOpen,
    isArtifactClickable,
    isArtifactRemovable,
} from '@/ee/pages/automation/ai-hub/artifacts/artifactOpen';
import {AiHubChatArtifactI} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {AiHubChatsKeys} from '@/ee/pages/automation/ai-hub/chats/hooks/useChats';
import {useDeleteAiHubChatArtifactMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {XIcon} from 'lucide-react';
import {toast} from 'sonner';
import {twMerge} from 'tailwind-merge';

interface AiHubArtifactRowPropsI {
    artifact: AiHubChatArtifactI;
    workspaceId: number;
}

const AiHubArtifactRow = ({artifact, workspaceId}: AiHubArtifactRowPropsI) => {
    const queryClient = useQueryClient();

    const deleteMutation = useDeleteAiHubChatArtifactMutation({
        onError: (error) => {
            const message = error instanceof Error ? error.message : String(error);

            toast.error(`Failed to remove ${artifact.artifactName}: ${message}`);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: [...AiHubChatsKeys.all, 'artifacts']});
        },
    });

    const clickable = isArtifactClickable(artifact);
    const removable = isArtifactRemovable(artifact);

    const handleRemove = () => {
        deleteMutation.mutate({
            input: {
                artifactId: String(artifact.id),
                workspaceId: String(workspaceId),
            },
        });
    };

    const label = (
        <>
            {getArtifactIcon(artifact.kind)}

            <span className="min-w-0 flex-1 truncate text-xs text-foreground">{artifact.artifactName}</span>
        </>
    );

    return (
        <div className={twMerge('group flex items-center gap-1.5 rounded px-1.5 py-1', clickable && 'hover:bg-accent')}>
            {clickable ? (
                <button
                    className="flex min-w-0 flex-1 items-center gap-1.5 text-left"
                    onClick={() => handleArtifactQuickOpen(artifact)}
                    type="button"
                >
                    {label}
                </button>
            ) : (
                <div className="flex min-w-0 flex-1 items-center gap-1.5">{label}</div>
            )}

            {removable && (
                <button
                    aria-label={`Remove ${artifact.artifactName}`}
                    className="shrink-0 rounded p-0.5 text-muted-foreground opacity-0 group-hover:opacity-100 hover:bg-muted hover:text-foreground focus-visible:opacity-100"
                    disabled={deleteMutation.isPending}
                    onClick={handleRemove}
                    type="button"
                >
                    <XIcon className="size-3" />
                </button>
            )}
        </div>
    );
};

export default AiHubArtifactRow;
