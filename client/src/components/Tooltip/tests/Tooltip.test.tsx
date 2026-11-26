import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it} from 'vitest';

import {TOOLTIP_DELAY_DURATION, Tooltip, TooltipContent, TooltipProvider, TooltipTrigger} from '../Tooltip';

const renderTooltip = (delayDuration?: number) =>
    render(
        <TooltipProvider>
            <Tooltip delayDuration={delayDuration}>
                <TooltipTrigger>Trigger</TooltipTrigger>

                <TooltipContent>Tooltip text</TooltipContent>
            </Tooltip>
        </TooltipProvider>
    );

const getTooltipContent = () => document.querySelector('[data-slot="tooltip-content"]');

describe('Tooltip', () => {
    it('does not open the tooltip immediately on hover', async () => {
        const user = userEvent.setup();

        renderTooltip();

        await user.hover(screen.getByText('Trigger'));

        expect(getTooltipContent()).toBeNull();

        await waitFor(() => expect(getTooltipContent()).toHaveTextContent('Tooltip text'), {
            timeout: TOOLTIP_DELAY_DURATION + 2000,
        });
    });

    it('opens the tooltip immediately when the delay is overridden', async () => {
        const user = userEvent.setup();

        renderTooltip(0);

        await user.hover(screen.getByText('Trigger'));

        await waitFor(() => expect(getTooltipContent()).toHaveTextContent('Tooltip text'), {timeout: 100});
    });
});
