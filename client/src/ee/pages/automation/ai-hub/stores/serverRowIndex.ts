import {ThreadMessageLike} from '@assistant-ui/react';

/** The index of the server transcript row a client message stands for, or undefined for a client-only row. */
export function getServerRowIndex(message: ThreadMessageLike | undefined): number | undefined {
    const serverRowIndex = message?.metadata?.custom?.serverRowIndex;

    return typeof serverRowIndex === 'number' ? serverRowIndex : undefined;
}

/** Returns `message` marked as standing for the server transcript row at `serverRowIndex`. */
export function withServerRowIndex(message: ThreadMessageLike, serverRowIndex: number): ThreadMessageLike {
    return {
        ...message,
        metadata: {...message.metadata, custom: {...message.metadata?.custom, serverRowIndex}},
    } as ThreadMessageLike;
}
