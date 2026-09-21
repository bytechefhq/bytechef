type MessageTextPartType = {text?: string; type?: string};
type MessageContentType = string | ReadonlyArray<MessageTextPartType> | undefined;

const FENCE_REGEX = /```(?:[a-zA-Z]*)\r?\n([\s\S]*?)```/;

export function extractDefinitionFromMessage(content: MessageContentType): string | null {
    let text = '';

    if (typeof content === 'string') {
        text = content;
    } else if (Array.isArray(content)) {
        text = content.map((part) => part?.text ?? '').join('');
    }

    const match = FENCE_REGEX.exec(text);

    if (!match) {
        return null;
    }

    return match[1].trim();
}
