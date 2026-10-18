import {type RunErrorDataI} from '@/shared/components/ai-chat/messages/RunErrorMessage';
import {humanizeAgentErrorMessage} from '@/shared/components/ai-chat/messages/humanizeAgentErrorMessage';

export const RUN_ERROR_CODE_NO_AI_PROVIDER = 'NO_AI_PROVIDER';

const FALLBACK_MESSAGE = 'The agent run failed before completing this turn.';

const RUN_ERROR_CODE_MESSAGES: Record<string, string> = {
    MODEL_SELECTION_INCOMPLETE: 'Your model selection is incomplete. Pick a model again.',
    [RUN_ERROR_CODE_NO_AI_PROVIDER]:
        'No AI provider is set up for this environment. Enable one in Settings > AI Providers. If you do not have permission, ask an administrator.',
    SELECTED_MODEL_UNAVAILABLE: 'The model you picked is no longer available. Pick another model in the model picker.',
};

/**
 * Turns an AG-UI RUN_ERROR into the data the run-error message part renders. A code the server attached replaces
 * the raw message with a plain explanation and the next step; without a known code the message is humanized as
 * before. Shared by the Copilot and AI Hub runtime providers.
 */
export const toRunErrorData = ({code, message}: {code?: string; message?: string}): RunErrorDataI => {
    const codeMessage = code ? RUN_ERROR_CODE_MESSAGES[code] : undefined;

    if (code && codeMessage) {
        return {code, message: codeMessage};
    }

    const rawMessage = typeof message === 'string' ? message.trim() : '';

    return {message: rawMessage.length > 0 ? humanizeAgentErrorMessage(rawMessage) : FALLBACK_MESSAGE};
};
