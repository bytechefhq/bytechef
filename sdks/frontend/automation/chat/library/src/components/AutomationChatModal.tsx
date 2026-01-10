import {type FC, forwardRef, type ComponentPropsWithoutRef} from 'react';
import {BotIcon, ChevronDownIcon} from 'lucide-react';
import {AssistantModalPrimitive} from '@assistant-ui/react';
import {Thread} from './assistant-ui/thread';
import {TooltipIconButton} from './assistant-ui/tooltip-icon-button';
import type {AutomationChatModalConfig} from '@/types';
import {AutomationChatProvider} from './AutomationChatProvider';

export interface AutomationChatModalProps {
    /**
     * Configuration for the chat modal
     */
    config: AutomationChatModalConfig;

    /**
     * Position of the modal trigger button
     * @default 'bottom-right'
     */
    position?: 'bottom-right' | 'bottom-left' | 'top-right' | 'top-left';
}

type AssistantModalButtonProps = ComponentPropsWithoutRef<typeof AssistantModalPrimitive.Trigger> & {
    'data-state'?: 'open' | 'closed';
};

const AssistantModalButton = forwardRef<HTMLButtonElement, AssistantModalButtonProps>(
    ({'data-state': state, ...rest}, ref) => {
        const tooltip = state === 'open' ? 'Close Assistant' : 'Open Assistant';

        return (
            <TooltipIconButton
                variant="default"
                tooltip={tooltip}
                side="left"
                {...rest}
                className="aui-modal-button size-full rounded-full shadow transition-transform hover:scale-110 active:scale-90"
                ref={ref}
            >
                <BotIcon
                    data-state={state}
                    className="aui-modal-button-closed-icon absolute size-6 transition-all data-[state=closed]:rotate-0 data-[state=open]:rotate-90 data-[state=closed]:scale-100 data-[state=open]:scale-0"
                />

                <ChevronDownIcon
                    data-state={state}
                    className="aui-modal-button-open-icon absolute size-6 transition-all data-[state=closed]:-rotate-90 data-[state=open]:rotate-0 data-[state=closed]:scale-0 data-[state=open]:scale-100"
                />
                <span className="sr-only">{tooltip}</span>
            </TooltipIconButton>
        );
    }
);

AssistantModalButton.displayName = 'AssistantModalButton';

/**
 * AutomationChatModal - Modal chat widget
 *
 * @example
 * ```tsx
 * <AutomationChatModal
 *   config={{
 *     webhookUrl: 'https://your-bytechef-instance.com/webhooks/your-webhook-id/sse',
 *     title: 'Chat with us',
 *   }}
 *   position="bottom-right"
 * />
 * ```
 */
export const AutomationChatModal: FC<AutomationChatModalProps> = ({config, position = 'bottom-right'}) => {
    const positionClasses = {
        'bottom-right': 'right-4 bottom-4',
        'bottom-left': 'left-4 bottom-4',
        'top-right': 'right-4 top-4',
        'top-left': 'left-4 top-4',
    };

    return (
        <AutomationChatProvider config={config}>
            <AssistantModalPrimitive.Root>
                <AssistantModalPrimitive.Anchor
                    className={`aui-root aui-modal-anchor fixed size-11 ${positionClasses[position]}`}
                >
                    <AssistantModalPrimitive.Trigger asChild>
                        <AssistantModalButton />
                    </AssistantModalPrimitive.Trigger>
                </AssistantModalPrimitive.Anchor>
                <AssistantModalPrimitive.Content
                    sideOffset={16}
                    className="aui-root aui-modal-content data-[state=closed]:fade-out-0 data-[state=closed]:slide-out-to-bottom-1/2 data-[state=closed]:slide-out-to-right-1/2 data-[state=closed]:zoom-out data-[state=open]:fade-in-0 data-[state=open]:slide-in-from-bottom-1/2 data-[state=open]:slide-in-from-right-1/2 data-[state=open]:zoom-in z-50 h-[500px] w-[400px] overflow-clip overscroll-contain rounded-xl border bg-popover p-0 text-popover-foreground shadow-md outline-none data-[state=closed]:animate-out data-[state=open]:animate-in [&>.aui-thread-root]:bg-inherit"
                >
                    <Thread />
                </AssistantModalPrimitive.Content>
            </AssistantModalPrimitive.Root>
        </AutomationChatProvider>
    );
};
