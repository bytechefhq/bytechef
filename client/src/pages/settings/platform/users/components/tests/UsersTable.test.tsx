import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {createRef} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import UsersTable, {UsersTableRefI} from '../UsersTable';

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

const hoisted = vi.hoisted(() => {
    return {
        mockUseUsersTable: vi.fn(),
    };
});

vi.mock('../hooks/useUsersTable', () => ({
    default: hoisted.mockUseUsersTable,
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
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

const renderUsersTable = (props = {}) => {
    const ref = createRef<UsersTableRefI>();
    const defaultProps = {
        onOpenDelete: vi.fn(),
        onOpenEdit: vi.fn(),
        pageNumber: 0,
    };
    const result = render(<UsersTable {...defaultProps} {...props} ref={ref} />);

    return {...result, ref};
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

            expect(screen.getByText('ROLE_ADMIN')).toBeInTheDocument();
            expect(screen.getByText('ROLE_USER')).toBeInTheDocument();
        });

        it('should render Active status for activated users', () => {
            renderUsersTable();

            expect(screen.getByText('Active')).toBeInTheDocument();
        });

        it('should render Pending status for non-activated users', () => {
            renderUsersTable();

            expect(screen.getByText('Pending')).toBeInTheDocument();
        });
    });

    describe('user actions', () => {
        it('should call onOpenEdit when clicking edit button', async () => {
            const onOpenEdit = vi.fn();

            renderUsersTable({onOpenEdit});

            const editButtons = screen.getAllByRole('button');
            const editButton = editButtons.find((btn) => btn.querySelector('.lucide-edit'));

            if (editButton) {
                await userEvent.click(editButton);

                await waitFor(() => {
                    expect(onOpenEdit).toHaveBeenCalledWith('admin');
                });
            }
        });

        it('should call onOpenDelete when clicking delete button', async () => {
            const onOpenDelete = vi.fn();

            renderUsersTable({onOpenDelete});

            const deleteButtons = screen.getAllByRole('button');
            const deleteButton = deleteButtons.find((btn) => btn.querySelector('.lucide-trash-2'));

            if (deleteButton) {
                await userEvent.click(deleteButton);

                await waitFor(() => {
                    expect(onOpenDelete).toHaveBeenCalledWith('admin');
                });
            }
        });
    });

    describe('ref', () => {
        it('should expose isLoading via ref', () => {
            const {ref} = renderUsersTable();

            expect(ref.current).not.toBeNull();
            expect(ref.current?.isLoading).toBe(false);
        });
    });

    describe('table structure', () => {
        it('should render correct number of rows for users', () => {
            renderUsersTable();

            const rows = screen.getAllByRole('row');
            // 1 header row + 2 user rows
            expect(rows.length).toBe(3);
        });

        it('should render 5 columns per user row', () => {
            renderUsersTable();

            // First user has 5 cells
            const cells = screen.getAllByRole('cell');

            expect(cells.length).toBe(10); // 5 cells per user * 2 users
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

    it('should expose isLoading as true via ref', () => {
        const {ref} = renderUsersTable();

        expect(ref.current?.isLoading).toBe(true);
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
