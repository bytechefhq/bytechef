import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useDataTablesFilterTitle from '../useDataTablesFilterTitle';

const hoisted = vi.hoisted(() => {
    return {
        mockSearchParams: new URLSearchParams(),
    };
});

vi.mock('react-router-dom', () => ({
    useSearchParams: vi.fn(() => [hoisted.mockSearchParams]),
}));

const mockAllTags = [
    {id: '1', name: 'Tag1'},
    {id: '2', name: 'Tag2'},
];

const mockTagsByTableData = [{tableId: '1', tags: [{id: '1', name: 'Tag1'}]}];

describe('useDataTablesFilterTitle', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        hoisted.mockSearchParams = new URLSearchParams();
    });

    describe('without tag filter', () => {
        it('returns tagId as null', () => {
            const {result} = renderHook(() =>
                useDataTablesFilterTitle({allTags: mockAllTags, tagsByTableData: mockTagsByTableData})
            );

            expect(result.current.tagId).toBeNull();
        });

        it('returns pageTitle as undefined', () => {
            const {result} = renderHook(() =>
                useDataTablesFilterTitle({allTags: mockAllTags, tagsByTableData: mockTagsByTableData})
            );

            expect(result.current.pageTitle).toBeUndefined();
        });
    });

    describe('with tag filter', () => {
        it('returns tagId from search params', () => {
            hoisted.mockSearchParams = new URLSearchParams('tagId=1');

            const {result} = renderHook(() =>
                useDataTablesFilterTitle({allTags: mockAllTags, tagsByTableData: mockTagsByTableData})
            );

            expect(result.current.tagId).toBe('1');
        });

        it('returns pageTitle from global tags', () => {
            hoisted.mockSearchParams = new URLSearchParams('tagId=1');

            const {result} = renderHook(() =>
                useDataTablesFilterTitle({allTags: mockAllTags, tagsByTableData: mockTagsByTableData})
            );

            expect(result.current.pageTitle).toBe('Tag1');
        });

        it('returns pageTitle from byTable tags when not in global', () => {
            hoisted.mockSearchParams = new URLSearchParams('tagId=1');

            const {result} = renderHook(() =>
                useDataTablesFilterTitle({allTags: mockAllTags, tagsByTableData: mockTagsByTableData})
            );

            expect(result.current.pageTitle).toBe('Tag1');
        });
    });
});
