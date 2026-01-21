import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import EditUserDialog from '../EditUserDialog';

const hoisted = vi.hoisted(() => {
    return {
        handleClose: vi.fn(),
        handleRoleChange: vi.fn(),
        handleUpdate: vi.fn(),
        mockUseEditUserDialog: vi.fn(),
    };
});

vi.mock('../hooks/useEditUserDialog', () => ({
    default: hoisted.mockUseEditUserDialog,
}));

const defaultMockReturn = {
    authorities: ['ROLE_ADMIN', 'ROLE_USER'],
    editRole: 'ROLE_ADMIN',
    editUser: {email: 'admin@test.com', login: 'admin'},
    handleClose: hoisted.handleClose,
    handleOpen: vi.fn(),
    handleRoleChange: hoisted.handleRoleChange,
    handleUpdate: hoisted.handleUpdate,
    open: true,
    updateDisabled: false,
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseEditUserDialog.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

const renderEditUserDialog = () => {
    return render(<EditUserDialog />);
};

describe('EditUserDialog', () => {
    it('should render the dialog when open is true', () => {
        renderEditUserDialog();

        expect(screen.getByText('Edit User')).toBeInTheDocument();
    });

    it('should display the dialog description', () => {
        renderEditUserDialog();

        expect(screen.getByText('Change the user role.')).toBeInTheDocument();
    });

    it('should display User label', () => {
        renderEditUserDialog();

        expect(screen.getByText('User')).toBeInTheDocument();
    });

    it('should display user email', () => {
        renderEditUserDialog();

        expect(screen.getByText('admin@test.com')).toBeInTheDocument();
    });

    it('should display Role label', () => {
        renderEditUserDialog();

        expect(screen.getByText('Role')).toBeInTheDocument();
    });

    it('should render Cancel and Save buttons', () => {
        renderEditUserDialog();

        expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Save'})).toBeInTheDocument();
    });

    it('should call handleUpdate when clicking Save button', async () => {
        renderEditUserDialog();

        const saveButton = screen.getByRole('button', {name: 'Save'});
        await userEvent.click(saveButton);

        expect(hoisted.handleUpdate).toHaveBeenCalledTimes(1);
    });

    it('should call handleClose when clicking Cancel button', async () => {
        renderEditUserDialog();

        const cancelButton = screen.getByRole('button', {name: 'Cancel'});
        await userEvent.click(cancelButton);

        expect(hoisted.handleClose).toHaveBeenCalled();
    });
});

describe('EditUserDialog closed state', () => {
    beforeEach(() => {
        hoisted.mockUseEditUserDialog.mockReturnValue({
            authorities: ['ROLE_ADMIN', 'ROLE_USER'],
            editRole: null,
            editUser: null,
            handleClose: hoisted.handleClose,
            handleOpen: vi.fn(),
            handleRoleChange: hoisted.handleRoleChange,
            handleUpdate: hoisted.handleUpdate,
            open: false,
            updateDisabled: true,
        });
    });

    it('should not render the dialog content when open is false', () => {
        renderEditUserDialog();

        expect(screen.queryByText('Edit User')).not.toBeInTheDocument();
    });
});

describe('EditUserDialog updateDisabled state', () => {
    beforeEach(() => {
        hoisted.mockUseEditUserDialog.mockReturnValue({
            authorities: ['ROLE_ADMIN', 'ROLE_USER'],
            editRole: 'ROLE_ADMIN',
            editUser: {email: 'admin@test.com', login: 'admin'},
            handleClose: hoisted.handleClose,
            handleOpen: vi.fn(),
            handleRoleChange: hoisted.handleRoleChange,
            handleUpdate: hoisted.handleUpdate,
            open: true,
            updateDisabled: true,
        });
    });

    it('should disable Save button when updateDisabled is true', () => {
        renderEditUserDialog();

        const saveButton = screen.getByRole('button', {name: 'Save'});

        expect(saveButton).toBeDisabled();
    });
});

describe('EditUserDialog with null email', () => {
    beforeEach(() => {
        hoisted.mockUseEditUserDialog.mockReturnValue({
            authorities: ['ROLE_ADMIN', 'ROLE_USER'],
            editRole: 'ROLE_ADMIN',
            editUser: {email: null, login: 'username'},
            handleClose: hoisted.handleClose,
            handleOpen: vi.fn(),
            handleRoleChange: hoisted.handleRoleChange,
            handleUpdate: hoisted.handleUpdate,
            open: true,
            updateDisabled: false,
        });
    });

    it('should display user login when email is not available', () => {
        renderEditUserDialog();

        expect(screen.getByText('username')).toBeInTheDocument();
    });
});
