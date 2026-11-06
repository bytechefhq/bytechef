import {TooltipProvider} from '@/components/ui/tooltip';
import OpenInButton from '@/shared/components/OpenInButton';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

describe('OpenInButton', () => {
    it('opens the target without triggering the row it sits in', async () => {
        const user = userEvent.setup();
        const onClick = vi.fn();
        const onRowClick = vi.fn();

        render(
            <TooltipProvider>
                <div onClick={onRowClick}>
                    <OpenInButton label="Open project" onClick={onClick} />
                </div>
            </TooltipProvider>
        );

        await user.click(screen.getByRole('button', {name: 'Open project'}));

        expect(onClick).toHaveBeenCalledTimes(1);
        expect(onRowClick).not.toHaveBeenCalled();
    });

    it('keeps the icon small by sizing both the icon and the button rule', () => {
        render(
            <TooltipProvider>
                <OpenInButton label="Open project" onClick={vi.fn()} />
            </TooltipProvider>
        );

        const button = screen.getByRole('button', {name: 'Open project'});

        expect(button).toHaveClass('[&_svg]:size-3');
        expect(button.querySelector('svg')).toHaveClass('size-3');
    });
});
