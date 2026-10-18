import {ThreadMessageLike} from '@assistant-ui/react';

const FUNCTION_CALLS_BLOCK = /<(?:antml:)?function_calls\b[\s\S]*?<\/(?:antml:)?function_calls>/gi;
const INVOKE_BLOCK = /<(?:antml:)?invoke\b[\s\S]*?<\/(?:antml:)?invoke>/gi;

const DANGLING_TOOL_MARKUP = /<(?:antml:)?(?:function_calls|invoke)\b[\s\S]*$/i;

/**
 * Removes leaked tool-call markup from a single assistant text string. Returns the input unchanged (same reference)
 * when there is nothing to strip, so callers can cheaply detect "no change".
 */
export const stripLeakedToolMarkup = (text: string): string => {
    if (!text.includes('<')) {
        return text;
    }

    const cleaned = text
        .replace(FUNCTION_CALLS_BLOCK, '')
        .replace(INVOKE_BLOCK, '')
        .replace(DANGLING_TOOL_MARKUP, '')
        .replace(/\n{3,}/g, '\n\n')
        .trim();

    return cleaned === text ? text : cleaned;
};

/**
 * Sanitizes leaked tool-call markup out of every assistant message with string content. Preserves reference equality
 * for the array and for each message when nothing changed, so downstream memoized projections don't re-render on clean
 * input.
 */
export const sanitizeAssistantMessages = (messages: ThreadMessageLike[]): ThreadMessageLike[] => {
    let changed = false;

    const sanitizedMessages = messages.map((message) => {
        if (message.role !== 'assistant' || typeof message.content !== 'string') {
            return message;
        }

        const cleaned = stripLeakedToolMarkup(message.content);

        if (cleaned === message.content) {
            return message;
        }

        changed = true;

        return {...message, content: cleaned};
    });

    return changed ? sanitizedMessages : messages;
};
