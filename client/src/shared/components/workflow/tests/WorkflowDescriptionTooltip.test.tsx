import {TooltipProvider} from '@/components/Tooltip/Tooltip';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it} from 'vitest';

import WorkflowDescriptionTooltip from '../WorkflowDescriptionTooltip';

const renderTooltip = (description?: string) =>
    render(
        <TooltipProvider>
            <WorkflowDescriptionTooltip description={description}>
                <span>Sync orders</span>
            </WorkflowDescriptionTooltip>
        </TooltipProvider>
    );

describe('WorkflowDescriptionTooltip', () => {
    it('renders the name without a tooltip when there is no description', () => {
        renderTooltip();

        expect(screen.getByText('Sync orders')).not.toHaveAttribute('data-slot', 'tooltip-trigger');
    });

    it('renders the name without a tooltip when the description is blank', () => {
        renderTooltip('   ');

        expect(screen.getByText('Sync orders')).not.toHaveAttribute('data-slot', 'tooltip-trigger');
    });

    it('shows the description after hovering the name', async () => {
        const user = userEvent.setup();

        renderTooltip('Copies new orders to the warehouse sheet');

        expect(screen.getByText('Sync orders')).toHaveAttribute('data-slot', 'tooltip-trigger');

        await user.hover(screen.getByText('Sync orders'));

        await waitFor(
            () =>
                expect(document.querySelector('[data-slot="tooltip-content"]')).toHaveTextContent(
                    'Copies new orders to the warehouse sheet'
                ),
            {timeout: 2500}
        );
    });

    it('does not open the tooltip immediately on hover', async () => {
        const user = userEvent.setup();

        renderTooltip('Copies new orders to the warehouse sheet');

        await user.hover(screen.getByText('Sync orders'));

        expect(document.querySelector('[data-slot="tooltip-content"]')).toBeNull();

        await waitFor(() => expect(document.querySelector('[data-slot="tooltip-content"]')).not.toBeNull(), {
            timeout: 2500,
        });
    });
});
