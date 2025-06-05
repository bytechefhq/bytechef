import {render, screen, fireEvent} from '@testing-library/react';
import {describe, it, expect, vi} from 'vitest';
import ConnectDialog from './ConnectDialog';

const mockIntegration = {
    icon: '<svg viewBox="0 0 24 24"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"></path></svg>',
    description: 'Test integration description',
    name: 'Test Integration',
};

describe('Dialog', () => {
    const closeDialog = vi.fn();
    const handleContinue = vi.fn();
    const handleSubmit = vi.fn();

    it('renders when isOpen is true', () => {
        render(
            <ConnectDialog
                closeDialog={closeDialog}
                dialogStep="initial"
                handleContinue={handleContinue}
                handleSubmit={handleSubmit}
                integration={mockIntegration}
                isOpen={true}
            />
        );

        expect(screen.getByText('Create Connection')).toBeTruthy();
    });

    it('does not render when isOpen is false', () => {
        render(
            <ConnectDialog
                closeDialog={closeDialog}
                dialogStep="initial"
                handleContinue={handleContinue}
                handleSubmit={handleSubmit}
                integration={mockIntegration}
                isOpen={false}
            />
        );

        expect(screen.queryByText('Create Connection')).toBeNull();
    });

    it('calls closeDialog when clicking the overlay', () => {
        render(
            <ConnectDialog
                closeDialog={closeDialog}
                dialogStep="initial"
                handleContinue={handleContinue}
                handleSubmit={handleSubmit}
                integration={mockIntegration}
                isOpen={true}
            />
        );

        const overlay = screen.getByTestId('dialog-overlay');
        fireEvent.click(overlay);

        expect(closeDialog).toHaveBeenCalled();
    });
});
