import {TooltipProvider} from '@/components/ui/tooltip';
import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import PropertyDynamicSwitch from './PropertyDynamicSwitch';

describe('PropertyDynamicSwitch', () => {
    it('is labelled Dynamic and reflects the mode', () => {
        render(
            <TooltipProvider>
                <PropertyDynamicSwitch dynamic handleClick={vi.fn()} />
            </TooltipProvider>
        );

        expect(screen.getByRole('switch', {name: 'Dynamic'})).toBeChecked();
    });

    it('calls handleClick when toggled', () => {
        const handleClick = vi.fn();

        render(
            <TooltipProvider>
                <PropertyDynamicSwitch dynamic={false} handleClick={handleClick} />
            </TooltipProvider>
        );

        fireEvent.click(screen.getByRole('switch', {name: 'Dynamic'}));

        expect(handleClick).toHaveBeenCalledTimes(1);
    });
});
