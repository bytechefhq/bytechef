import {act, createTestQueryClientWrapper, renderHook, resetAll, waitFor} from '@/shared/util/test-utils';
import {QueryClient} from '@tanstack/react-query';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {useAiSkillsStore} from '../../stores/useAiSkillsStore';
import useAiSkillDetail, {findDefaultFilePath, isMarkdownPath} from '../useAiSkillDetail';

const {
    closeSkillDetailMock,
    deleteAiSkillMock,
    fileContentQueryMock,
    filePathsRef,
    navigateMock,
    noopMutationMock,
    removeFileInSkillMock,
} = vi.hoisted(() => ({
    closeSkillDetailMock: vi.fn(),
    deleteAiSkillMock: vi.fn(),
    fileContentQueryMock: vi.fn(),
    filePathsRef: {current: [] as string[]},
    navigateMock: vi.fn(),
    noopMutationMock: vi.fn(),
    removeFileInSkillMock: vi.fn(),
}));

vi.mock('@/pages/automation/ai/skills/utils/downloadAiSkill', () => ({default: vi.fn()}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiSkillFileContentQuery: (variables: {path: string}, options: {enabled: boolean}) => {
        fileContentQueryMock(variables, options);

        return {data: {aiSkillFileContent: '# Billing'}, isError: false, isLoading: false};
    },
    useAiSkillFilePathsQuery: () => ({data: {aiSkillFilePaths: filePathsRef.current}, isError: false}),
    useAiSkillQuery: () => ({
        data: {aiSkill: {description: 'Refund runbook', id: '42', name: 'billing-runbook'}},
        isError: false,
    }),
    useCreateAdditionalFilesInSkillMutation: () => ({mutateAsync: noopMutationMock}),
    useDeleteAiSkillMutation: () => ({mutateAsync: deleteAiSkillMock}),
    useRemoveFileInSkillMutation: () => ({mutateAsync: removeFileInSkillMock}),
    useUpdateAiSkillContentMutation: () => ({mutateAsync: noopMutationMock}),
    useUpdateAiSkillMutation: () => ({mutateAsync: noopMutationMock}),
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => navigateMock,
}));

vi.mock('sonner', () => ({toast: {error: vi.fn(), success: vi.fn()}}));

const renderDetail = (initialEntry = '/embedded/settings/ai/skills/42') => {
    const wrapper = ({children}: {children: ReactNode}) => (
        <MemoryRouter initialEntries={[initialEntry]}>{createTestQueryClientWrapper()({children})}</MemoryRouter>
    );

    return renderHook(() => useAiSkillDetail(), {wrapper});
};

describe('findDefaultFilePath', () => {
    it('finds SKILL.md whatever the casing and wherever it sits', () => {
        expect(findDefaultFilePath(['scripts/run.sh', 'skill.md'])).toBe('skill.md');
        expect(findDefaultFilePath(['nested/SKILL.md'])).toBe('nested/SKILL.md');
    });

    it('returns undefined when the archive carries no SKILL.md', () => {
        expect(findDefaultFilePath(['README.md', 'scripts/run.sh'])).toBeUndefined();
    });
});

describe('isMarkdownPath', () => {
    it('recognises markdown by extension, case-insensitively', () => {
        expect(isMarkdownPath('SKILL.MD')).toBe(true);
        expect(isMarkdownPath('scripts/run.sh')).toBe(false);
    });
});

describe('useAiSkillDetail', () => {
    beforeEach(() => {
        filePathsRef.current = ['SKILL.md', 'scripts/run.sh'];

        useAiSkillsStore.setState({closeSkillDetail: closeSkillDetailMock, selectedSkillId: '42'});
    });

    afterEach(() => {
        resetAll();
    });

    it('opens SKILL.md by default and treats it as markdown', async () => {
        const {result} = renderDetail();

        await waitFor(() => expect(result.current.selectedFilePath).toBe('SKILL.md'));

        expect(result.current.isMarkdown).toBe(true);
        expect(result.current.editorLanguage).toBe('plaintext');
    });

    it('leaves the selection empty when the archive carries no SKILL.md', async () => {
        filePathsRef.current = ['scripts/run.sh'];

        const {result} = renderDetail();

        await waitFor(() => expect(result.current.filePaths).toHaveLength(1));

        expect(result.current.selectedFilePath).toBeNull();
        expect(result.current.isMarkdown).toBe(false);
    });

    it('stops loading a removed file before the file list is refreshed', async () => {
        let refreshFilePaths: () => void = () => {};

        const invalidateQueriesSpy = vi.spyOn(QueryClient.prototype, 'invalidateQueries').mockImplementation(
            () =>
                new Promise<void>((resolve) => {
                    refreshFilePaths = resolve;
                })
        );

        const {result} = renderDetail();

        await waitFor(() => expect(result.current.selectedFilePath).toBe('SKILL.md'));

        act(() => {
            result.current.handleFileSelect('scripts/run.sh');
        });

        let removal: Promise<void> = Promise.resolve();

        await act(async () => {
            removal = result.current.handleRemoveFile('scripts/run.sh');
        });

        await waitFor(() => expect(invalidateQueriesSpy).toHaveBeenCalled());

        expect(removeFileInSkillMock).toHaveBeenCalledWith({id: '42', path: 'scripts/run.sh'});
        expect(fileContentQueryMock).toHaveBeenLastCalledWith(
            expect.not.objectContaining({path: 'scripts/run.sh'}),
            expect.anything()
        );

        await act(async () => {
            refreshFilePaths();

            await removal;
        });

        invalidateQueriesSpy.mockRestore();
    });

    it('returns to the skills mount the visitor came from after a delete', async () => {
        const {result} = renderDetail();

        await act(async () => {
            await result.current.handleDelete();
        });

        expect(closeSkillDetailMock).toHaveBeenCalled();
        expect(navigateMock).toHaveBeenCalledWith('/embedded/settings/ai/skills');
        expect(deleteAiSkillMock).toHaveBeenCalledWith({id: '42'});
    });
});
