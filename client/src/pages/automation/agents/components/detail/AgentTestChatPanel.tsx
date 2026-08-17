import Button from '@/components/Button/Button';
import {Thread} from '@/components/assistant-ui/thread';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {AgentTestChatRuntimeProvider} from '@/pages/automation/agents/components/detail/runtime-providers/AgentTestChatRuntimeProvider';
import {aiChatDataComponents} from '@/shared/components/ai-chat/messages/aiChatDataComponents';
import {type SuggestionConfig} from '@assistant-ui/react';
import {BotIcon, MessageSquareXIcon, XIcon} from 'lucide-react';
import {useState} from 'react';

/**
 * Replaces the shared Thread's generic "How can I help you today?" greeting, which is written for the copilot
 * and the Chat page. This panel is a test harness, so it says what it is and what to do — matching the simple
 * editor's own testing panel wording. Supplied through Thread's `components.Welcome` slot rather than by
 * changing the shared greeting, which the copilot and Chat page still want.
 */
const AgentTestChatWelcome = () => (
    <div className="mb-6 flex flex-col px-4">
        <h1 className="text-2xl font-semibold">Test your agent</h1>

        {/* text-base, not the heading's text-2xl: at 24px this wraps to two lines in the panel's width. */}

        <p className="text-base text-muted-foreground/65">Send a message to start a conversation</p>
    </div>
);

// Deliberately generic: the panel has no idea what this particular agent does, so the starters ask the agent
// to introduce itself rather than pretending to know its domain.
const AGENT_TEST_SUGGESTIONS: SuggestionConfig[] = [
    {
        label: 'help me with?',
        prompt: 'What can you help me with?',
        title: 'What can you',
    },
    {
        label: 'do you have access to?',
        prompt: 'What tools do you have access to?',
        title: 'What tools',
    },
    {
        label: 'instructions and capabilities',
        prompt: 'Summarize your instructions and capabilities',
        title: 'Summarize your',
    },
    {
        label: 'how you would handle a typical request',
        prompt: 'Walk me through how you would handle a typical request',
        title: 'Walk me through',
    },
];

export interface AgentTestChatPanelProps {
    onClose?: () => void;
    workflowId: string;
}

/**
 * The Agent detail page's draft test-chat panel: a chat surface wired to the agent's UNPUBLISHED draft
 * workflow (`workflowId` is {@code agent.draftWorkflowId}) rather than any deployed/published version. Lets
 * a builder converse with the agent as they iterate, before publishing makes those changes live.
 */
const AgentTestChatPanel = ({onClose, workflowId}: AgentTestChatPanelProps) => {
    const [conversationKey, setConversationKey] = useState(0);

    // The transcript and the conversation id both live in the runtime provider's own state, so remounting it
    // is the reset — there is no separate "clear" the provider exposes, and reusing the conversation id would
    // leave the agent's chat memory holding the discarded turns.
    const handleReset = () => setConversationKey((previousKey) => previousKey + 1);

    return (
        <div className="flex h-full flex-col bg-surface-main">
            <header className="flex items-center justify-between gap-2 px-4 py-3">
                <div className="flex items-center space-x-1">
                    <BotIcon className="size-6" /> <h4>Test Agent</h4>
                </div>

                <div className="flex items-center gap-1">
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Button
                                aria-label="Clean messages"
                                icon={<MessageSquareXIcon />}
                                onClick={handleReset}
                                size="icon"
                                variant="ghost"
                            />
                        </TooltipTrigger>

                        <TooltipContent>Clean messages</TooltipContent>
                    </Tooltip>

                    {onClose && (
                        <Button
                            aria-label="Close test panel"
                            icon={<XIcon />}
                            onClick={onClose}
                            size="icon"
                            variant="ghost"
                        />
                    )}
                </div>
            </header>

            {/* min-h-0 is load-bearing: <Thread> is h-full with its own scroll viewport, and without it the
                flex item grows to its content height instead of scrolling inside the panel. */}

            <div className="min-h-0 flex-1">
                <AgentTestChatRuntimeProvider
                    key={conversationKey}
                    suggestions={AGENT_TEST_SUGGESTIONS}
                    workflowId={workflowId}
                >
                    <Thread components={{Welcome: AgentTestChatWelcome}} dataComponents={aiChatDataComponents} />
                </AgentTestChatRuntimeProvider>
            </div>
        </div>
    );
};

export default AgentTestChatPanel;
