import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {createRef} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import InviteUserDialog, {InviteUserDialogRefI} from '../InviteUserDialog';

const hoisted = vi.hoisted(() => {
    return {
        handleClose: vi.fn(),
        handleInvite: vi.fn(),
        handleOpen: vi.fn(),
        handleRegeneratePassword: vi.fn(),
        mockUseInviteUserDialog: vi.fn(),
        setInviteEmail: vi.fn(),
        setInviteRole: vi.fn(),
    };
});

vi.mock('../hooks/useInviteUserDialog', () => ({
    default: hoisted.mockUseInviteUserDialog,
}));

const defaultMockReturn = {
    authorities: ['ROLE_ADMIN', 'ROLE_USER'],
    handleClose: hoisted.handleClose,
    handleInvite: hoisted.handleInvite,
    handleOpen: hoisted.handleOpen,
    handleRegeneratePassword: hoisted.handleRegeneratePassword,
    inviteDisabled: false,
    inviteEmail: 'test@example.com',
    invitePassword: 'GeneratedPass1',
    inviteRole: 'ROLE_ADMIN',
    open: true,
    setInviteEmail: hoisted.setInviteEmail,
    setInviteRole: hoisted.setInviteRole,
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
    const ref = createRef<InviteUserDialogRefI>();
    const result = render(<InviteUserDialog ref={ref} />);

    return {...result, ref};
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

    it('should expose open method via ref', () => {
        const {ref} = renderInviteUserDialog();

        expect(ref.current).not.toBeNull();
        expect(typeof ref.current?.open).toBe('function');
    });

    it('should call handleOpen when open method is called via ref', () => {
        const {ref} = renderInviteUserDialog();

        ref.current?.open();

        expect(hoisted.handleOpen).toHaveBeenCalled();
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
            handleInvite: hoisted.handleInvite,
            handleOpen: hoisted.handleOpen,
            handleRegeneratePassword: hoisted.handleRegeneratePassword,
            inviteDisabled: true,
            inviteEmail: '',
            invitePassword: 'GeneratedPass1',
            inviteRole: null,
            open: false,
            setInviteEmail: hoisted.setInviteEmail,
            setInviteRole: hoisted.setInviteRole,
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
            handleInvite: hoisted.handleInvite,
            handleOpen: hoisted.handleOpen,
            handleRegeneratePassword: hoisted.handleRegeneratePassword,
            inviteDisabled: true,
            inviteEmail: '',
            invitePassword: 'GeneratedPass1',
            inviteRole: 'ROLE_ADMIN',
            open: true,
            setInviteEmail: hoisted.setInviteEmail,
            setInviteRole: hoisted.setInviteRole,
        });
    });

    it('should disable Invite button when inviteDisabled is true', () => {
        renderInviteUserDialog();

        const inviteButton = screen.getByRole('button', {name: 'Invite'});

        expect(inviteButton).toBeDisabled();
    });
});
