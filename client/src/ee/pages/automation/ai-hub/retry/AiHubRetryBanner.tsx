import Button from '@/components/Button/Button';
import {useAiChatRetryableErrorStore} from '@/shared/components/ai-chat/stores/useAiChatRetryableErrorStore';
import {useAui} from '@assistant-ui/react';
import {AlertTriangleIcon} from 'lucide-react';
import {useShallow} from 'zustand/react/shallow';

const AiHubRetryBanner = () => {
    const {clearError, currentError} = useAiChatRetryableErrorStore(
        useShallow((state) => ({
            clearError: state.clearError,
            currentError: state.currentError,
        }))
    );

    // useAui requires this component to be rendered inside AssistantRuntimeProvider.
    const aui = useAui();

    if (!currentError) {
        return null;
    }

    const handleRetry = () => {
        clearError();

        aui.thread.append({
            content: [{text: currentError.lastUserMessage, type: 'text'}],
            role: 'user',
        });
    };

    const handleDismiss = () => {
        clearError();
    };

    return (
        <div className="border-stroke-error-subtle bg-surface-error-secondary flex items-start gap-2 border-b px-3 py-2">
            <AlertTriangleIcon className="mt-0.5 size-4 shrink-0 text-content-destructive" />

            <div className="flex min-w-0 flex-1 flex-col gap-1">
                <span className="text-xs font-medium text-content-destructive">
                    {`"${currentError.toolName}" failed: ${currentError.errorMessage}`}
                </span>

                <div className="flex items-center gap-2">
                    <Button label="Retry" onClick={handleRetry} size="xs" variant="default" />

                    <Button label="Dismiss" onClick={handleDismiss} size="xs" variant="ghost" />
                </div>
            </div>
        </div>
    );
};

export default AiHubRetryBanner;
