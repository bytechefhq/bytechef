import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import PropertyCopilotPopover from './PropertyCopilotPopover';

const baseProps = {
    error: null,
    generatedValue: null,
    hasValue: false,
    onApply: vi.fn(),
    onGenerate: vi.fn(),
    pending: false,
};

describe('PropertyCopilotPopover', () => {
    it('submits the typed prompt via onGenerate', () => {
        const onGenerate = vi.fn();

        render(<PropertyCopilotPopover {...baseProps} onGenerate={onGenerate} />);

        fireEvent.change(screen.getByPlaceholderText(/describe/i), {target: {value: 'greet the user'}});
        fireEvent.click(screen.getByRole('button', {name: /generate/i}));

        expect(onGenerate).toHaveBeenCalledWith('greet the user');
    });

    it('disables generate while pending and shows a busy label', () => {
        render(<PropertyCopilotPopover {...baseProps} pending={true} />);

        expect(screen.getByRole('button', {name: /generating/i})).toBeDisabled();
    });

    it('renders an error message', () => {
        render(<PropertyCopilotPopover {...baseProps} error="boom" />);

        expect(screen.getByText('boom')).toBeInTheDocument();
    });

    it('previews the generated value and offers Insert when the field is empty', () => {
        const onApply = vi.fn();

        render(
            <PropertyCopilotPopover {...baseProps} generatedValue="generated text" hasValue={false} onApply={onApply} />
        );

        expect(screen.getByText('generated text')).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: /insert/i}));

        expect(onApply).toHaveBeenCalledWith('generated text');
    });

    it('offers Replace when the field already has a value', () => {
        render(<PropertyCopilotPopover {...baseProps} generatedValue="generated text" hasValue={true} />);

        expect(screen.getByRole('button', {name: /replace/i})).toBeInTheDocument();
    });

    it('labels the generate button as Regenerate once a value exists', () => {
        render(<PropertyCopilotPopover {...baseProps} generatedValue="generated text" />);

        expect(screen.getByRole('button', {name: /regenerate/i})).toBeInTheDocument();
    });
});
