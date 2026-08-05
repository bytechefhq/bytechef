import {useAiHubProgressStore} from '@/ee/pages/automation/ai-hub/progress/stores/useAiHubProgressStore';
import {Loader2Icon} from 'lucide-react';

/**
 * Renders a dimmed italic status line while a subagent tool call is in flight.
 * The line appears below the Thread and auto-hides once progressText is null.
 */
const SubagentProgressLine = () => {
    const progressText = useAiHubProgressStore((state) => state.progressText);

    if (!progressText) {
        return null;
    }

    return (
        <div className="flex items-center gap-1.5 px-4 py-1.5 text-xs text-muted-foreground/70">
            <Loader2Icon className="size-3 animate-spin" />

            <span className="italic">{progressText}</span>
        </div>
    );
};

export default SubagentProgressLine;
