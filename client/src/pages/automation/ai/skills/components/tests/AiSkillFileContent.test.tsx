import {render, resetAll, screen} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import {AiSkillFileContent} from '../AiSkillDetail';

const {monacoPropsMock} = vi.hoisted(() => ({monacoPropsMock: vi.fn()}));

vi.mock('@/shared/components/MonacoEditorWrapper', () => ({
    default: (props: {options?: {readOnly?: boolean}; value?: string}) => {
        monacoPropsMock(props);

        return <div data-testid="monaco-editor">{props.value}</div>;
    },
}));

vi.mock('@tiptap/react', () => ({
    EditorContent: () => <div data-testid="markdown-viewer" />,
    useEditor: () => ({
        commands: {setContent: vi.fn()},
        setEditable: vi.fn(),
        storage: {markdown: {getMarkdown: () => ''}},
    }),
}));

vi.mock('@tiptap/starter-kit', () => ({StarterKit: {configure: () => ({})}}));

vi.mock('tiptap-markdown', () => ({Markdown: {configure: () => ({})}}));

const defaultProps = {
    content: '# Title',
    editorLanguage: 'markdown',
    isFileContentLoading: false,
    onChange: vi.fn(),
    readOnly: false,
    selectedFilePath: 'SKILL.md',
    showMarkdownPreview: false,
};

describe('AiSkillFileContent', () => {
    afterEach(() => {
        resetAll();
    });

    it('prompts for a selection while no file is picked', () => {
        render(<AiSkillFileContent {...defaultProps} selectedFilePath={null} />);

        expect(screen.getByText('Select a file to view its contents')).toBeInTheDocument();
        expect(screen.queryByTestId('monaco-editor')).not.toBeInTheDocument();
    });

    it('renders neither viewer while the file content is loading', () => {
        render(<AiSkillFileContent {...defaultProps} isFileContentLoading />);

        expect(screen.queryByTestId('monaco-editor')).not.toBeInTheDocument();
        expect(screen.queryByTestId('markdown-viewer')).not.toBeInTheDocument();
    });

    it('renders the markdown preview when the file is markdown and preview is on', async () => {
        render(<AiSkillFileContent {...defaultProps} showMarkdownPreview />);

        expect(await screen.findByTestId('markdown-viewer')).toBeInTheDocument();
        expect(screen.queryByTestId('monaco-editor')).not.toBeInTheDocument();
    });

    it('falls back to the editor when preview is off', async () => {
        render(<AiSkillFileContent {...defaultProps} />);

        expect(await screen.findByTestId('monaco-editor')).toBeInTheDocument();
        expect(screen.queryByTestId('markdown-viewer')).not.toBeInTheDocument();
    });

    it('hands the editor the file content and a writable editor', async () => {
        render(<AiSkillFileContent {...defaultProps} />);

        await screen.findByTestId('monaco-editor');

        expect(monacoPropsMock).toHaveBeenCalledWith(
            expect.objectContaining({options: expect.objectContaining({readOnly: false}), value: '# Title'})
        );
    });

    it('locks the editor for a read-only caller', async () => {
        render(<AiSkillFileContent {...defaultProps} readOnly />);

        await screen.findByTestId('monaco-editor');

        expect(monacoPropsMock).toHaveBeenCalledWith(
            expect.objectContaining({options: expect.objectContaining({readOnly: true})})
        );
    });
});
