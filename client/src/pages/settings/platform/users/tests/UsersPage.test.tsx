import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import UsersPage from '../UsersPage';

const hoisted = vi.hoisted(() => {
    return {
        handleDeleteDialogOpen: vi.fn(),
        handleEditDialogOpen: vi.fn(),
        handleInviteDialogOpen: vi.fn(),
        mockUseDeleteUserAlertDialog: vi.fn(),
        mockUseEditUserDialog: vi.fn(),
        mockUseInviteUserDialog: vi.fn(),
        mockUseUsersTable: vi.fn(),
    };
});

vi.mock('@/pages/settings/platform/users/components/hooks/useUsersTable', () => ({
    default: hoisted.mockUseUsersTable,
}));

vi.mock('@/pages/settings/platform/users/components/hooks/useDeleteUserAlertDialog', () => ({
    default: hoisted.mockUseDeleteUserAlertDialog,
}));

vi.mock('@/pages/settings/platform/users/components/hooks/useEditUserDialog', () => ({
    default: hoisted.mockUseEditUserDialog,
}));

vi.mock('@/pages/settings/platform/users/components/hooks/useInviteUserDialog', () => ({
    default: hoisted.mockUseInviteUserDialog,
}));

const mockUsers = [
    {
        activated: true,
        authorities: ['ROLE_ADMIN'],
        email: 'admin@test.com',
        firstName: 'Admin',
        id: '1',
        lastName: 'User',
        login: 'admin',
    },
    {
        activated: false,
        authorities: ['ROLE_USER'],
        email: 'user@test.com',
        firstName: 'Regular',
        id: '2',
        lastName: 'User',
        login: 'user',
    },
];

const defaultUsersTableMockReturn = {
    error: null,
    isLoading: false,
    pageNumber: 0,
    pageSize: 20,
    totalElements: mockUsers.length,
    totalPages: 1,
    users: mockUsers,
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseUsersTable.mockReturnValue({...defaultUsersTableMockReturn});
    hoisted.mockUseDeleteUserAlertDialog.mockReturnValue({
        handleClose: vi.fn(),
        handleDelete: vi.fn(),
        handleOpen: hoisted.handleDeleteDialogOpen,
        open: false,
    });
    hoisted.mockUseEditUserDialog.mockReturnValue({
        authorities: [],
        editRole: null,
        editUser: null,
        handleClose: vi.fn(),
        handleOpen: hoisted.handleEditDialogOpen,
        handleRoleChange: vi.fn(),
        handleUpdate: vi.fn(),
        open: false,
        updateDisabled: true,
    });
    hoisted.mockUseInviteUserDialog.mockReturnValue({
        authorities: [],
        handleClose: vi.fn(),
        handleEmailChange: vi.fn(),
        handleInvite: vi.fn(),
        handleOpen: hoisted.handleInviteDialogOpen,
        handleRegeneratePassword: vi.fn(),
        handleRoleChange: vi.fn(),
        inviteDisabled: true,
        inviteEmail: '',
        invitePassword: '',
        inviteRole: null,
        open: false,
    });
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('UsersPage', () => {
    describe('loading state', () => {
        it('should render skeleton rows when isLoading is true', () => {
            hoisted.mockUseUsersTable.mockReturnValue({...defaultUsersTableMockReturn, isLoading: true, users: []});

            render(<UsersPage />);

            const skeletons = document.querySelectorAll('.animate-pulse');

            expect(skeletons.length).toBeGreaterThan(0);
        });

        it('should not render user data when loading', () => {
            hoisted.mockUseUsersTable.mockReturnValue({...defaultUsersTableMockReturn, isLoading: true, users: []});

            render(<UsersPage />);

            expect(screen.queryByText('admin@test.com')).not.toBeInTheDocument();
        });
    });

    describe('error state', () => {
        it('should render error message when error exists', () => {
            hoisted.mockUseUsersTable.mockReturnValue({
                ...defaultUsersTableMockReturn,
                error: new Error('Failed to fetch users'),
            });

            render(<UsersPage />);

            expect(screen.getByText('Error: Failed to fetch users')).toBeInTheDocument();
        });
    });

    describe('users table', () => {
        it('should render page title', () => {
            render(<UsersPage />);

            expect(screen.getByText('Users')).toBeInTheDocument();
        });

        it('should render page description', () => {
            render(<UsersPage />);

            expect(screen.getByText('Manage organization users: invite or delete users.')).toBeInTheDocument();
        });

        it('should render Invite User button', () => {
            render(<UsersPage />);

            expect(screen.getByRole('button', {name: 'Invite User'})).toBeInTheDocument();
        });

        it('should render table headers', () => {
            render(<UsersPage />);

            expect(screen.getByText('Email')).toBeInTheDocument();
            expect(screen.getByText('Name')).toBeInTheDocument();
            expect(screen.getByText('Role')).toBeInTheDocument();
            expect(screen.getByText('Status')).toBeInTheDocument();
        });

        it('should render user emails', () => {
            render(<UsersPage />);

            expect(screen.getByText('admin@test.com')).toBeInTheDocument();
            expect(screen.getByText('user@test.com')).toBeInTheDocument();
        });

        it('should render user names', () => {
            render(<UsersPage />);

            expect(screen.getByText('Admin User')).toBeInTheDocument();
            expect(screen.getByText('Regular User')).toBeInTheDocument();
        });

        it('should render user roles', () => {
            render(<UsersPage />);

            expect(screen.getByText('ROLE_ADMIN')).toBeInTheDocument();
            expect(screen.getByText('ROLE_USER')).toBeInTheDocument();
        });

        it('should render user status', () => {
            render(<UsersPage />);

            expect(screen.getByText('Active')).toBeInTheDocument();
            expect(screen.getByText('Pending')).toBeInTheDocument();
        });

        it('should render empty state when no users', () => {
            hoisted.mockUseUsersTable.mockReturnValue({...defaultUsersTableMockReturn, users: []});

            render(<UsersPage />);

            expect(screen.getByText('No users found.')).toBeInTheDocument();
        });
    });

    describe('invite dialog', () => {
        it('should call handleOpen from useInviteUserDialog when clicking Invite User button', async () => {
            render(<UsersPage />);

            const inviteButton = screen.getByRole('button', {name: 'Invite User'});
            await userEvent.click(inviteButton);

            expect(hoisted.handleInviteDialogOpen).toHaveBeenCalled();
        });
    });

    describe('delete dialog', () => {
        it('should call handleOpen from useDeleteUserAlertDialog when clicking delete button', async () => {
            render(<UsersPage />);

            const deleteButtons = screen.getAllByRole('button', {name: ''});
            const deleteButton = deleteButtons.find((button) => button.querySelector('.lucide-trash-2'));

            if (deleteButton) {
                await userEvent.click(deleteButton);

                await waitFor(() => {
                    expect(hoisted.handleDeleteDialogOpen).toHaveBeenCalledWith('admin');
                });
            }
        });
    });

    describe('edit dialog', () => {
        it('should call handleOpen from useEditUserDialog when clicking edit button', async () => {
            render(<UsersPage />);

            const editButtons = screen.getAllByRole('button', {name: ''});
            const editButton = editButtons.find((button) => button.querySelector('.lucide-edit'));

            if (editButton) {
                await userEvent.click(editButton);

                await waitFor(() => {
                    expect(hoisted.handleEditDialogOpen).toHaveBeenCalledWith('admin');
                });
            }
        });
    });
});
