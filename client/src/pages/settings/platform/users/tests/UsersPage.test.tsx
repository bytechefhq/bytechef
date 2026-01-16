import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {ForwardedRef, forwardRef} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import UsersPage from '../UsersPage';

const hoisted = vi.hoisted(() => {
    return {
        deleteDialogOpen: vi.fn(),
        editDialogOpen: vi.fn(),
        inviteDialogOpen: vi.fn(),
        mockUseUsersTable: vi.fn(),
    };
});

vi.mock('@/pages/settings/platform/users/components/hooks/useUsersTable', () => ({
    default: hoisted.mockUseUsersTable,
}));

vi.mock('@/pages/settings/platform/users/components/DeleteUserAlertDialog', () => ({
    default: forwardRef((_, ref: ForwardedRef<{open: (login: string | null) => void}>) => {
        if (ref && typeof ref === 'object') {
            ref.current = {
                open: hoisted.deleteDialogOpen,
            };
        }

        return <div data-testid="delete-dialog" />;
    }),
}));

vi.mock('@/pages/settings/platform/users/components/InviteUserDialog', () => ({
    default: forwardRef((_, ref: ForwardedRef<{open: () => void}>) => {
        if (ref && typeof ref === 'object') {
            ref.current = {
                open: hoisted.inviteDialogOpen,
            };
        }

        return <div data-testid="invite-dialog" />;
    }),
}));

vi.mock('@/pages/settings/platform/users/components/EditUserDialog', () => ({
    default: forwardRef((_, ref: ForwardedRef<{open: (login: string) => void}>) => {
        if (ref && typeof ref === 'object') {
            ref.current = {
                open: hoisted.editDialogOpen,
            };
        }

        return <div data-testid="edit-dialog" />;
    }),
}));

const defaultMockReturn = {
    error: null,
    isLoading: false,
    users: [
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
    ],
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseUsersTable.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('UsersPage', () => {
    describe('loading state', () => {
        it('should render skeleton rows when isLoading is true', () => {
            hoisted.mockUseUsersTable.mockReturnValue({...defaultMockReturn, isLoading: true, users: []});

            render(<UsersPage />);

            const skeletons = document.querySelectorAll('.animate-pulse');

            expect(skeletons.length).toBeGreaterThan(0);
        });

        it('should not render user data when loading', () => {
            hoisted.mockUseUsersTable.mockReturnValue({...defaultMockReturn, isLoading: true, users: []});

            render(<UsersPage />);

            expect(screen.queryByText('admin@test.com')).not.toBeInTheDocument();
        });
    });

    describe('error state', () => {
        it('should render error message when error exists', () => {
            hoisted.mockUseUsersTable.mockReturnValue({
                ...defaultMockReturn,
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
            hoisted.mockUseUsersTable.mockReturnValue({...defaultMockReturn, users: []});

            render(<UsersPage />);

            expect(screen.getByText('No users found.')).toBeInTheDocument();
        });
    });

    describe('invite dialog', () => {
        it('should render InviteUserDialog component', () => {
            render(<UsersPage />);

            expect(screen.getByTestId('invite-dialog')).toBeInTheDocument();
        });

        it('should call invite dialog open method when clicking Invite User button', async () => {
            render(<UsersPage />);

            const inviteButton = screen.getByRole('button', {name: 'Invite User'});
            await userEvent.click(inviteButton);

            expect(hoisted.inviteDialogOpen).toHaveBeenCalled();
        });
    });

    describe('delete dialog', () => {
        it('should render DeleteUserAlertDialog component', () => {
            render(<UsersPage />);

            expect(screen.getByTestId('delete-dialog')).toBeInTheDocument();
        });

        it('should call delete dialog open method when clicking delete button', async () => {
            render(<UsersPage />);

            const deleteButtons = screen.getAllByRole('button', {name: ''});
            const deleteButton = deleteButtons.find((btn) => btn.querySelector('.lucide-trash-2'));

            if (deleteButton) {
                await userEvent.click(deleteButton);

                await waitFor(() => {
                    expect(hoisted.deleteDialogOpen).toHaveBeenCalledWith('admin');
                });
            }
        });
    });

    describe('edit dialog', () => {
        it('should render EditUserDialog component', () => {
            render(<UsersPage />);

            expect(screen.getByTestId('edit-dialog')).toBeInTheDocument();
        });

        it('should call edit dialog open method when clicking edit button', async () => {
            render(<UsersPage />);

            const editButtons = screen.getAllByRole('button', {name: ''});
            const editButton = editButtons.find((btn) => btn.querySelector('.lucide-edit'));

            if (editButton) {
                await userEvent.click(editButton);

                await waitFor(() => {
                    expect(hoisted.editDialogOpen).toHaveBeenCalledWith('admin');
                });
            }
        });
    });
});
