import {AiSkill} from '@/shared/middleware/graphql';
import {createTestQueryClientWrapper, renderHook, resetAll, waitFor} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import useAiSkillsList from '../useAiSkillsList';

const {deleteMutateAsyncMock, downloadAiSkillMock, searchQueryRef, toastErrorMock, updateMutateMock} = vi.hoisted(
    () => ({
        deleteMutateAsyncMock: vi.fn(),
        downloadAiSkillMock: vi.fn(),
        searchQueryRef: {current: ''},
        toastErrorMock: vi.fn(),
        updateMutateMock: vi.fn(),
    })
);

vi.mock('@/pages/automation/ai/skills/stores/useAiSkillsStore', () => ({
    useAiSkillsStore: (selector: (state: {searchQuery: string}) => unknown) =>
        selector({searchQuery: searchQueryRef.current}),
}));

vi.mock('@/pages/automation/ai/skills/utils/downloadAiSkill', () => ({default: downloadAiSkillMock}));

vi.mock('@/shared/middleware/graphql', () => ({
    useDeleteAiSkillMutation: () => ({mutateAsync: deleteMutateAsyncMock}),
    useUpdateAiSkillMutation: () => ({mutate: updateMutateMock}),
}));

vi.mock('sonner', () => ({toast: {error: toastErrorMock}}));

const skills = [
    {id: '1', name: 'billing-runbook', tags: [{id: '10', name: 'Billing'}]},
    {id: '2', name: 'support-triage', tags: [{id: '20', name: 'Support'}]},
    {id: '3', name: 'billing-refunds', tags: []},
] as unknown as AiSkill[];

const renderList = (initialEntry = '/settings/ai/skills') => {
    const wrapper = ({children}: {children: ReactNode}) => (
        <MemoryRouter initialEntries={[initialEntry]}>{createTestQueryClientWrapper()({children})}</MemoryRouter>
    );

    return renderHook(() => useAiSkillsList(skills), {wrapper});
};

describe('useAiSkillsList', () => {
    beforeEach(() => {
        searchQueryRef.current = '';
    });

    afterEach(() => {
        resetAll();
    });

    it('returns every skill when nothing is filtered', () => {
        const {result} = renderList();

        expect(result.current.filteredSkills).toHaveLength(3);
    });

    it('filters by the search query, case-insensitively', () => {
        searchQueryRef.current = 'BILLING';

        const {result} = renderList();

        expect(result.current.filteredSkills.map((skill) => skill.id)).toEqual(['1', '3']);
    });

    it('filters by the tag in the url', () => {
        const {result} = renderList('/settings/ai/skills?tagId=20');

        expect(result.current.filteredSkills.map((skill) => skill.id)).toEqual(['2']);
    });

    it('drops a skill carrying no tags when a tag is selected', () => {
        const {result} = renderList('/settings/ai/skills?tagId=10');

        expect(result.current.filteredSkills.map((skill) => skill.id)).toEqual(['1']);
    });

    it('applies the search query and the tag together', () => {
        searchQueryRef.current = 'billing';

        const {result} = renderList('/settings/ai/skills?tagId=10');

        expect(result.current.filteredSkills.map((skill) => skill.id)).toEqual(['1']);
    });

    it('deletes a skill through the mutation', async () => {
        const {result} = renderList();

        await result.current.deleteSkill('1');

        expect(deleteMutateAsyncMock).toHaveBeenCalledWith({id: '1'});
    });

    it('updates a skill, dropping a blank description', () => {
        const {result} = renderList();

        result.current.updateSkill('1', 'renamed', null);

        expect(updateMutateMock).toHaveBeenCalledWith({description: undefined, id: '1', name: 'renamed'});
    });

    it('reports a failed download as a toast', async () => {
        downloadAiSkillMock.mockRejectedValue(new Error('disk full'));

        const {result} = renderList();

        await result.current.handleDownloadSkill('1', 'billing-runbook');

        await waitFor(() =>
            expect(toastErrorMock).toHaveBeenCalledWith('Failed to download skill', {description: 'disk full'})
        );
    });
});
