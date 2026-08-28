import {renderHook} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import useKnowledgeBaseList from '../useKnowledgeBaseList';

const mockKnowledgeBases = [
    {id: 'kb-1', name: 'Zebra KB'},
    {id: 'kb-2', name: 'Apple KB'},
    {id: 'kb-3', name: 'Banana KB'},
];

const mockTagsByKnowledgeBase = [
    {knowledgeBaseId: 'kb-1', tags: [{id: '1', name: 'Tag 1'}]},
    {
        knowledgeBaseId: 'kb-2',
        tags: [
            {id: '1', name: 'Tag 1'},
            {id: '2', name: 'Tag 2'},
        ],
    },
];

describe('useKnowledgeBaseList', () => {
    describe('sortedKnowledgeBases', () => {
        it('sorts knowledge bases alphabetically by name', () => {
            const {result} = renderHook(() =>
                useKnowledgeBaseList({
                    knowledgeBases: mockKnowledgeBases,
                    tagsByKnowledgeBaseData: mockTagsByKnowledgeBase,
                })
            );

            expect(result.current.sortedKnowledgeBases.map((kb) => kb.name)).toEqual([
                'Apple KB',
                'Banana KB',
                'Zebra KB',
            ]);
        });

        it('handles empty list', () => {
            const {result} = renderHook(() =>
                useKnowledgeBaseList({
                    knowledgeBases: [],
                    tagsByKnowledgeBaseData: [],
                })
            );

            expect(result.current.sortedKnowledgeBases).toEqual([]);
        });

        it('trims names for comparison', () => {
            const kbWithWhitespace = [
                {id: 'kb-1', name: '  Zebra KB'},
                {id: 'kb-2', name: 'Apple KB  '},
            ];

            const {result} = renderHook(() =>
                useKnowledgeBaseList({
                    knowledgeBases: kbWithWhitespace,
                    tagsByKnowledgeBaseData: [],
                })
            );

            expect(result.current.sortedKnowledgeBases[0].name).toBe('Apple KB  ');
            expect(result.current.sortedKnowledgeBases[1].name).toBe('  Zebra KB');
        });

        it('handles numeric sorting naturally', () => {
            const kbWithNumbers = [
                {id: 'kb-1', name: 'KB 10'},
                {id: 'kb-2', name: 'KB 2'},
                {id: 'kb-3', name: 'KB 1'},
            ];

            const {result} = renderHook(() =>
                useKnowledgeBaseList({
                    knowledgeBases: kbWithNumbers,
                    tagsByKnowledgeBaseData: [],
                })
            );

            expect(result.current.sortedKnowledgeBases.map((kb) => kb.name)).toEqual(['KB 1', 'KB 2', 'KB 10']);
        });
    });

    describe('tagsByKnowledgeBaseMap', () => {
        it('creates map from tags by knowledge base data', () => {
            const {result} = renderHook(() =>
                useKnowledgeBaseList({
                    knowledgeBases: mockKnowledgeBases,
                    tagsByKnowledgeBaseData: mockTagsByKnowledgeBase,
                })
            );

            expect(result.current.tagsByKnowledgeBaseMap.get('kb-1')).toEqual([{id: '1', name: 'Tag 1'}]);
            expect(result.current.tagsByKnowledgeBaseMap.get('kb-2')).toEqual([
                {id: '1', name: 'Tag 1'},
                {id: '2', name: 'Tag 2'},
            ]);
        });

        it('returns empty array for knowledge base without tags', () => {
            const {result} = renderHook(() =>
                useKnowledgeBaseList({
                    knowledgeBases: mockKnowledgeBases,
                    tagsByKnowledgeBaseData: mockTagsByKnowledgeBase,
                })
            );

            expect(result.current.tagsByKnowledgeBaseMap.get('kb-3')).toBeUndefined();
        });

        it('handles null tags', () => {
            const tagsWithNull = [{knowledgeBaseId: 'kb-1', tags: null as unknown as {id: string; name: string}[]}];

            const {result} = renderHook(() =>
                useKnowledgeBaseList({
                    knowledgeBases: mockKnowledgeBases,
                    tagsByKnowledgeBaseData: tagsWithNull,
                })
            );

            expect(result.current.tagsByKnowledgeBaseMap.get('kb-1')).toEqual([]);
        });
    });
});
