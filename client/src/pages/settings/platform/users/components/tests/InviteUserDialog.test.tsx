import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import InviteUserDialog from '../InviteUserDialog';

const hoisted = vi.hoisted(() => {
    return {
        handleClose: vi.fn(),
        handleEmailChange: vi.fn(),
        handleInvite: vi.fn(),
        handleRoleChange: vi.fn(),
        handleWorkspaceRoleChange: vi.fn(),
        handleWorkspaceToggle: vi.fn(),
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
    handleOpenChange: vi.fn(),
    handleRoleChange: hoisted.handleRoleChange,
    handleWorkspaceRoleChange: hoisted.handleWorkspaceRoleChange,
    handleWorkspaceToggle: hoisted.handleWorkspaceToggle,
    inviteDisabled: false,
    inviteEmail: 'test@example.com',
    inviteRole: 'ROLE_ADMIN',
    inviteWorkspaces: [],
    open: true,
    workspaces: [
        {id: '1', name: 'Engineering'},
        {id: '2', name: 'Marketing'},
    ],
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

    it('should describe the claim link rather than a password', () => {
        renderInviteUserDialog();

        expect(screen.getByText(/set their own password/)).toBeInTheDocument();
    });

    it('should not offer any password field', () => {
        renderInviteUserDialog();

        expect(screen.queryByText('Password')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Regenerate'})).not.toBeInTheDocument();
    });

    it('should display Email label', () => {
        renderInviteUserDialog();

        expect(screen.getByText('Email')).toBeInTheDocument();
    });

    it('should display Role label', () => {
        renderInviteUserDialog();

        expect(screen.getByText('Role')).toBeInTheDocument();
    });

    it('should list the available workspaces', () => {
        renderInviteUserDialog();

        expect(screen.getByText('Workspaces')).toBeInTheDocument();
        expect(screen.getByText('Engineering')).toBeInTheDocument();
        expect(screen.getByText('Marketing')).toBeInTheDocument();
    });

    it('should call handleWorkspaceToggle when selecting a workspace', async () => {
        renderInviteUserDialog();

        await userEvent.click(screen.getAllByRole('checkbox')[0]);

        expect(hoisted.handleWorkspaceToggle).toHaveBeenCalledWith('1');
    });

    it('should render Cancel and Invite buttons', () => {
        renderInviteUserDialog();

        expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Invite'})).toBeInTheDocument();
    });

    it('should call handleInvite when clicking Invite button', async () => {
        renderInviteUserDialog();

        await userEvent.click(screen.getByRole('button', {name: 'Invite'}));

        expect(hoisted.handleInvite).toHaveBeenCalledTimes(1);
    });

    it('should call handleClose when clicking Cancel button', async () => {
        renderInviteUserDialog();

        await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(hoisted.handleClose).toHaveBeenCalledTimes(1);
    });

    it('should display current email value in input', () => {
        renderInviteUserDialog();

        expect(screen.getByDisplayValue('test@example.com')).toBeInTheDocument();
    });

    it('should enable Invite with no workspaces selected', () => {
        renderInviteUserDialog();

        // An invite carrying no workspaces is valid: it provisions an account belonging to none, which is how a
        // second tenant admin is created.
        expect(screen.getByRole('button', {name: 'Invite'})).toBeEnabled();
    });
});

describe('InviteUserDialog closed state', () => {
    beforeEach(() => {
        hoisted.mockUseInviteUserDialog.mockReturnValue({
            ...defaultMockReturn,
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
            ...defaultMockReturn,
            inviteDisabled: true,
        });
    });

    it('should disable the Invite button', () => {
        renderInviteUserDialog();

        expect(screen.getByRole('button', {name: 'Invite'})).toBeDisabled();
    });
});
