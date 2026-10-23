import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import InviteUserDialog from '../InviteUserDialog';

const hoisted = vi.hoisted(() => {
    return {
        handleClose: vi.fn(),
        handleEmailChange: vi.fn(),
        handleInvite: vi.fn(),
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
    handleRoleChange: hoisted.handleRoleChange,
    inviteDisabled: false,
    inviteEmail: 'test@example.com',
    inviteRole: 'ROLE_ADMIN',
    open: true,
    roleSelectVisible: true,
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
                'The invitee receives an email with a link on which they set their own password. The link expires after 3 days.'
            )
        ).toBeInTheDocument();
    });

    it('should display Email label', () => {
        renderInviteUserDialog();

        expect(screen.getByText('Email')).toBeInTheDocument();
    });

    it('should not ask for a password', () => {
        renderInviteUserDialog();

        expect(screen.queryByText('Password')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Regenerate'})).not.toBeInTheDocument();
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

    it('should call handleInvite when clicking Invite button', async () => {
        renderInviteUserDialog();

        const inviteButton = screen.getByRole('button', {name: 'Invite'});
        await userEvent.click(inviteButton);

        expect(hoisted.handleInvite).toHaveBeenCalledTimes(1);
    });

    it('should call handleClose when clicking Cancel button', async () => {
        renderInviteUserDialog();

        const cancelButton = screen.getByRole('button', {name: 'Cancel'});
        await userEvent.click(cancelButton);

        expect(hoisted.handleClose).toHaveBeenCalled();
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
            handleRoleChange: hoisted.handleRoleChange,
            inviteDisabled: true,
            inviteEmail: '',
            inviteRole: null,
            open: false,
            roleSelectVisible: true,
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
            handleRoleChange: hoisted.handleRoleChange,
            inviteDisabled: true,
            inviteEmail: '',
            inviteRole: 'ROLE_ADMIN',
            open: true,
            roleSelectVisible: true,
        });
    });

    it('should disable Invite button when inviteDisabled is true', () => {
        renderInviteUserDialog();

        const inviteButton = screen.getByRole('button', {name: 'Invite'});

        expect(inviteButton).toBeDisabled();
    });
});

describe('InviteUserDialog Community Edition', () => {
    beforeEach(() => {
        hoisted.mockUseInviteUserDialog.mockReturnValue({...defaultMockReturn, roleSelectVisible: false});
    });

    it('should not display Role label when the role select is hidden', () => {
        renderInviteUserDialog();

        expect(screen.queryByText('Role')).not.toBeInTheDocument();
    });

    it('should still display Email label when the role select is hidden', () => {
        renderInviteUserDialog();

        expect(screen.getByText('Email')).toBeInTheDocument();
    });
});
