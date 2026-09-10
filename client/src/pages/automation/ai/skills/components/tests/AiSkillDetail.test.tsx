import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import * as graphql from '@/shared/middleware/graphql';
import {render} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AiSkillDetail from '../AiSkillDetail';

const aiSkillDetailHookMock = vi.fn(() => ({
    editorLanguage: 'plaintext',
    fileContent: '',
    fileTree: [],
    handleDelete: vi.fn(),
    handleDownload: vi.fn(),
    handleFileSelect: vi.fn(),
    handleSaveContent: vi.fn(),
    isFileContentLoading: false,
    isMarkdown: false,
    isSaving: false,
    selectedFilePath: null,
    skill: undefined,
}));

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
});
