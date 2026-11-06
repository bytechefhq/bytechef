import {TooltipProvider} from '@/components/ui/tooltip';
import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import DeletePropertyButton from './DeletePropertyButton';

const renderDeletePropertyButton = (onClick = vi.fn()) =>
    render(
        <TooltipProvider>
            <DeletePropertyButton onClick={onClick} propertyName="brojEtaza" />
        </TooltipProvider>
    );

describe('DeletePropertyButton', () => {
    it('renders a red outlined button on a neutral background', () => {
        renderDeletePropertyButton();

        const deleteButton = screen.getByRole('button', {name: 'Delete brojEtaza'});

        expect(deleteButton).toHaveClass(
            'border',
            'border-stroke-destructive-primary',
            'text-content-destructive-primary'
        );
        expect(deleteButton).not.toHaveClass('text-content-onsurface-primary');
    });

    it('matches the height of the small Formula switch and keeps some space from it', () => {
        renderDeletePropertyButton();

        const deleteButton = screen.getByRole('button', {name: 'Delete brojEtaza'});

        expect(deleteButton).toHaveClass('ml-1', 'size-3.5', 'p-0', '[&_svg]:size-2.5');
        expect(deleteButton).not.toHaveClass('size-5', 'p-1', '[&_svg]:size-3');
    });

    it('calls onClick when clicked', () => {
        const onClick = vi.fn();

        renderDeletePropertyButton(onClick);

        fireEvent.click(screen.getByRole('button', {name: 'Delete brojEtaza'}));

        expect(onClick).toHaveBeenCalledTimes(1);
    });
});
