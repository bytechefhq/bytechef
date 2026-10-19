import {EXTERNAL_STORE_FALLBACK_ID_PREFIX} from '@/shared/components/ai-chat/messages/findExternalStoreMessageIndex';
import {type Message} from '@ag-ui/client';
import {ThreadMessageLike} from '@assistant-ui/react';

function getMessageText(content: ThreadMessageLike['content']): string {
    if (typeof content === 'string') {
        return content;
    }

    return content
        .map((part) => (part.type === 'text' && 'text' in part ? part.text : ''))
        .filter((text) => text.length > 0)
        .join('\n');
}

export function toAgentMessages(messages: readonly ThreadMessageLike[]): Message[] {
    return messages.flatMap((message, index): Message[] => {
        const id = message.id ?? `${EXTERNAL_STORE_FALLBACK_ID_PREFIX}${index}`;
        const text = getMessageText(message.content);

        if (message.role === 'user') {
            return [{content: text, id, role: 'user'}];
        }

        if (message.role === 'assistant' && text.length > 0) {
            return [{content: text, id, role: 'assistant'}];
        }

        return [];
    });
}
