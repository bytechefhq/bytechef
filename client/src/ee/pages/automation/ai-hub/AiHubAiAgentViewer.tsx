import Button from '@/components/Button/Button';
import AgentDetailContent from '@/pages/automation/agents/AgentDetailContent';
import AgentTestChatPanel from '@/pages/automation/agents/components/detail/AgentTestChatPanel';
import getAgentPath from '@/pages/automation/agents/utils/getAgentPath';
import {useAiAgentQuery} from '@/shared/middleware/graphql';
import {ExternalLinkIcon, PlayIcon} from 'lucide-react';
import {useState} from 'react';
import {Link} from 'react-router-dom';

interface AiHubAiAgentViewerPropsI {
    aiAgentId: string;
    name: string;
}

/**
 * An AI Agent tab in the resource panel: the agent's detail cards without the routed page's header and sidebars. The
 * Test toggle opens the agent's test chat, which runs the current draft, as a drawer over the cards so they stay
 * mounted and editable beside it.
 */
const AiHubAiAgentViewer = ({aiAgentId, name}: AiHubAiAgentViewerPropsI) => {
    const [testPanelOpen, setTestPanelOpen] = useState(false);

    const {data} = useAiAgentQuery({id: aiAgentId}, {enabled: !!aiAgentId});

    const agent = data?.aiAgent;

    const testPanelVisible = testPanelOpen && !!agent?.draftWorkflowId;

    return (
        <div className="flex size-full flex-col">
            <header className="flex shrink-0 items-center justify-between gap-2 border-b border-stroke-neutral-secondary px-4 py-3">
                <span className="truncate text-sm font-semibold text-content-neutral-primary">{name}</span>

                {agent && (
                    <div className="flex shrink-0 items-center gap-3">
                        {agent.draftWorkflowId && (
                            <Button
                                aria-label={testPanelVisible ? 'Hide Test Agent panel' : 'Test Agent'}
                                icon={<PlayIcon />}
                                label="Test"
                                onClick={() => setTestPanelOpen((open) => !open)}
                                size="sm"
                                variant={testPanelVisible ? 'secondary' : 'default'}
                            />
                        )}

                        <Link
                            className="flex items-center gap-1 text-xs text-content-brand-primary hover:underline"
                            rel="noreferrer"
                            target="_blank"
                            to={getAgentPath(agent)}
                        >
                            Open in full view
                            <ExternalLinkIcon className="size-3" />
                        </Link>
                    </div>
                )}
            </header>

            <div className="relative min-h-0 flex-1">
                <div className="size-full overflow-y-auto">
                    <AgentDetailContent agentId={aiAgentId} />
                </div>

                {testPanelVisible && agent?.draftWorkflowId && (
                    <div className="absolute inset-y-0 right-0 z-10 m-3 w-[calc(100%-1.5rem)] max-w-[420px] overflow-hidden rounded-lg border border-stroke-neutral-secondary bg-background shadow-lg">
                        <AgentTestChatPanel
                            key={agent.id}
                            onClose={() => setTestPanelOpen(false)}
                            workflowId={agent.draftWorkflowId}
                        />
                    </div>
                )}
            </div>
        </div>
    );
};

export default AiHubAiAgentViewer;
