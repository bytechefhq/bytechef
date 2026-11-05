import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook} from '@testing-library/react';
import {type ChangeEvent, type DragEvent, type ReactNode} from 'react';
import {toast} from 'sonner';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useAiSkillUploadForm from '../useAiSkillUploadForm';

const hoisted = vi.hoisted(() => ({
    createAiSkillFromInstructionsMutateAsync: vi.fn(),
    createAiSkillMutateAsync: vi.fn(),
    mutationPendingState: {fromFile: false, fromInstructions: false},
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useCreateAiSkillFromInstructionsMutation: () => ({
        isPending: hoisted.mutationPendingState.fromInstructions,
        mutateAsync: hoisted.createAiSkillFromInstructionsMutateAsync,
    }),
    useCreateAiSkillMutation: () => ({
        isPending: hoisted.mutationPendingState.fromFile,
        mutateAsync: hoisted.createAiSkillMutateAsync,
    }),
}));

vi.mock('sonner', () => ({toast: {error: vi.fn(), info: vi.fn(), success: vi.fn()}}));

const createFile = (name: string, content = 'content') => new File([content], name);

const createChangeEvent = (files: File[]) =>
    ({target: {files: files.length ? files : null}}) as unknown as ChangeEvent<HTMLInputElement>;

const createDragEvent = (files: File[]) =>
    ({
        dataTransfer: {files},
        preventDefault: vi.fn(),
    }) as unknown as DragEvent<HTMLDivElement> & {preventDefault: ReturnType<typeof vi.fn>};

describe('useAiSkillUploadForm', () => {
    let queryClient: QueryClient;
    let invalidateQueriesSpy: ReturnType<typeof vi.spyOn>;

    beforeEach(() => {
        vi.clearAllMocks();

        hoisted.mutationPendingState.fromFile = false;
        hoisted.mutationPendingState.fromInstructions = false;

        queryClient = new QueryClient({defaultOptions: {mutations: {retry: false}, queries: {retry: false}}});

        invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');
    });

    const renderUploadForm = (onSuccess?: (createdSkillId: string | null) => void) => {
        const wrapper = ({children}: {children: ReactNode}) => (
            <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
        );

        return renderHook(() => useAiSkillUploadForm({onSuccess}), {wrapper});
    };

    describe('file selection', () => {
        it('accepts .zip, .skill and .md files regardless of extension case', () => {
            const {result} = renderUploadForm();

            const acceptedFiles = [createFile('a.ZIP'), createFile('b.skill'), createFile('c.Md')];

            act(() => {
                result.current.handleFileInputChange(createChangeEvent(acceptedFiles));
            });

            expect(result.current.selectedFiles).toEqual(acceptedFiles);
            expect(toast.error).not.toHaveBeenCalled();
            expect(result.current.acceptedExtensions).toEqual(['.zip', '.skill', '.md']);
        });

        it('rejects unsupported files with a toast and keeps the supported ones', () => {
            const {result} = renderUploadForm();

            const supportedFile = createFile('runbook.md');

            act(() => {
                result.current.handleFileInputChange(
                    createChangeEvent([createFile('image.png'), supportedFile, createFile('README')])
                );
            });

            expect(result.current.selectedFiles).toEqual([supportedFile]);
            expect(toast.error).toHaveBeenCalledTimes(2);
            expect(toast.error).toHaveBeenCalledWith('Unsupported file type: image.png', {
                description: 'Accepted formats: .zip, .skill, .md',
            });
            expect(toast.error).toHaveBeenCalledWith('Unsupported file type: README', expect.anything());
        });

        it('appends to the existing selection and clears the file input value', () => {
            const {result} = renderUploadForm();

            const fileInput = document.createElement('input');

            fileInput.type = 'file';

            result.current.fileInputRef.current = fileInput;

            const firstFile = createFile('first.md');
            const secondFile = createFile('second.zip');

            act(() => {
                result.current.handleFileInputChange(createChangeEvent([firstFile]));
            });

            act(() => {
                result.current.handleFileInputChange(createChangeEvent([secondFile]));
            });

            expect(result.current.selectedFiles).toEqual([firstFile, secondFile]);
            expect(fileInput.value).toBe('');
        });

        it('ignores a change event without files', () => {
            const {result} = renderUploadForm();

            act(() => {
                result.current.handleFileInputChange(createChangeEvent([]));
            });

            expect(result.current.selectedFiles).toEqual([]);
        });

        it('removes the file at the given index', () => {
            const {result} = renderUploadForm();

            const files = [createFile('a.md'), createFile('b.md'), createFile('c.md')];

            act(() => {
                result.current.handleFileInputChange(createChangeEvent(files));
            });

            act(() => {
                result.current.handleRemoveFile(1);
            });

            expect(result.current.selectedFiles).toEqual([files[0], files[2]]);
        });
    });

    describe('drag and drop', () => {
        it('activates on drag over and deactivates on drag leave', () => {
            const {result} = renderUploadForm();

            const dragOverEvent = createDragEvent([]);

            act(() => {
                result.current.handleDragOver(dragOverEvent);
            });

            expect(dragOverEvent.preventDefault).toHaveBeenCalled();
            expect(result.current.dragActive).toBe(true);

            act(() => {
                result.current.handleDragLeave();
            });

            expect(result.current.dragActive).toBe(false);
        });

        it('adds dropped files and deactivates the drop zone', () => {
            const {result} = renderUploadForm();

            const droppedFile = createFile('dropped.skill');

            act(() => {
                result.current.handleDragOver(createDragEvent([]));
            });

            const dropEvent = createDragEvent([droppedFile, createFile('notes.txt')]);

            act(() => {
                result.current.handleDrop(dropEvent);
            });

            expect(dropEvent.preventDefault).toHaveBeenCalled();
            expect(result.current.dragActive).toBe(false);
            expect(result.current.selectedFiles).toEqual([droppedFile]);
        });

        it('ignores a drop without files', () => {
            const {result} = renderUploadForm();

            act(() => {
                result.current.handleDrop(createDragEvent([]));
            });

            expect(result.current.selectedFiles).toEqual([]);
            expect(toast.error).not.toHaveBeenCalled();
        });
    });

    describe('handleUpload', () => {
        it('asks for a file when nothing is selected', async () => {
            const onSuccess = vi.fn();

            const {result} = renderUploadForm(onSuccess);

            await act(async () => {
                await result.current.handleUpload();
            });

            expect(toast.error).toHaveBeenCalledWith('Please select at least one file to upload');
            expect(hoisted.createAiSkillMutateAsync).not.toHaveBeenCalled();
            expect(hoisted.createAiSkillFromInstructionsMutateAsync).not.toHaveBeenCalled();
            expect(onSuccess).not.toHaveBeenCalled();
        });

        it('creates a skill from the instructions of a markdown file', async () => {
            hoisted.createAiSkillFromInstructionsMutateAsync.mockResolvedValue({
                createAiSkillFromInstructions: {id: 'skill-1'},
            });

            const onSuccess = vi.fn();

            const {result} = renderUploadForm(onSuccess);

            act(() => {
                result.current.handleFileInputChange(
                    createChangeEvent([createFile('billing.runbook.md', '# Refund steps')])
                );
            });

            await act(async () => {
                await result.current.handleUpload();
            });

            expect(hoisted.createAiSkillFromInstructionsMutateAsync).toHaveBeenCalledWith({
                instructions: '# Refund steps',
                name: 'billing.runbook',
            });
            expect(hoisted.createAiSkillMutateAsync).not.toHaveBeenCalled();
            expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['aiSkills']});
            expect(onSuccess).toHaveBeenCalledWith('skill-1');
            expect(result.current.selectedFiles).toEqual([]);
        });

        it('uploads an archive as base64 file bytes', async () => {
            hoisted.createAiSkillMutateAsync.mockResolvedValue({createAiSkill: {id: 'skill-2'}});

            const onSuccess = vi.fn();

            const {result} = renderUploadForm(onSuccess);

            act(() => {
                result.current.handleFileInputChange(createChangeEvent([createFile('archive.zip', 'hello')]));
            });

            await act(async () => {
                await result.current.handleUpload();
            });

            expect(hoisted.createAiSkillMutateAsync).toHaveBeenCalledWith({
                fileBytes: btoa('hello'),
                filename: 'archive.zip',
                name: 'archive',
            });
            expect(onSuccess).toHaveBeenCalledWith('skill-2');
        });

        it('reports no single created id when several skills were created', async () => {
            hoisted.createAiSkillFromInstructionsMutateAsync
                .mockResolvedValueOnce({createAiSkillFromInstructions: {id: 'skill-1'}})
                .mockResolvedValueOnce({createAiSkillFromInstructions: {id: 'skill-2'}});

            const onSuccess = vi.fn();

            const {result} = renderUploadForm(onSuccess);

            act(() => {
                result.current.handleFileInputChange(createChangeEvent([createFile('a.md'), createFile('b.md')]));
            });

            await act(async () => {
                await result.current.handleUpload();
            });

            expect(onSuccess).toHaveBeenCalledTimes(1);
            expect(onSuccess).toHaveBeenCalledWith(null);
            expect(invalidateQueriesSpy).toHaveBeenCalledTimes(1);
        });

        it('reports a partial failure and does not call onSuccess', async () => {
            hoisted.createAiSkillFromInstructionsMutateAsync
                .mockResolvedValueOnce({createAiSkillFromInstructions: {id: 'skill-1'}})
                .mockRejectedValueOnce(new Error('Invalid frontmatter'));

            const onSuccess = vi.fn();

            const {result} = renderUploadForm(onSuccess);

            const failingFile = createFile('broken.md');

            act(() => {
                result.current.handleFileInputChange(createChangeEvent([createFile('good.md'), failingFile]));
            });

            await act(async () => {
                await result.current.handleUpload();
            });

            expect(toast.error).toHaveBeenCalledWith('Failed to create skill: broken.md', {
                description: 'Invalid frontmatter',
            });
            expect(toast.error).toHaveBeenCalledWith('1 of 2 skills created, 1 failed. Fix issues and retry.');
            expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['aiSkills']});
            expect(onSuccess).not.toHaveBeenCalled();
            expect(result.current.selectedFiles).toEqual([failingFile]);
        });

        it('uses a generic description when the failure is not an Error', async () => {
            hoisted.createAiSkillMutateAsync.mockRejectedValueOnce('boom');

            const {result} = renderUploadForm();

            act(() => {
                result.current.handleFileInputChange(createChangeEvent([createFile('bad.skill')]));
            });

            await act(async () => {
                await result.current.handleUpload();
            });

            expect(toast.error).toHaveBeenCalledWith('Failed to create skill: bad.skill', {
                description: 'An unexpected error occurred',
            });
            expect(toast.error).toHaveBeenCalledWith('0 of 1 skills created, 1 failed. Fix issues and retry.');
        });

        it('does not create the already-created skills again when retrying after a partial failure', async () => {
            hoisted.createAiSkillFromInstructionsMutateAsync
                .mockResolvedValueOnce({createAiSkillFromInstructions: {id: 'skill-1'}})
                .mockRejectedValueOnce(new Error('Invalid frontmatter'))
                .mockResolvedValueOnce({createAiSkillFromInstructions: {id: 'skill-2'}});

            const onSuccess = vi.fn();

            const {result} = renderUploadForm(onSuccess);

            act(() => {
                result.current.handleFileInputChange(
                    createChangeEvent([createFile('good.md', 'good'), createFile('broken.md', 'broken')])
                );
            });

            await act(async () => {
                await result.current.handleUpload();
            });

            await act(async () => {
                await result.current.handleUpload();
            });

            expect(hoisted.createAiSkillFromInstructionsMutateAsync).toHaveBeenCalledTimes(3);
            expect(hoisted.createAiSkillFromInstructionsMutateAsync).toHaveBeenNthCalledWith(3, {
                instructions: 'broken',
                name: 'broken',
            });
            expect(onSuccess).toHaveBeenCalledTimes(1);
            expect(onSuccess).toHaveBeenCalledWith('skill-2');
            expect(result.current.selectedFiles).toEqual([]);
        });
    });

    describe('isUploadPending', () => {
        it('is false while neither mutation is pending', () => {
            const {result} = renderUploadForm();

            expect(result.current.isUploadPending).toBe(false);
        });

        it('is true while the archive upload is pending', () => {
            hoisted.mutationPendingState.fromFile = true;

            const {result} = renderUploadForm();

            expect(result.current.isUploadPending).toBe(true);
        });

        it('is true while the instructions upload is pending', () => {
            hoisted.mutationPendingState.fromInstructions = true;

            const {result} = renderUploadForm();

            expect(result.current.isUploadPending).toBe(true);
        });
    });
});
