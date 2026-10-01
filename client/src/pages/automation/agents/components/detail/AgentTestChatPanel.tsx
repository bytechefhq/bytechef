import Button from '@/components/Button/Button';
import {Thread} from '@/components/assistant-ui/thread';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {AgentTestChatRuntimeProvider} from '@/pages/automation/agents/components/detail/runtime-providers/AgentTestChatRuntimeProvider';
import {aiChatDataComponents} from '@/shared/components/ai-chat/messages/aiChatDataComponents';
import {type SuggestionConfig} from '@assistant-ui/react';
import {BotIcon, MessageSquareXIcon, XIcon} from 'lucide-react';
import {useState} from 'react';

const AgentTestChatWelcome = () => (
    <div className="mb-6 flex flex-col px-4">
        <h1 className="text-2xl font-semibold">Test your agent</h1>

        <p className="text-base text-muted-foreground/65">Send a message to start a conversation</p>
    </div>
);

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

const AgentTestChatPanel = ({onClose, workflowId}: AgentTestChatPanelProps) => {
    const [conversationKey, setConversationKey] = useState(0);

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
