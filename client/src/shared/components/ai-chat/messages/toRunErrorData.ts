import {humanizeAgentErrorMessage} from '@/shared/components/ai-chat/messages/humanizeAgentErrorMessage';

export const RUN_ERROR_CODE_ACCESS_DENIED = 'ACCESS_DENIED';

export const RUN_ERROR_CODE_CHAT_ACCESS_DENIED = 'CHAT_ACCESS_DENIED';

export const RUN_ERROR_CODE_NO_AI_PROVIDER = 'NO_AI_PROVIDER';

export const RUN_ERROR_CODE_WORKSPACE_ACCESS_DENIED = 'WORKSPACE_ACCESS_DENIED';

export interface RunErrorDataI {
    code?: string;
    message: string;
}

const FALLBACK_MESSAGE = 'The agent run failed before completing this turn.';

const RUN_ERROR_CODE_MESSAGES: Record<string, string> = {
    MODEL_SELECTION_INCOMPLETE: 'Your model selection is incomplete. Pick a model again.',
    [RUN_ERROR_CODE_ACCESS_DENIED]: "You don't have permission to do this.",
    [RUN_ERROR_CODE_CHAT_ACCESS_DENIED]: "You don't have access to this chat. Start a new chat to continue.",
    [RUN_ERROR_CODE_NO_AI_PROVIDER]:
        'No AI provider is set up for this environment. Enable one in Settings > AI > Providers. If you do not have permission, ask an administrator.',
    [RUN_ERROR_CODE_WORKSPACE_ACCESS_DENIED]:
        "You don't have permission to do this in this workspace. Ask a workspace administrator for access.",
    SELECTED_MODEL_UNAVAILABLE: 'The model you picked is no longer available. Pick another model in the model picker.',
};

export const toRunErrorData = ({code, message}: {code?: string; message?: string}): RunErrorDataI => {
    const codeMessage = code ? RUN_ERROR_CODE_MESSAGES[code] : undefined;

    if (code && codeMessage) {
        return {code, message: codeMessage};
    }

    const rawMessage = typeof message === 'string' ? message.trim() : '';

    return {message: rawMessage.length > 0 ? humanizeAgentErrorMessage(rawMessage) : FALLBACK_MESSAGE};
};
