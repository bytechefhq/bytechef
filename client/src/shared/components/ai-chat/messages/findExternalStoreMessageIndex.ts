export const EXTERNAL_STORE_FALLBACK_ID_PREFIX = '__external_store_fallback_';

interface ExternalStoreMessageI {
    id?: string;
    role: string;
}

export function findExternalStoreMessageIndex(
    messages: readonly ExternalStoreMessageI[],
    messageId: string | null | undefined
): number {
    if (messageId == null) {
        return -1;
    }

    const identifiedIndex = messages.findIndex((message) => message.id === messageId);

    if (identifiedIndex !== -1) {
        return identifiedIndex;
    }

    if (!messageId.startsWith(EXTERNAL_STORE_FALLBACK_ID_PREFIX)) {
        return -1;
    }

    const fallbackIndexText = messageId.slice(EXTERNAL_STORE_FALLBACK_ID_PREFIX.length);

    if (!/^\d+$/.test(fallbackIndexText)) {
        return -1;
    }

    const fallbackIndex = Number(fallbackIndexText);

    if (fallbackIndex >= messages.length || messages[fallbackIndex].id != null) {
        return -1;
    }

    return fallbackIndex;
}

export function findTurnUserMessageIndex(messages: readonly ExternalStoreMessageI[], fromIndex: number): number {
    for (let index = Math.min(fromIndex, messages.length - 1); index >= 0; index--) {
        if (messages[index].role === 'user') {
            return index;
        }
    }

    return -1;
}
