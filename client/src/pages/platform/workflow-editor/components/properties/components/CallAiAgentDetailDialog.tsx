import {Dialog, DialogBody, DialogContent, DialogHeader, DialogMain} from '@/components/Dialog';
import AgentDetailContent from '@/pages/automation/agents/AgentDetailContent';
import getAgentPath from '@/pages/automation/agents/utils/getAgentPath';
import invalidateAgentQueries from '@/pages/automation/agents/utils/invalidateAgentQueries';
import {useAiAgentQuery} from '@/shared/middleware/graphql';
import {WorkflowNodeOptionKeys} from '@/shared/queries/platform/workflowNodeOptions.queries';
import {useQueryClient} from '@tanstack/react-query';
import {ExternalLinkIcon} from 'lucide-react';
import {Link} from 'react-router-dom';

export interface CallAiAgentDetailDialogPropsI {
    agentId: string;
    agentTitle: string;
    onOpenChange: (open: boolean) => void;
    open: boolean;
}

const CallAiAgentDetailDialog = ({agentId, agentTitle, onOpenChange, open}: CallAiAgentDetailDialogPropsI) => {
    const queryClient = useQueryClient();

    const {data} = useAiAgentQuery({id: agentId}, {enabled: !!agentId});

    const agent = data?.aiAgent;

    const handleOpenChange = (nextOpen: boolean) => {
        if (!nextOpen) {
            queryClient.invalidateQueries({queryKey: WorkflowNodeOptionKeys.workflowNodeOptions});

            invalidateAgentQueries(queryClient);
        }

        onOpenChange(nextOpen);
    };

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            <DialogContent className="h-[85vh]" size="xl">
                <DialogMain>
                    <DialogHeader
                        endContent={
                            agent && (
                                <Link
                                    className="flex shrink-0 items-center gap-1 text-xs text-content-brand-primary hover:underline"
                                    rel="noreferrer"
                                    target="_blank"
                                    to={getAgentPath(agent)}
                                >
                                    Open in full view
                                    <ExternalLinkIcon className="size-3" />
                                </Link>
                            )
                        }
                        title={agentTitle}
                    />

                    <DialogBody>
                        <AgentDetailContent agentId={agentId} className="max-w-none p-0" />
                    </DialogBody>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default CallAiAgentDetailDialog;
