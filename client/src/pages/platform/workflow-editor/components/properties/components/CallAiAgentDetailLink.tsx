import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import CallAiAgentDetailDialog from '@/pages/platform/workflow-editor/components/properties/components/CallAiAgentDetailDialog';
import {useAiAgentsQuery} from '@/shared/middleware/graphql';
import {ExternalLinkIcon} from 'lucide-react';
import {useState} from 'react';

export interface CallAiAgentDetailLinkPropsI {
    agentUuid: string | undefined;
}

const CallAiAgentDetailLink = ({agentUuid}: CallAiAgentDetailLinkPropsI) => {
    const [dialogOpen, setDialogOpen] = useState(false);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data} = useAiAgentsQuery(
        {workspaceId: String(currentWorkspaceId)},
        {enabled: !!currentWorkspaceId && !!agentUuid}
    );

    const agent = data?.aiAgents?.find((aiAgent) => aiAgent?.uuid === agentUuid);

    if (!agent) {
        return null;
    }

    return (
        <>
            <button
                className="flex items-center gap-1 self-start text-xs text-content-brand-primary hover:underline"
                onClick={() => setDialogOpen(true)}
                type="button"
            >
                View agent details
                <ExternalLinkIcon className="size-3" />
            </button>

            {dialogOpen && (
                <CallAiAgentDetailDialog
                    agentId={agent.id}
                    agentTitle={agent.title}
                    onOpenChange={setDialogOpen}
                    open={dialogOpen}
                />
            )}
        </>
    );
};

export default CallAiAgentDetailLink;
