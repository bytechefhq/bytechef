import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import InviteUserDialog from '../InviteUserDialog';

const hoisted = vi.hoisted(() => {
    return {
        handleClose: vi.fn(),
        handleEmailChange: vi.fn(),
        handleInvite: vi.fn(),
        handleRegeneratePassword: vi.fn(),
        handleRoleChange: vi.fn(),
        mockUseInviteUserDialog: vi.fn(),
    };
});

vi.mock('../hooks/useInviteUserDialog', () => ({
    default: hoisted.mockUseInviteUserDialog,
}));

const defaultMockReturn = {
    authorities: ['ROLE_ADMIN', 'ROLE_USER'],
    handleClose: hoisted.handleClose,
    handleEmailChange: hoisted.handleEmailChange,
    handleInvite: hoisted.handleInvite,
    handleOpen: vi.fn(),
    handleRegeneratePassword: hoisted.handleRegeneratePassword,
    handleRoleChange: hoisted.handleRoleChange,
    inviteDisabled: false,
    inviteEmail: 'test@example.com',
    invitePassword: 'GeneratedPass1',
    inviteRole: 'ROLE_ADMIN',
    open: true,
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseInviteUserDialog.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

const renderInviteUserDialog = () => {
    return render(<InviteUserDialog />);
};

describe('InviteUserDialog', () => {
    it('should render the dialog when open is true', () => {
        renderInviteUserDialog();

        expect(screen.getByText('Invite User')).toBeInTheDocument();
    });

    it('should display the dialog description', () => {
        renderInviteUserDialog();

        expect(
            screen.getByText(
                'Enter the user email. A strong password is pre-generated according to the security rules. This password will be included in the invitation email.'
            )
        ).toBeInTheDocument();
    });

    it('should display Email label', () => {
        renderInviteUserDialog();

        expect(screen.getByText('Email')).toBeInTheDocument();
    });

    it('should display Password label', () => {
        renderInviteUserDialog();

        expect(screen.getByText('Password')).toBeInTheDocument();
    });

    it('should display Role label', () => {
        renderInviteUserDialog();

        expect(screen.getByText('Role')).toBeInTheDocument();
    });

    it('should render Cancel and Invite buttons', () => {
        renderInviteUserDialog();

        expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Invite'})).toBeInTheDocument();
    });

    it('should render Regenerate button', () => {
        renderInviteUserDialog();

        expect(screen.getByRole('button', {name: 'Regenerate'})).toBeInTheDocument();
    });

    it('should call handleInvite when clicking Invite button', async () => {
        renderInviteUserDialog();

        const inviteButton = screen.getByRole('button', {name: 'Invite'});
        await userEvent.click(inviteButton);

        expect(hoisted.handleInvite).toHaveBeenCalledTimes(1);
    });

    it('should call handleRegeneratePassword when clicking Regenerate button', async () => {
        renderInviteUserDialog();

        const regenerateButton = screen.getByRole('button', {name: 'Regenerate'});
        await userEvent.click(regenerateButton);

        expect(hoisted.handleRegeneratePassword).toHaveBeenCalledTimes(1);
    });

    it('should call handleClose when clicking Cancel button', async () => {
        renderInviteUserDialog();

        const cancelButton = screen.getByRole('button', {name: 'Cancel'});
        await userEvent.click(cancelButton);

        expect(hoisted.handleClose).toHaveBeenCalled();
    });

    it('should display password value', () => {
        renderInviteUserDialog();

        const passwordInput = screen.getByDisplayValue('GeneratedPass1');

        expect(passwordInput).toBeInTheDocument();
    });

    it('should display current email value in input', () => {
        renderInviteUserDialog();

        const emailInput = screen.getByDisplayValue('test@example.com');

        expect(emailInput).toBeInTheDocument();
    });
});

describe('InviteUserDialog closed state', () => {
    beforeEach(() => {
        hoisted.mockUseInviteUserDialog.mockReturnValue({
            authorities: ['ROLE_ADMIN', 'ROLE_USER'],
            handleClose: hoisted.handleClose,
            handleEmailChange: hoisted.handleEmailChange,
            handleInvite: hoisted.handleInvite,
            handleOpen: vi.fn(),
            handleRegeneratePassword: hoisted.handleRegeneratePassword,
            handleRoleChange: hoisted.handleRoleChange,
            inviteDisabled: true,
            inviteEmail: '',
            invitePassword: 'GeneratedPass1',
            inviteRole: null,
            open: false,
        });
    });

    it('should not render the dialog content when open is false', () => {
        renderInviteUserDialog();

        expect(screen.queryByText('Invite User')).not.toBeInTheDocument();
    });
});

describe('InviteUserDialog inviteDisabled state', () => {
    beforeEach(() => {
        hoisted.mockUseInviteUserDialog.mockReturnValue({
            authorities: ['ROLE_ADMIN', 'ROLE_USER'],
            handleClose: hoisted.handleClose,
            handleEmailChange: hoisted.handleEmailChange,
            handleInvite: hoisted.handleInvite,
            handleOpen: vi.fn(),
            handleRegeneratePassword: hoisted.handleRegeneratePassword,
            handleRoleChange: hoisted.handleRoleChange,
            inviteDisabled: true,
            inviteEmail: '',
            invitePassword: 'GeneratedPass1',
            inviteRole: 'ROLE_ADMIN',
            open: true,
        });
    });

    it('should disable Invite button when inviteDisabled is true', () => {
        renderInviteUserDialog();

        const inviteButton = screen.getByRole('button', {name: 'Invite'});

        expect(inviteButton).toBeDisabled();
    });
});
