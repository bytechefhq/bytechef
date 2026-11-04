import {TooltipProvider} from '@/components/ui/tooltip';
import PropertyFormulaSwitch from '@/pages/platform/workflow-editor/components/properties/components/PropertyFormulaSwitch';
import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

describe('PropertyFormulaSwitch', () => {
    it('renders a checked, disabled switch that ignores clicks when locked', () => {
        const handleClick = vi.fn();

        render(
            <TooltipProvider>
                <PropertyFormulaSwitch disabled formulaMode handleClick={handleClick} />
            </TooltipProvider>
        );

        const formulaSwitch = screen.getByRole('switch');

        expect(formulaSwitch).toBeChecked();
        expect(formulaSwitch).toBeDisabled();

        fireEvent.click(formulaSwitch);

        expect(handleClick).not.toHaveBeenCalled();
    });

    it('toggles when enabled', () => {
        const handleClick = vi.fn();

        render(
            <TooltipProvider>
                <PropertyFormulaSwitch formulaMode={false} handleClick={handleClick} />
            </TooltipProvider>
        );

        fireEvent.click(screen.getByRole('switch'));

        expect(handleClick).toHaveBeenCalledTimes(1);
    });
});
