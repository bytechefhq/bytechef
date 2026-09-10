import {act, renderHook, resetAll} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {MemoryRouter, useSearchParams} from 'react-router-dom';
import {afterEach, describe, expect, it, vi} from 'vitest';

import useAiSkillsTagFilterGroups, {ALL_TAGS} from '../useAiSkillsTagFilterGroups';

const {useAiSkillTagsQueryMock} = vi.hoisted(() => ({useAiSkillTagsQueryMock: vi.fn()}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiSkillTagsQuery: useAiSkillTagsQueryMock,
}));

/**
 * Renders the hook next to the router's own view of the query string, so a test can tell a deleted `tagId`
 * apart from one set to the all-tags sentinel - the hook reads both back as {@link ALL_TAGS}.
 */
const renderTagFilterGroups = (initialEntry = '/settings/skills') => {
    const wrapper = ({children}: {children: ReactNode}) => (
        <MemoryRouter initialEntries={[initialEntry]}>{children}</MemoryRouter>
    );

    return renderHook(
        () => {
            const [searchParams] = useSearchParams();

            return {group: useAiSkillsTagFilterGroups()[0], searchParams};
        },
        {wrapper}
    );
};

describe('useAiSkillsTagFilterGroups', () => {
    afterEach(() => {
        resetAll();
    });

    it('offers only the all-tags option while the tags query has not resolved', () => {
        useAiSkillTagsQueryMock.mockReturnValue({data: undefined});

        const {result} = renderTagFilterGroups();

        expect(result.current.group.options).toEqual([{label: 'All tags', value: ALL_TAGS}]);
    });

    it('lists every tag after the all-tags option', () => {
        useAiSkillTagsQueryMock.mockReturnValue({
            data: {
                aiSkillTags: [
                    {id: '1', name: 'Billing'},
                    {id: '2', name: 'Support'},
                ],
            },
        });

        const {result} = renderTagFilterGroups();

        expect(result.current.group.options).toEqual([
            {label: 'All tags', value: ALL_TAGS},
            {label: 'Billing', value: '1'},
            {label: 'Support', value: '2'},
        ]);
    });

    it('sits on the all-tags value when the url carries no tagId', () => {
        useAiSkillTagsQueryMock.mockReturnValue({data: {aiSkillTags: []}});

        const {result} = renderTagFilterGroups();

        expect(result.current.group.allValue).toBe(ALL_TAGS);
        expect(result.current.group.value).toBe(ALL_TAGS);
    });

    it('takes its selection from the tagId search param', () => {
        useAiSkillTagsQueryMock.mockReturnValue({data: {aiSkillTags: [{id: '1', name: 'Billing'}]}});

        const {result} = renderTagFilterGroups('/settings/skills?tagId=1');

        expect(result.current.group.value).toBe('1');
    });

    it('writes a picked tag back to the url', () => {
        useAiSkillTagsQueryMock.mockReturnValue({data: {aiSkillTags: [{id: '1', name: 'Billing'}]}});

        const {result} = renderTagFilterGroups();

        act(() => result.current.group.onChange('1'));

        expect(result.current.searchParams.get('tagId')).toBe('1');
        expect(result.current.group.value).toBe('1');
    });

    it('drops the tagId from the url rather than storing the sentinel', () => {
        useAiSkillTagsQueryMock.mockReturnValue({data: {aiSkillTags: [{id: '1', name: 'Billing'}]}});

        const {result} = renderTagFilterGroups('/settings/skills?tagId=1');

        act(() => result.current.group.onChange(ALL_TAGS));

        expect(result.current.searchParams.has('tagId')).toBe(false);
        expect(result.current.group.value).toBe(ALL_TAGS);
    });

    it('leaves unrelated search params in place', () => {
        useAiSkillTagsQueryMock.mockReturnValue({data: {aiSkillTags: [{id: '1', name: 'Billing'}]}});

        const {result} = renderTagFilterGroups('/settings/skills?view=grid');

        act(() => result.current.group.onChange('1'));

        expect(result.current.searchParams.get('view')).toBe('grid');
        expect(result.current.searchParams.get('tagId')).toBe('1');
    });
});
