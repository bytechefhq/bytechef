import {fireEvent, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

import MarkdownEditor from './MarkdownEditor';

describe('MarkdownEditor', () => {
    it('renders markdown as formatted content rather than raw markup', () => {
        render(<MarkdownEditor ariaLabel="Instructions" value={'# Heading\n\nSome **bold** text.'} />);

        const editable = screen.getByLabelText('Instructions');

        expect(editable.querySelector('h1')?.textContent).toBe('Heading');
        expect(editable.querySelector('strong')?.textContent).toBe('bold');
        expect(editable.textContent).not.toContain('**');
    });

    it('renders markdown lists as list items', () => {
        render(<MarkdownEditor ariaLabel="Instructions" value={'- first\n- second'} />);

        const items = screen.getByLabelText('Instructions').querySelectorAll('li');

        expect(Array.from(items).map((item) => item.textContent)).toEqual(['first', 'second']);
    });

    it('exposes the placeholder on an empty document', () => {
        render(<MarkdownEditor ariaLabel="Instructions" placeholder="Describe the agent" value="" />);

        const paragraph = screen.getByLabelText('Instructions').querySelector('p');

        expect(paragraph?.getAttribute('data-placeholder')).toBe('Describe the agent');
    });

    it('replaces its content when the value changes while unfocused', () => {
        const {rerender} = render(<MarkdownEditor ariaLabel="Instructions" value="First" />);

        expect(screen.getByLabelText('Instructions').textContent).toBe('First');

        rerender(<MarkdownEditor ariaLabel="Instructions" value="Second" />);

        expect(screen.getByLabelText('Instructions').textContent).toBe('Second');
    });

    it('reports markdown, not html, when the editor blurs', () => {
        const handleBlur = vi.fn();

        render(<MarkdownEditor ariaLabel="Instructions" onBlur={handleBlur} value={'# Heading'} />);

        fireEvent.blur(screen.getByLabelText('Instructions'));

        expect(handleBlur).toHaveBeenCalledWith('# Heading');
    });

    it('turns the block under the cursor into a heading from the toolbar', async () => {
        const user = userEvent.setup();

        render(<MarkdownEditor ariaLabel="Instructions" value="Plain line" />);

        await user.click(screen.getByRole('button', {name: 'Heading 1'}));

        expect(screen.getByLabelText('Instructions').querySelector('h1')?.textContent).toBe('Plain line');
    });

    it('turns the block under the cursor into a list from the toolbar', async () => {
        const user = userEvent.setup();

        render(<MarkdownEditor ariaLabel="Instructions" value="Plain line" />);

        await user.click(screen.getByRole('button', {name: 'Bulleted list'}));

        expect(screen.getByLabelText('Instructions').querySelector('li')?.textContent).toBe('Plain line');
    });

    it('marks the toolbar control pressed for the block under the cursor', async () => {
        const user = userEvent.setup();

        render(<MarkdownEditor ariaLabel="Instructions" value="Plain line" />);

        const headingToggle = screen.getByRole('button', {name: 'Heading 1'});

        expect(headingToggle).toHaveAttribute('aria-pressed', 'false');

        await user.click(headingToggle);

        expect(headingToggle).toHaveAttribute('aria-pressed', 'true');
    });

    it('shows the markdown source when the source control is pressed', async () => {
        const user = userEvent.setup();

        render(<MarkdownEditor ariaLabel="Instructions" value={'# Heading'} />);

        await user.click(screen.getByRole('button', {name: 'Show Markdown source'}));

        expect(screen.getByLabelText('Instructions')).toHaveValue('# Heading');
    });

    it('disables the formatting controls while the source is showing', async () => {
        const user = userEvent.setup();

        render(<MarkdownEditor ariaLabel="Instructions" value={'# Heading'} />);

        await user.click(screen.getByRole('button', {name: 'Show Markdown source'}));

        expect(screen.getByRole('button', {name: 'Heading 1'})).toBeDisabled();
    });

    it('renders edits made in the markdown source once the source is hidden again', async () => {
        const user = userEvent.setup();

        render(<MarkdownEditor ariaLabel="Instructions" value={'# Heading'} />);

        await user.click(screen.getByRole('button', {name: 'Show Markdown source'}));

        const source = screen.getByLabelText('Instructions');

        await user.clear(source);
        await user.type(source, '## Changed');
        await user.click(screen.getByRole('button', {name: 'Show formatted text'}));

        expect(screen.getByLabelText('Instructions').querySelector('h2')?.textContent).toBe('Changed');
    });

    it('reports the raw text on blur while the source is showing', async () => {
        const handleBlur = vi.fn();
        const user = userEvent.setup();

        render(<MarkdownEditor ariaLabel="Instructions" onBlur={handleBlur} value={'# Heading'} />);

        await user.click(screen.getByRole('button', {name: 'Show Markdown source'}));

        const source = screen.getByLabelText('Instructions');

        await user.clear(source);
        await user.type(source, '*   loose bullet');

        fireEvent.blur(source);

        expect(handleBlur).toHaveBeenCalledWith('*   loose bullet');
    });
});
