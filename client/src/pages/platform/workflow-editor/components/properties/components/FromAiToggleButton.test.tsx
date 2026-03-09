import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import FromAiToggleButton from './FromAiToggleButton';

function renderWithTooltip(ui: React.ReactElement) {
    return render(<TooltipProvider>{ui}</TooltipProvider>);
}

describe('FromAiToggleButton', () => {
    it('renders the sparkle icon when isFromAi is false', () => {
        renderWithTooltip(<FromAiToggleButton isFromAi={false} onToggle={vi.fn()} />);

        const button = screen.getByRole('button');

        expect(button.querySelector('.lucide-sparkles')).toBeInTheDocument();
    });

    it('renders the dismiss icon when isFromAi is true', () => {
        renderWithTooltip(<FromAiToggleButton isFromAi={true} onToggle={vi.fn()} />);

        const button = screen.getByRole('button');

        expect(button.querySelector('.lucide-x')).toBeInTheDocument();
    });

    it('calls onToggle with true when sparkle button is clicked', async () => {
        const handleToggle = vi.fn();

        renderWithTooltip(<FromAiToggleButton isFromAi={false} onToggle={handleToggle} />);

        await userEvent.click(screen.getByRole('button'));

        expect(handleToggle).toHaveBeenCalledWith(true);
    });

    it('calls onToggle with false when dismiss button is clicked', async () => {
        const handleToggle = vi.fn();

        renderWithTooltip(<FromAiToggleButton isFromAi={true} onToggle={handleToggle} />);

        await userEvent.click(screen.getByRole('button'));

        expect(handleToggle).toHaveBeenCalledWith(false);
    });

    it('renders buttons with type="button" to prevent form submission', () => {
        const {rerender} = renderWithTooltip(<FromAiToggleButton isFromAi={false} onToggle={vi.fn()} />);

        expect(screen.getByRole('button')).toHaveAttribute('type', 'button');

        rerender(
            <TooltipProvider>
                <FromAiToggleButton isFromAi={true} onToggle={vi.fn()} />
            </TooltipProvider>
        );

        expect(screen.getByRole('button')).toHaveAttribute('type', 'button');
    });
});
