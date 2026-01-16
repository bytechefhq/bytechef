import {FC} from 'react';
import {AutomationChatProvider} from './AutomationChatProvider';
import {Thread} from './assistant-ui/thread';
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
 */
export const AutomationChat: FC<AutomationChatProps> = ({config, className = '', header}) => {
    return (
        <div className={`bytechef-chat-container flex flex-1 flex-col ${className}`}>
            {header && <div className="bytechef-chat-header">{header}</div>}
            <AutomationChatProvider config={config}>
                <div className="flex size-full flex-col">
                    <Thread />
                </div>
            </AutomationChatProvider>
        </div>
    );
};
