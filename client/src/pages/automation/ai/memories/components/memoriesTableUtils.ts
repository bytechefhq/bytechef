import {formatDistanceToNow} from 'date-fns';

import {AiAutoMemoryI} from '../hooks/useAiAutoMemories';

export function filterMemoriesBySearchTerm<MemoryType extends AiAutoMemoryI>(
    memories: MemoryType[] | undefined,
    searchTerm: string
): MemoryType[] {
    if (!memories) {
        return [];
    }

    const lowerSearch = searchTerm.trim().toLowerCase();

    if (!lowerSearch) {
        return memories;
    }

    return memories.filter((memory) => {
        const title = memory.title.toLowerCase();
        const description = (memory.description ?? '').toLowerCase();

        return title.includes(lowerSearch) || description.includes(lowerSearch);
    });
}

export function findMemory<MemoryType extends AiAutoMemoryI>(
    memories: MemoryType[] | undefined,
    memoryId: number | null
): MemoryType | null {
    if (memoryId === null) {
        return null;
    }

    return memories?.find((memory) => memory.id === memoryId) || null;
}

export function formatRelative(value: string): string {
    try {
        return formatDistanceToNow(new Date(value), {addSuffix: true});
    } catch (formatError) {
        console.warn('Memories: formatRelative failed', {
            message: formatError instanceof Error ? formatError.message : String(formatError),
            value,
        });

        return '';
    }
}
