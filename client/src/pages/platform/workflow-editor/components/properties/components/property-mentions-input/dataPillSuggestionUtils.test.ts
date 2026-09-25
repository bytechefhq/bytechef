import {DataPillType} from '@/shared/types';
import {Editor} from '@tiptap/react';
import {describe, expect, it} from 'vitest';

import {filterDataPillSuggestions, findDataPillMatchRange} from './dataPillSuggestionUtils';
import {getSuggestionOptions} from './propertyMentionsInputEditorSuggestionOptions';

function dataPills(...values: string[]): DataPillType[] {
    return values.map((value) => ({id: value, value}));
}

function values(pills: DataPillType[]): string[] {
    return pills.map((pill) => pill.value);
}

const FIRECRAWL_DATA_PILLS = dataPills(
    'trigger_1',
    'trigger_1.body',
    'firecrawl_5',
    'firecrawl_5.success',
    'firecrawl_5.data',
    'firecrawl_5.data.json',
    'firecrawl_5.data.json.result',
    'firecrawl_5.data.json.result[index]',
    'firecrawl_5.data.jsonResult'
);

describe('filterDataPillSuggestions', () => {
    it('returns every data pill in its original order for an empty query', () => {
        expect(values(filterDataPillSuggestions(FIRECRAWL_DATA_PILLS, ''))).toEqual(values(FIRECRAWL_DATA_PILLS));
    });

    it('keeps full-path prefix matches', () => {
        expect(values(filterDataPillSuggestions(FIRECRAWL_DATA_PILLS, 'trigger'))).toEqual([
            'trigger_1',
            'trigger_1.body',
        ]);
    });

    it('matches a segment in the middle of the path', () => {
        expect(values(filterDataPillSuggestions(FIRECRAWL_DATA_PILLS, 'result'))).toEqual([
            'firecrawl_5.data.json.result',
            'firecrawl_5.data.json.result[index]',
            'firecrawl_5.data.jsonResult',
        ]);
    });

    it('matches a segment that starts after an array bracket', () => {
        expect(values(filterDataPillSuggestions(FIRECRAWL_DATA_PILLS, 'index'))).toEqual([
            'firecrawl_5.data.json.result[index]',
        ]);
    });

    it('matches a query spanning several segments', () => {
        expect(values(filterDataPillSuggestions(FIRECRAWL_DATA_PILLS, 'json.result'))).toEqual([
            'firecrawl_5.data.json.result',
            'firecrawl_5.data.json.result[index]',
        ]);
    });

    it('ranks prefix matches before segment matches before plain substring matches', () => {
        const pills = dataPills('myData.value', 'other.data', 'data', 'other.data.nested');

        expect(values(filterDataPillSuggestions(pills, 'data'))).toEqual([
            'data',
            'other.data',
            'other.data.nested',
            'myData.value',
        ]);
    });

    it('ignores case', () => {
        expect(values(filterDataPillSuggestions(FIRECRAWL_DATA_PILLS, 'JSONRESULT'))).toEqual([
            'firecrawl_5.data.jsonResult',
        ]);
    });

    it('returns nothing when no path contains the query', () => {
        expect(filterDataPillSuggestions(FIRECRAWL_DATA_PILLS, 'missing')).toEqual([]);
    });
});

describe('findDataPillMatchRange', () => {
    it('returns null for an empty query', () => {
        expect(findDataPillMatchRange('firecrawl_5.data', '')).toBeNull();
    });

    it('returns null when the value does not contain the query', () => {
        expect(findDataPillMatchRange('firecrawl_5.data', 'missing')).toBeNull();
    });

    it('returns the prefix range for a prefix match', () => {
        expect(findDataPillMatchRange('firecrawl_5.data', 'fire')).toEqual({end: 4, start: 0});
    });

    it('prefers the segment-start occurrence over an earlier mid-word one', () => {
        expect(findDataPillMatchRange('myData.data', 'data')).toEqual({end: 11, start: 7});
    });

    it('falls back to the first substring occurrence', () => {
        expect(findDataPillMatchRange('firecrawl_5.data.jsonResult', 'result')).toEqual({end: 27, start: 21});
    });

    it('ignores case', () => {
        expect(findDataPillMatchRange('firecrawl_5.data.JSON', 'json')).toEqual({end: 21, start: 17});
    });
});

describe('getSuggestionOptions items', () => {
    it('filters the editor data pills with segment-aware matching', () => {
        const items = getSuggestionOptions().items!;

        const editor = {storage: {MentionStorage: {dataPills: FIRECRAWL_DATA_PILLS}}} as unknown as Editor;

        const abortController = new AbortController();

        expect(values(items({editor, query: 'index', signal: abortController.signal}) as DataPillType[])).toEqual([
            'firecrawl_5.data.json.result[index]',
        ]);
    });
});
