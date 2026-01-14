import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {createRef} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import EditUserDialog, {EditUserDialogRefI} from '../EditUserDialog';

const hoisted = vi.hoisted(() => {
    return {
        handleClose: vi.fn(),
        handleOpen: vi.fn(),
        handleUpdate: vi.fn(),
        mockUseEditUserDialog: vi.fn(),
        setEditRole: vi.fn(),
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
    handleOpen: hoisted.handleOpen,
    handleUpdate: hoisted.handleUpdate,
    open: true,
    setEditRole: hoisted.setEditRole,
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
    const ref = createRef<EditUserDialogRefI>();
    const result = render(<EditUserDialog ref={ref} />);

    return {...result, ref};
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

    it('should expose open method via ref', () => {
        const {ref} = renderEditUserDialog();

        expect(ref.current).not.toBeNull();
        expect(typeof ref.current?.open).toBe('function');
    });

    it('should call handleOpen when open method is called via ref', () => {
        const {ref} = renderEditUserDialog();

        ref.current?.open('testuser');

        expect(hoisted.handleOpen).toHaveBeenCalledWith('testuser');
    });
});

describe('EditUserDialog closed state', () => {
    beforeEach(() => {
        hoisted.mockUseEditUserDialog.mockReturnValue({
            authorities: ['ROLE_ADMIN', 'ROLE_USER'],
            editRole: null,
            editUser: null,
            handleClose: hoisted.handleClose,
            handleOpen: hoisted.handleOpen,
            handleUpdate: hoisted.handleUpdate,
            open: false,
            setEditRole: hoisted.setEditRole,
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
            handleOpen: hoisted.handleOpen,
            handleUpdate: hoisted.handleUpdate,
            open: true,
            setEditRole: hoisted.setEditRole,
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
            handleOpen: hoisted.handleOpen,
            handleUpdate: hoisted.handleUpdate,
            open: true,
            setEditRole: hoisted.setEditRole,
            updateDisabled: false,
        });
    });

    it('should display user login when email is not available', () => {
        renderEditUserDialog();

        expect(screen.getByText('username')).toBeInTheDocument();
    });
});
