import TooltipTriggerIcon from '@/components/TooltipTriggerIcon/TooltipTriggerIcon';
import {Tooltip, TooltipContent, TooltipProvider} from '@/components/ui/tooltip';
import {render, screen} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {InfoIcon} from 'lucide-react';
import {expect, it} from 'vitest';

const renderTrigger = () =>
    render(
        <TooltipProvider>
            <Tooltip>
                <TooltipTriggerIcon label="Threshold">
                    <InfoIcon className="size-3.5" />
                </TooltipTriggerIcon>

                <TooltipContent>How close a match has to be to count.</TooltipContent>
            </Tooltip>
        </TooltipProvider>
    );

it('exposes the icon as a named button', () => {
    renderTrigger();

    expect(screen.getByRole('button', {name: 'Threshold'})).toBeInTheDocument();
});

it('reaches the trigger by keyboard and reveals the tooltip on focus', async () => {
    renderTrigger();

    await userEvent.tab();

    expect(screen.getByRole('button', {name: 'Threshold'})).toHaveFocus();

    expect(await screen.findAllByText('How close a match has to be to count.')).not.toHaveLength(0);
});

it('is a non-submitting button', () => {
    renderTrigger();

    expect(screen.getByRole('button', {name: 'Threshold'})).toHaveAttribute('type', 'button');
});
