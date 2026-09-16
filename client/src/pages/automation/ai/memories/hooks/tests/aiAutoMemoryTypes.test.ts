import {AI_AUTO_MEMORY_TYPES, AI_AUTO_MEMORY_TYPE_META} from '@/pages/automation/ai/memories/hooks/useAiAutoMemories';
import {AiAutoMemoryType} from '@/shared/middleware/graphql';
import {describe, expect, it} from 'vitest';

// The Record's exhaustiveness is enforced at compile time, but the list itself is derived through an
// Object.keys cast. These pin the two ends at runtime so a server-side type appended to AiAutoMemoryType
// cannot end up missing from the Memories sidebar filter or the edit dialog's Type select.
describe('AI_AUTO_MEMORY_TYPES', () => {
    it('covers every value of the generated AiAutoMemoryType enum', () => {
        expect([...AI_AUTO_MEMORY_TYPES].sort()).toEqual([...Object.values(AiAutoMemoryType)].sort());
    });

    it('gives every type a non-empty label', () => {
        for (const memoryType of AI_AUTO_MEMORY_TYPES) {
            expect(AI_AUTO_MEMORY_TYPE_META[memoryType].label).toBeTruthy();
        }
    });

    it('lists the types in the declared order rather than alphabetically', () => {
        expect(AI_AUTO_MEMORY_TYPES).toEqual(['USER', 'FEEDBACK', 'PROJECT', 'REFERENCE']);
    });
});
