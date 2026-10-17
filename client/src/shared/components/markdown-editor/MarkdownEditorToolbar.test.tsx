import {act, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {Editor} from '@tiptap/react';
import {StarterKit} from '@tiptap/starter-kit';
import {afterEach, describe, expect, it, vi} from 'vitest';

import MarkdownEditorToolbar from './MarkdownEditorToolbar';

describe('MarkdownEditorToolbar', () => {
    let editor: Editor | undefined;

    const createEditor = (content: string) => {
        editor = new Editor({content: `<p>${content}</p>`, extensions: [StarterKit]});

        act(() => {
            editor?.commands.setTextSelection({from: 1, to: content.length + 1});
        });

        return editor;
    };

    afterEach(() => {
        editor?.destroy();

        editor = undefined;
    });

    it('renders nothing without an editor', () => {
        const {container} = render(<MarkdownEditorToolbar editor={null} isRaw={false} onToggleRaw={vi.fn()} />);

        expect(container).toBeEmptyDOMElement();
    });

    it.each([
        ['Bold', 'bold', undefined],
        ['Italic', 'italic', undefined],
        ['Strikethrough', 'strike', undefined],
        ['Inline code', 'code', undefined],
        ['Heading 1', 'heading', {level: 1}],
        ['Heading 2', 'heading', {level: 2}],
        ['Heading 3', 'heading', {level: 3}],
        ['Bulleted list', 'bulletList', undefined],
        ['Numbered list', 'orderedList', undefined],
        ['Quote', 'blockquote', undefined],
        ['Code block', 'codeBlock', undefined],
    ])('applies %s to the selection', async (label, name, attributes) => {
        const user = userEvent.setup();
        const currentEditor = createEditor('Plain line');

        render(<MarkdownEditorToolbar editor={currentEditor} isRaw={false} onToggleRaw={vi.fn()} />);

        const toggle = screen.getByRole('button', {name: label});

        expect(toggle).toHaveAttribute('aria-pressed', 'false');

        await user.click(toggle);

        expect(currentEditor.isActive(name, attributes)).toBe(true);
        expect(toggle).toHaveAttribute('aria-pressed', 'true');
    });

    it('reports the source toggle with the next raw state', async () => {
        const handleToggleRaw = vi.fn();
        const user = userEvent.setup();

        render(<MarkdownEditorToolbar editor={createEditor('Text')} isRaw onToggleRaw={handleToggleRaw} />);

        await user.click(screen.getByRole('button', {name: 'Show formatted text'}));

        expect(handleToggleRaw).toHaveBeenCalledWith(false);
    });
});
