import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver, within} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import UsersTable from '../UsersTable';

const mockUsers = [
    {
        activated: true,
        authorities: ['ROLE_ADMIN'],
        email: 'admin@test.com',
        externalId: 'idp-admin-1',
        firstName: 'Admin',
        id: '1',
        lastName: 'User',
        login: 'admin',
    },
    {
        activated: false,
        authorities: ['ROLE_USER'],
        email: 'user@test.com',
        externalId: null,
        firstName: 'Regular',
        id: '2',
        lastName: 'User',
        login: 'user',
    },
];

const hoisted = vi.hoisted(() => {
    return {
        handleOpenDelete: vi.fn(),
        handleOpenEdit: vi.fn(),
        mockUseDeleteUserAlertDialog: vi.fn(),
        mockUseEditUserDialog: vi.fn(),
        mockUseUsersTable: vi.fn(),
    };
});

vi.mock('../hooks/useUsersTable', () => ({
    default: hoisted.mockUseUsersTable,
}));

vi.mock('../hooks/useDeleteUserAlertDialog', () => ({
    default: hoisted.mockUseDeleteUserAlertDialog,
}));

vi.mock('../hooks/useEditUserDialog', () => ({
    default: hoisted.mockUseEditUserDialog,
}));

const defaultMockReturn = {
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
    hoisted.mockUseUsersTable.mockReturnValue({...defaultMockReturn});
    hoisted.mockUseDeleteUserAlertDialog.mockReturnValue({
        handleClose: vi.fn(),
        handleDelete: vi.fn(),
        handleOpen: hoisted.handleOpenDelete,
        open: false,
    });
    hoisted.mockUseEditUserDialog.mockReturnValue({
        authorities: [],
        editRole: null,
        editUser: null,
        handleClose: vi.fn(),
        handleOpen: hoisted.handleOpenEdit,
        handleRoleChange: vi.fn(),
        handleUpdate: vi.fn(),
        open: false,
        updateDisabled: true,
    });
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

const renderUsersTable = (props = {}) => {
    const defaultProps = {
        pageNumber: 0,
    };

    return render(<UsersTable {...defaultProps} {...props} />);
};

describe('UsersTable', () => {
    describe('table headers', () => {
        it('should render Email header', () => {
            renderUsersTable();

            expect(screen.getByText('Email')).toBeInTheDocument();
        });

        it('should render Name header', () => {
            renderUsersTable();

            expect(screen.getByText('Name')).toBeInTheDocument();
        });

        it('should render Role header', () => {
            renderUsersTable();

            expect(screen.getByText('Role')).toBeInTheDocument();
        });

        it('should render Status header', () => {
            renderUsersTable();

            expect(screen.getByText('Status')).toBeInTheDocument();
        });

        it('should render External ID header', () => {
            renderUsersTable();

            expect(screen.getByText('External ID')).toBeInTheDocument();
        });
    });

    describe('user data display', () => {
        it('should render user emails', () => {
            renderUsersTable();

            expect(screen.getByText('admin@test.com')).toBeInTheDocument();
            expect(screen.getByText('user@test.com')).toBeInTheDocument();
        });

        it('should render user full names', () => {
            renderUsersTable();

            expect(screen.getByText('Admin User')).toBeInTheDocument();
            expect(screen.getByText('Regular User')).toBeInTheDocument();
        });

        it('should render user roles', () => {
            renderUsersTable();

            expect(screen.getByText('Admin')).toBeInTheDocument();
            expect(screen.getByText('User')).toBeInTheDocument();
        });

        it('should render Active status for activated users', () => {
            renderUsersTable();

            expect(screen.getByText('Active')).toBeInTheDocument();
        });

        it('should render Pending status for non-activated users', () => {
            renderUsersTable();

            expect(screen.getByText('Pending')).toBeInTheDocument();
        });

        it('should render the external ID when present', () => {
            renderUsersTable();

            const adminRow = screen.getByText('admin@test.com').closest('tr');

            expect(adminRow).toHaveTextContent('idp-admin-1');
        });

        it('should render an em dash when the external ID is absent', () => {
            renderUsersTable();

            const userRow = screen.getByText('user@test.com').closest('tr');
            const userCells = within(userRow as HTMLElement).getAllByRole('cell');

            expect(userCells[2]).toHaveTextContent(/^—$/);
        });

        it('should render an em dash when the external ID is an empty string', () => {
            hoisted.mockUseUsersTable.mockReturnValue({
                ...defaultMockReturn,
                users: [{...mockUsers[0], externalId: ''}],
            });

            renderUsersTable();

            const adminCells = within(screen.getByText('admin@test.com').closest('tr') as HTMLElement).getAllByRole(
                'cell'
            );

            expect(adminCells[2]).toHaveTextContent(/^—$/);
        });

        it('should not render the external ID as an editable field', () => {
            renderUsersTable();

            expect(screen.queryByDisplayValue('idp-admin-1')).not.toBeInTheDocument();
            expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
        });
    });

    describe('user actions', () => {
        it('should call handleOpenEdit when clicking edit button', async () => {
            renderUsersTable();

            const editButtons = screen.getAllByRole('button');
            const editButton = editButtons.find((btn) => btn.querySelector('.lucide-edit'));

            if (editButton) {
                await userEvent.click(editButton);

                await waitFor(() => {
                    expect(hoisted.handleOpenEdit).toHaveBeenCalledWith('admin');
                });
            }
        });

        it('should call handleOpenDelete when clicking delete button', async () => {
            renderUsersTable();

            const deleteButtons = screen.getAllByRole('button');
            const deleteButton = deleteButtons.find((btn) => btn.querySelector('.lucide-trash-2'));

            if (deleteButton) {
                await userEvent.click(deleteButton);

                await waitFor(() => {
                    expect(hoisted.handleOpenDelete).toHaveBeenCalledWith('admin');
                });
            }
        });
    });

    describe('table structure', () => {
        it('should render correct number of rows for users', () => {
            renderUsersTable();

            const rows = screen.getAllByRole('row');
            // 1 header row + 2 user rows
            expect(rows.length).toBe(3);
        });

        it('should render 6 columns per user row', () => {
            renderUsersTable();

            const cells = screen.getAllByRole('cell');

            expect(cells.length).toBe(12);
        });
    });
});

describe('UsersTable loading state', () => {
    beforeEach(() => {
        hoisted.mockUseUsersTable.mockReturnValue({
            error: null,
            isLoading: true,
            pageNumber: 0,
            pageSize: 20,
            totalElements: 0,
            totalPages: 0,
            users: [],
        });
    });

    it('should render skeleton when isLoading is true', () => {
        renderUsersTable();

        const skeletons = document.querySelectorAll('.animate-pulse');

        expect(skeletons.length).toBeGreaterThan(0);
    });

    it('should not render user data when loading', () => {
        renderUsersTable();

        expect(screen.queryByText('admin@test.com')).not.toBeInTheDocument();
    });
});

describe('UsersTable empty state', () => {
    beforeEach(() => {
        hoisted.mockUseUsersTable.mockReturnValue({
            error: null,
            isLoading: false,
            pageNumber: 0,
            pageSize: 20,
            totalElements: 0,
            totalPages: 0,
            users: [],
        });
    });

    it('should render empty message when no users', () => {
        renderUsersTable();

        expect(screen.getByText('No users found.')).toBeInTheDocument();
    });
});

describe('UsersTable error state', () => {
    beforeEach(() => {
        hoisted.mockUseUsersTable.mockReturnValue({
            error: new Error('Failed to fetch users'),
            isLoading: false,
            pageNumber: 0,
            pageSize: 20,
            totalElements: 0,
            totalPages: 0,
            users: [],
        });
    });

    it('should render error message when error exists', () => {
        renderUsersTable();

        expect(screen.getByText('Error: Failed to fetch users')).toBeInTheDocument();
    });

    it('should not render table when error exists', () => {
        renderUsersTable();

        expect(screen.queryByText('Email')).not.toBeInTheDocument();
    });
});
