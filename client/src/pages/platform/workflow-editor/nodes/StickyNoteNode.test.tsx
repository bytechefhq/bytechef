import {fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import StickyNoteNode from './StickyNoteNode';

const {deleteStickyNoteMock, updateStickyNoteMock} = vi.hoisted(() => ({
    deleteStickyNoteMock: vi.fn(),
    updateStickyNoteMock: vi.fn(),
}));

vi.mock('@xyflow/react', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@xyflow/react')>();

    return {
        ...actual,
        NodeResizer: () => null,
    };
});

vi.mock('../providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({updateWorkflowMutation: {mutate: vi.fn()}}),
}));

vi.mock('../utils/stickyNoteUtils', async (importOriginal) => {
    const actual = await importOriginal<typeof import('../utils/stickyNoteUtils')>();

    return {
        ...actual,
        deleteStickyNote: deleteStickyNoteMock,
        updateStickyNote: updateStickyNoteMock,
    };
});

describe('StickyNoteNode', () => {
    beforeEach(() => {
        deleteStickyNoteMock.mockClear();
        updateStickyNoteMock.mockClear();
    });

    it('should render the note content', () => {
        render(<StickyNoteNode data={{color: 'yellow', content: 'Remember the retry logic'}} id="stickyNote_1" />);

        expect(screen.getByText('Remember the retry logic')).toBeInTheDocument();
    });

    it('should show a placeholder for an empty editable note', () => {
        render(<StickyNoteNode data={{color: 'yellow', content: ''}} id="stickyNote_1" />);

        expect(screen.getByText('Double-click to add a note (Markdown supported)')).toBeInTheDocument();
    });

    it('should not show the placeholder in read-only mode', () => {
        render(<StickyNoteNode data={{color: 'yellow', content: '', readOnly: true}} id="stickyNote_1" />);

        expect(screen.queryByText('Double-click to add a note (Markdown supported)')).not.toBeInTheDocument();
    });

    it('should commit edited content on blur', () => {
        render(<StickyNoteNode data={{color: 'yellow', content: 'Old text'}} id="stickyNote_1" />);

        fireEvent.doubleClick(screen.getByText('Old text'));

        const textarea = screen.getByRole('textbox');

        fireEvent.change(textarea, {target: {value: 'New text'}});
        fireEvent.blur(textarea);

        expect(updateStickyNoteMock).toHaveBeenCalledTimes(1);
        expect(updateStickyNoteMock.mock.calls[0][0]).toMatchObject({
            id: 'stickyNote_1',
            patch: {content: 'New text'},
        });
    });

    it('should not save when the content is unchanged after editing', () => {
        render(<StickyNoteNode data={{color: 'yellow', content: 'Same text'}} id="stickyNote_1" />);

        fireEvent.doubleClick(screen.getByText('Same text'));

        fireEvent.blur(screen.getByRole('textbox'));

        expect(updateStickyNoteMock).not.toHaveBeenCalled();
    });

    it('should not enter edit mode in read-only mode', () => {
        render(<StickyNoteNode data={{color: 'yellow', content: 'Locked', readOnly: true}} id="stickyNote_1" />);

        fireEvent.doubleClick(screen.getByText('Locked'));

        expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
    });

    it('should hide the note toolbar in read-only mode', () => {
        render(<StickyNoteNode data={{color: 'yellow', content: 'Locked', readOnly: true}} id="stickyNote_1" />);

        expect(screen.queryByLabelText('Delete note')).not.toBeInTheDocument();
    });

    it('should delete the note from the toolbar', () => {
        render(<StickyNoteNode data={{color: 'yellow', content: 'Doomed'}} id="stickyNote_1" />);

        fireEvent.click(screen.getByLabelText('Delete note'));

        expect(deleteStickyNoteMock).toHaveBeenCalledTimes(1);
        expect(deleteStickyNoteMock.mock.calls[0][0]).toMatchObject({id: 'stickyNote_1'});
    });

    it('should change the note color from the toolbar', () => {
        render(<StickyNoteNode data={{color: 'yellow', content: 'Colorful'}} id="stickyNote_1" />);

        fireEvent.click(screen.getByLabelText('Set blue color'));

        expect(updateStickyNoteMock).toHaveBeenCalledTimes(1);
        expect(updateStickyNoteMock.mock.calls[0][0]).toMatchObject({
            id: 'stickyNote_1',
            patch: {color: 'blue'},
        });
    });

    it('should offer all seven preset colors', () => {
        render(<StickyNoteNode data={{color: 'yellow', content: ''}} id="stickyNote_1" />);

        for (const presetColor of ['blue', 'gray', 'green', 'orange', 'pink', 'purple', 'yellow']) {
            expect(screen.getByLabelText(`Set ${presetColor} color`)).toBeInTheDocument();
        }
    });

    it('should render markdown content', () => {
        render(
            <StickyNoteNode data={{color: 'yellow', content: '## Heading\n\nSome **bold** text'}} id="stickyNote_1" />
        );

        expect(screen.getByRole('heading', {level: 2, name: 'Heading'})).toBeInTheDocument();

        const boldElement = screen.getByText('bold');

        expect(boldElement.tagName).toBe('STRONG');
    });

    it('should render markdown links opening in a new tab', () => {
        render(
            <StickyNoteNode data={{color: 'yellow', content: '[docs](https://docs.bytechef.io)'}} id="stickyNote_1" />
        );

        const link = screen.getByRole('link', {name: 'docs'});

        expect(link).toHaveAttribute('href', 'https://docs.bytechef.io');
        expect(link).toHaveAttribute('target', '_blank');
        expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    });

    it('should not render raw HTML in markdown content', () => {
        render(
            <StickyNoteNode data={{color: 'yellow', content: '<script>alert(1)</script>plain'}} id="stickyNote_1" />
        );

        expect(document.querySelector('script')).toBeNull();
    });

    it('should render a youtube marker as an embedded player', () => {
        render(
            <StickyNoteNode
                data={{color: 'yellow', content: 'Watch this:\n\n@[youtube](dQw4w9WgXcQ)'}}
                id="stickyNote_1"
            />
        );

        expect(screen.getByText('Watch this:')).toBeInTheDocument();

        const iframe = screen.getByTitle('YouTube video player');

        expect(iframe).toHaveAttribute('src', 'https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ');
    });

    it('should not render an embed for an unresolvable youtube marker', () => {
        render(<StickyNoteNode data={{color: 'yellow', content: '@[youtube](not a video)'}} id="stickyNote_1" />);

        expect(screen.queryByTitle('YouTube video player')).not.toBeInTheDocument();
    });

    it('should apply a custom hex color as inline background', () => {
        render(<StickyNoteNode data={{color: '#123456', content: 'Custom'}} id="stickyNote_1" />);

        const container = screen.getByText('Custom').closest('[data-nodetype="stickyNote"]');

        expect(container).toHaveStyle({backgroundColor: '#123456'});
    });
});
