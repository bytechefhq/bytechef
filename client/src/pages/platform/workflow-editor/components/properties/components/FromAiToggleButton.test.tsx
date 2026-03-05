import {render, screen, userEvent} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import FromAiToggleButton from './FromAiToggleButton';

describe('FromAiToggleButton', () => {
    it('renders the sparkle button when isFromAi is false', () => {
        render(<FromAiToggleButton isFromAi={false} onToggle={vi.fn()} />);

        expect(screen.getByTitle('Generate content with AI')).toBeInTheDocument();
    });

    it('renders the dismiss button when isFromAi is true', () => {
        render(<FromAiToggleButton isFromAi={true} onToggle={vi.fn()} />);

        expect(screen.getByTitle('Customize AI generation')).toBeInTheDocument();
    });

    it('calls onToggle with true when sparkle button is clicked', async () => {
        const handleToggle = vi.fn();

        render(<FromAiToggleButton isFromAi={false} onToggle={handleToggle} />);

        await userEvent.click(screen.getByTitle('Generate content with AI'));

        expect(handleToggle).toHaveBeenCalledWith(true);
    });

    it('calls onToggle with false when dismiss button is clicked', async () => {
        const handleToggle = vi.fn();

        render(<FromAiToggleButton isFromAi={true} onToggle={handleToggle} />);

        await userEvent.click(screen.getByTitle('Customize AI generation'));

        expect(handleToggle).toHaveBeenCalledWith(false);
    });

    it('renders buttons with type="button" to prevent form submission', () => {
        const {rerender} = render(<FromAiToggleButton isFromAi={false} onToggle={vi.fn()} />);

        expect(screen.getByRole('button')).toHaveAttribute('type', 'button');

        rerender(<FromAiToggleButton isFromAi={true} onToggle={vi.fn()} />);

        expect(screen.getByRole('button')).toHaveAttribute('type', 'button');
    });
});
