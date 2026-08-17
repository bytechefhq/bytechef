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

// The whole point of the component: the previous idiom put a bare <svg> here, which is neither focusable nor
// exposed as a control, so the explanation was mouse-only and — where no aria-label was passed, which was most
// sites — absent from the accessibility tree entirely.
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

// Inside a form, an unspecified button type submits. These sit next to fields in dialogs, so a missing
// type="button" would turn "read the help text" into "save the dialog".
it('is a non-submitting button', () => {
    renderTrigger();

    expect(screen.getByRole('button', {name: 'Threshold'})).toHaveAttribute('type', 'button');
});
