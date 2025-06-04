import {render, screen, fireEvent} from '@testing-library/react';
import '@testing-library/jest-dom';
import Dialog from './Dialog';

const mockIntegration = {
    icon: '<svg viewBox="0 0 24 24"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"></path></svg>',
    description: 'Test integration description',
    name: 'Test Integration',
};

describe('Dialog', () => {
    const closeDialog = jest.fn();
    const handleContinue = jest.fn();
    const handleSubmit = jest.fn();

    it('renders when isOpen is true', () => {
        render(
            <Dialog
                closeDialog={closeDialog}
                dialogStep="initial"
                handleContinue={handleContinue}
                handleSubmit={handleSubmit}
                integration={mockIntegration}
                isOpen={true}
            />
        );

        expect(screen.getByText('Create Connection')).toBeInTheDocument();
    });

    it('does not render when isOpen is false', () => {
        render(
            <Dialog
                closeDialog={closeDialog}
                dialogStep="initial"
                handleContinue={handleContinue}
                handleSubmit={handleSubmit}
                integration={mockIntegration}
                isOpen={false}
            />
        );

        expect(screen.queryByText('Create Connection')).not.toBeInTheDocument();
    });

    it('calls closeDialog when clicking the overlay', () => {
        render(
            <Dialog
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
