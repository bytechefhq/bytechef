import {TooltipProvider} from '@/components/ui/tooltip';
import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import PropertyFormulaSwitch from './PropertyFormulaSwitch';

describe('PropertyFormulaSwitch', () => {
    it('is labelled Formula and reflects the mode', () => {
        render(
            <TooltipProvider>
                <PropertyFormulaSwitch formulaMode handleClick={vi.fn()} />
            </TooltipProvider>
        );

        expect(screen.getByRole('switch', {name: 'Formula'})).toBeChecked();
    });

    it('calls handleClick when toggled', () => {
        const handleClick = vi.fn();

        render(
            <TooltipProvider>
                <PropertyFormulaSwitch formulaMode={false} handleClick={handleClick} />
            </TooltipProvider>
        );

        fireEvent.click(screen.getByRole('switch', {name: 'Formula'}));

        expect(handleClick).toHaveBeenCalledTimes(1);
    });
});
