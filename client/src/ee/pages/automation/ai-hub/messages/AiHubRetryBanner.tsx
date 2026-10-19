import Button from '@/components/Button/Button';
import {useAiChatRetryableErrorStore} from '@/shared/components/ai-chat/stores/useAiChatRetryableErrorStore';
import {useAui} from '@assistant-ui/react';
import {AlertTriangleIcon} from 'lucide-react';
import {useShallow} from 'zustand/react/shallow';

/**
 * Shows why the user's last message got no answer, with a Retry that sends it again and a Dismiss. The runtime provider
 * raises it for a tool failure the run ended with and never recovered from, for a turn request that failed (such as an
 * HTTP 500), and for a chat that could not be created; each of these is reported only here, not in a toast as well. A
 * run error, which shows in the chat, or a Stop raises nothing. Must render inside the runtime provider.
 */
const AiHubRetryBanner = () => {
    const {clearError, currentError} = useAiChatRetryableErrorStore(
        useShallow((state) => ({
            clearError: state.clearError,
            currentError: state.currentError,
        }))
    );

    const aui = useAui();

    if (!currentError) {
        return null;
    }

    const handleRetryClick = () => {
        clearError();

        aui.thread.append({
            content: [{text: currentError.lastUserMessage, type: 'text'}],
            role: 'user',
        });
    };

    return (
        <div className="mx-auto w-full max-w-[var(--thread-max-width)] px-4">
            <div
                className="my-2 flex items-start gap-2 rounded-md border-l-4 border-red-500 bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-950/40 dark:text-red-300"
                data-testid="ai-hub-retry-banner"
                role="alert"
            >
                <AlertTriangleIcon className="mt-0.5 size-4 shrink-0" />

                <div className="flex min-w-0 flex-1 flex-col gap-2">
                    <span className="break-words">{`${currentError.toolName} failed: ${currentError.errorMessage}`}</span>

                    <div className="flex items-center gap-2">
                        {currentError.lastUserMessage && (
                            <Button label="Retry" onClick={handleRetryClick} size="xs" variant="default" />
                        )}

                        <Button label="Dismiss" onClick={clearError} size="xs" variant="ghost" />
                    </div>
                </div>
            </div>
        </div>
    );
};

export default AiHubRetryBanner;
