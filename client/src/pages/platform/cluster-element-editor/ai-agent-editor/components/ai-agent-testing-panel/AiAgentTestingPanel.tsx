import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AiAgentTestRuntimeProvider from '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-testing-panel/runtime-providers/AiAgentTestRuntimeProvider';
import {MessageSquareOffIcon, PlayIcon, SparklesIcon, WorkflowIcon, XIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

import {Thread} from './AiAgentTestingPanelThread';
import useAiAgentTestingPanel from './hooks/useAiAgentTestingPanel';

interface AiAgentTestingPanelProps {
    contentClassName?: string;
    copilotEnabled?: boolean;
    headerClassName?: string;
    onClose?: () => void;
    onCopilotClick?: () => void;
}

export default function AiAgentTestingPanel({
    contentClassName,
    copilotEnabled,
    headerClassName,
    onClose,
    onCopilotClick,
}: AiAgentTestingPanelProps) {
    const {conversationId, handleReset, handleTestAgent, isTestingAgent} = useAiAgentTestingPanel();

    if (!isTestingAgent) {
        return (
            <div className="flex size-full flex-col gap-4">
                <div className={headerClassName}>
                    <h2 className="font-medium">Agent Playbook</h2>
                </div>

                <div className="flex h-full flex-col items-center justify-center rounded-lg bg-muted/50">
                    <div className="flex flex-col items-center gap-4">
                        <div className="relative text-muted-foreground/40">
                            <WorkflowIcon className="size-12" strokeWidth={1.5} />

                            <SparklesIcon className="absolute -right-1 -top-1 size-5" strokeWidth={1.5} />
                        </div>

                        <div className="flex flex-col items-center gap-1.5">
                            <h3 className="text-base font-medium">Test this Agent</h3>

                            <p className="text-center text-sm text-muted-foreground">
                                Test your agent to make sure it&apos;s running properly. <br />
                                Send sample messages that imitate real user messages to the agent and see the results.
                            </p>
                        </div>

                        <Button icon={<PlayIcon />} label="Test Agent" onClick={handleTestAgent} variant="outline" />
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="flex h-full flex-col gap-4">
            <div className={twMerge('flex items-center justify-between', headerClassName)}>
                <h2 className="font-medium">Agent Playbook</h2>

                <div className="flex items-center gap-2">
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Button
                                icon={<MessageSquareOffIcon />}
                                onClick={handleReset}
                                size="iconSm"
                                variant="ghost"
                            />
                        </TooltipTrigger>

                        <TooltipContent>Reset conversation</TooltipContent>
                    </Tooltip>

                    {copilotEnabled && onCopilotClick && (
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button
                                    icon={<SparklesIcon />}
                                    onClick={onCopilotClick}
                                    size="iconSm"
                                    variant="ghost"
                                />
                            </TooltipTrigger>

                            <TooltipContent>Open Copilot panel</TooltipContent>
                        </Tooltip>
                    )}

                    {onClose && <Button icon={<XIcon />} onClick={onClose} size="iconSm" variant="ghost" />}
                </div>
            </div>

            <div className={twMerge('flex-1 overflow-hidden rounded-lg bg-muted/50', contentClassName)}>
                <AiAgentTestRuntimeProvider key={conversationId}>
                    <Thread />
                </AiAgentTestRuntimeProvider>
            </div>
        </div>
    );
}
