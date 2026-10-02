import {FileTreeNodeI} from '@/pages/automation/ai/skills/hooks/useAiSkillDetail';
import useAiSkillDetailToolbarStore from '@/pages/automation/ai/skills/stores/useAiSkillDetailToolbarStore';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import * as graphql from '@/shared/middleware/graphql';
import {act, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AiSkillDetail from '../AiSkillDetail';

const handleAddFile = vi.fn();
const handleDelete = vi.fn();
const handleRemoveFile = vi.fn();

const aiSkillDetailHookMock = vi.fn(() => ({
    editorLanguage: 'plaintext',
    fileContent: '',
    filePaths: [] as string[],
    fileTree: [] as FileTreeNodeI[],
    handleAddFile,
    handleDelete,
    handleDownload: vi.fn(),
    handleFileSelect: vi.fn(),
    handleRemoveFile,
    handleSaveContent: vi.fn(),
    isFileContentLoading: false,
    isMarkdown: false,
    isSaving: false,
    selectedFilePath: null,
    skill: undefined as {id: string; name: string} | undefined,
}));

const mockLoadedSkill = () => {
    const defaultHookResult = aiSkillDetailHookMock();

    aiSkillDetailHookMock.mockReturnValue({
        ...defaultHookResult,
        filePaths: ['notes.txt'],
        fileTree: [{children: [], name: 'notes.txt', path: 'notes.txt', type: 'file'}],
        skill: {id: '7', name: 'Test skill'},
    });
};

vi.mock('@/pages/automation/ai/skills/hooks/useAiSkillDetail', async () => {
    const actual = await vi.importActual<typeof import('@/pages/automation/ai/skills/hooks/useAiSkillDetail')>(
        '@/pages/automation/ai/skills/hooks/useAiSkillDetail'
    );

    return {
        ...actual,
        default: () => aiSkillDetailHookMock(),
    };
});

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<typeof import('@/shared/middleware/graphql')>('@/shared/middleware/graphql');

    return {
        ...actual,
        useAiSkillFileContentQuery: vi.fn(() => ({data: undefined, isLoading: false})),
        useAiSkillFilePathsQuery: vi.fn(() => ({data: {aiSkillFilePaths: []}})),
        useAiSkillQuery: vi.fn(() => ({data: {aiSkill: {description: null, id: '7', name: 'Test skill'}}})),
    };
});

describe('AiSkillDetail', () => {
    beforeEach(() => {
        useCopilotPanelStore.setState({copilotPanelOpen: false});
        vi.clearAllMocks();
        aiSkillDetailHookMock.mockReset();
    });

    it('closes the copilot panel when the detail view unmounts', () => {
        const {unmount} = render(<AiSkillDetail />);

        useCopilotPanelStore.getState().setCopilotPanelOpen(true);

        unmount();

        expect(useCopilotPanelStore.getState().copilotPanelOpen).toBe(false);
    });

    it('fetches the skill identified by the skillId prop instead of the route-driven store', () => {
        render(<AiSkillDetail skillId="7" />);

        expect(graphql.useAiSkillQuery).toHaveBeenCalledWith({id: '7'});
        expect(aiSkillDetailHookMock).not.toHaveBeenCalled();
    });

    describe('with a loaded skill', () => {
        beforeEach(() => {
            mockLoadedSkill();
        });

        it('keeps the file when removal is cancelled', async () => {
            render(<AiSkillDetail />);

            await userEvent.click(screen.getByRole('button', {name: 'Remove notes.txt'}));

            expect(screen.getByRole('alertdialog')).toHaveTextContent('permanently remove "notes.txt"');

            await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

            expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
            expect(handleRemoveFile).not.toHaveBeenCalled();
        });

        it('adds a file entered in the add file dialog', async () => {
            render(<AiSkillDetail />);

            await userEvent.click(screen.getByRole('button', {name: 'Add file'}));

            await userEvent.type(screen.getByLabelText('File Path'), 'scripts/extract.py');

            await userEvent.click(screen.getByRole('button', {name: 'Add'}));

            expect(handleAddFile).toHaveBeenCalledWith('scripts/extract.py');
        });

        it('opens the delete skill dialog from the toolbar and deletes on confirm', async () => {
            render(<AiSkillDetail />);

            act(() => {
                useAiSkillDetailToolbarStore.getState().handlers?.onDelete();
            });

            await userEvent.click(screen.getByRole('button', {name: 'Delete'}));

            expect(handleDelete).toHaveBeenCalledTimes(1);
            expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
        });
    });
});
