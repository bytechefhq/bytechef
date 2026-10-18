import AiHubArtifactRow from '@/ee/pages/automation/ai-hub/artifacts/AiHubArtifactRow';
import useAiHubArtifactsCard from '@/ee/pages/automation/ai-hub/artifacts/useAiHubArtifactsCard';
import useAiHubSettingsStore from '@/ee/pages/automation/ai-hub/stores/useAiHubSettingsStore';
import {ChevronDownIcon, ChevronUpIcon} from 'lucide-react';

const AiHubArtifactsCard = () => {
    const artifactsCardCollapsed = useAiHubSettingsStore((state) => state.artifactsCardCollapsed);
    const setArtifactsCardCollapsed = useAiHubSettingsStore((state) => state.setArtifactsCardCollapsed);

    const {artifacts, visible, workspaceId} = useAiHubArtifactsCard();

    if (!visible) {
        return null;
    }

    return (
        <div className="absolute top-14 right-3 z-10 w-64 overflow-hidden rounded-xl border bg-surface-neutral-primary">
            <button
                aria-expanded={!artifactsCardCollapsed}
                className="flex w-full items-center gap-2 px-3 py-2 hover:bg-accent"
                onClick={() => setArtifactsCardCollapsed(!artifactsCardCollapsed)}
                type="button"
            >
                <span className="flex-1 text-left text-xs font-medium text-content-neutral-secondary">Artifacts</span>

                <span className="rounded bg-muted px-1 py-0.5 text-xs font-medium text-muted-foreground">
                    {artifacts.length}
                </span>

                {artifactsCardCollapsed ? (
                    <ChevronDownIcon className="size-3.5 text-muted-foreground" />
                ) : (
                    <ChevronUpIcon className="size-3.5 text-muted-foreground" />
                )}
            </button>

            {!artifactsCardCollapsed && (
                <div className="flex max-h-64 flex-col overflow-y-auto px-1.5 pb-1.5">
                    {artifacts.map((artifact) => (
                        <AiHubArtifactRow artifact={artifact} key={artifact.id} workspaceId={workspaceId} />
                    ))}
                </div>
            )}
        </div>
    );
};

export default AiHubArtifactsCard;
