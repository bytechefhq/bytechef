import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import AgentVisibilityCaveat from '@/pages/automation/agents/components/AgentVisibilityCaveat';
import ResourceVisibilityPicker, {
    ResourceVisibilityValueType,
} from '@/shared/components/visibility/ResourceVisibilityPicker';
import {useAiAgentVisibility} from '@/shared/hooks/useAiAgentVisibility';

interface AgentVisibilityDialogProps {
    agentId: string;
    onClose: () => void;
    visibility?: ResourceVisibilityValueType;
}

/**
 * The agent counterpart of ProjectVisibilityDialog, wired to the agent-keyed sharing operations — which reach the
 * project ones underneath, because an agent's reach IS its hidden backing project's.
 *
 * The wording is deliberately narrower than the project dialog's. "Visibility" on an agent is easy to read as
 * "who may talk to it", and it is not that: no runtime path in the server consults visibility, so a PRIVATE agent
 * keeps answering Slack, WhatsApp, its webhooks and the in-app chat exactly as before. Every string here says
 * SEE rather than use, and {@link AgentVisibilityCaveat} states the consequence outright instead of leaving the
 * user to discover it. The caveat is a shared component, not a string in this file, because the agent list item's
 * badge dropdown is the other place a user can set this and must say the same thing.
 */
const AgentVisibilityDialog = ({agentId, onClose, visibility}: AgentVisibilityDialogProps) => {
    const agentVisibility = useAiAgentVisibility({agentId, visibility});

    return (
        <Dialog onOpenChange={onClose} open={true}>
            <DialogContent>
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>Who Can See This Agent</DialogTitle>

                        <DialogDescription>
                            Decide who in the workspace finds this agent in their agent and deployment lists, and can
                            open it.
                        </DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                {agentVisibility.enabled && (
                    <>
                        <ResourceVisibilityPicker
                            grantedUserIds={agentVisibility.grantedUserIds}
                            onGrantedUserIdsChange={agentVisibility.onGrantedUserIdsChange}
                            onVisibilityChange={agentVisibility.onVisibilityChange}
                            visibility={visibility || 'WORKSPACE'}
                            workspaceMembers={agentVisibility.workspaceMembers}
                        />

                        <AgentVisibilityCaveat />
                    </>
                )}
            </DialogContent>
        </Dialog>
    );
};

export default AgentVisibilityDialog;
