import {FC} from 'react';
import {AutomationChatProvider} from './AutomationChatProvider';
import {Thread} from './assistant-ui/thread';
import {useAutomationChatConfig} from '@/hooks/useAutomationChatConfig';
import {VoiceModeLayout} from '@/lib/VoiceModeLayout';
import type {AutomationChatConfig} from '@/types';

export interface AutomationChatProps {
    /**
     * Configuration for the chat widget
     */
    config: AutomationChatConfig;

    /**
     * Custom CSS class for the chat container
     */
    className?: string;

    /**
     * Optional header component
     */
    header?: React.ReactNode;
}

/**
 * Inner content component — reads voiceMode/chatMode from context set by AutomationChatProvider.
 * Voice-only widgets (voiceMode=true, chatMode=false) get the full-screen VoiceModeLayout.
 * Everything else falls through to Thread, which exposes an inline voice button in its composer
 * when voice is enabled.
 */
const AutomationChatContent: FC = () => {
    const {chatMode, voiceMode} = useAutomationChatConfig();

    if (voiceMode && !chatMode) {
        return <VoiceModeLayout sessionLimitSeconds={150} />;
    }

    return <Thread />;
};

/**
 * AutomationChat - Embeddable chat widget
 *
 * @example
 * ```tsx
 * <AutomationChat
 *   config={{
 *     webhookUrl: 'https://your-bytechef-instance.com/webhooks/your-webhook-id/sse',
 *   }}
 * />
 * ```
 *
 * @example Voice-only workflow (full-screen takeover)
 * ```tsx
 * <AutomationChat
 *   config={{
 *     voiceMode: true,
 *     webhookUrl: 'https://your-bytechef-instance.com/webhooks/your-webhook-id',
 *   }}
 * />
 * ```
 *
 * @example Chat + inline voice (button in the composer)
 * ```tsx
 * <AutomationChat
 *   config={{
 *     chatMode: true,
 *     voiceMode: true,
 *     voiceWebhookUrl: 'https://your-bytechef-instance.com/webhooks/<voice-webhook-id>',
 *     webhookUrl: 'https://your-bytechef-instance.com/webhooks/<chat-webhook-id>/sse',
 *   }}
 * />
 * ```
 */
export const AutomationChat: FC<AutomationChatProps> = ({config, className = '', header}) => {
    return (
        <div className={`bytechef-chat-container flex h-full flex-col ${className}`}>
            {header && <div className="bytechef-chat-header">{header}</div>}
            <AutomationChatProvider config={config}>
                <div className="flex w-full flex-1 flex-col">
                    <AutomationChatContent />
                </div>
            </AutomationChatProvider>
        </div>
    );
};
