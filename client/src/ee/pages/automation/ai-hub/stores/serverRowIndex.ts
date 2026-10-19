import {ThreadMessageLike} from '@assistant-ui/react';

export function getServerRowIndex(message: ThreadMessageLike | undefined): number | undefined {
    const serverRowIndex = message?.metadata?.custom?.serverRowIndex;

    return typeof serverRowIndex === 'number' ? serverRowIndex : undefined;
}

export function withServerRowIndex(message: ThreadMessageLike, serverRowIndex: number): ThreadMessageLike {
    return {
        ...message,
        metadata: {...message.metadata, custom: {...message.metadata?.custom, serverRowIndex}},
    } as ThreadMessageLike;
}
