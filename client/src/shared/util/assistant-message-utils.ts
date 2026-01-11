import {ThreadMessageLike} from '@assistant-ui/react';

/**
 * Appends content to the last assistant message in the messages array.
 * If no assistant message exists, creates a new one with the delta content.
 *
 * @param messages - Array of thread messages
 * @param delta - Content to append to the last assistant message
 * @returns Updated messages array
 */
export function appendToLastAssistantMessage(messages: ThreadMessageLike[], delta: string): ThreadMessageLike[] {
    const updatedMessages = [...messages];

    // find last assistant message
    for (let i = updatedMessages.length - 1; i >= 0; i--) {
        const msg = updatedMessages[i] as ThreadMessageLike & {content?: string; role?: string};

        if (msg && msg.role === 'assistant') {
            const current = typeof msg.content === 'string' ? msg.content : '';
            const chunk = typeof delta === 'string' ? delta : String(delta ?? '');

            updatedMessages[i] = {...msg, content: current + chunk};

            return updatedMessages;
        }
    }

    // no assistant message yet; create one
    return [...updatedMessages, {content: delta, role: 'assistant'} as ThreadMessageLike];
}

/**
 * Sets the content of the last assistant message in the messages array.
 * If no assistant message exists, creates a new one with the content.
 *
 * @param messages - Array of thread messages
 * @param content - Content to set on the last assistant message
 * @returns Updated messages array
 */
export function setLastAssistantMessageContent(messages: ThreadMessageLike[], content: string): ThreadMessageLike[] {
    const updatedMessages = [...messages];

    for (let i = updatedMessages.length - 1; i >= 0; i--) {
        const msg = updatedMessages[i] as ThreadMessageLike & {content?: string; role?: string};

        if (msg && msg.role === 'assistant') {
            updatedMessages[i] = {...msg, content};

            return updatedMessages;
        }
    }

    return [...updatedMessages, {content, role: 'assistant'} as ThreadMessageLike];
}
