import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {render} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AiSkillDetail from '../AiSkillDetail';

vi.mock('@/pages/automation/ai/skills/hooks/useAiSkillDetail', () => ({
    default: () => ({
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
    }),
}));

describe('AiSkillDetail', () => {
    beforeEach(() => {
        useCopilotPanelStore.setState({copilotPanelOpen: false});
    });

    it('closes the copilot panel when the detail view unmounts', () => {
        const {unmount} = render(<AiSkillDetail />);

        useCopilotPanelStore.getState().setCopilotPanelOpen(true);

        unmount();

        expect(useCopilotPanelStore.getState().copilotPanelOpen).toBe(false);
    });
});
