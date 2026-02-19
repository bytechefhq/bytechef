import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useEditUserDialog from '../useEditUserDialog';

const hoisted = vi.hoisted(() => {
    return {
        updateUserMutate: vi.fn(),
    };
});

vi.mock('@/shared/middleware/graphql', () => ({
    useAuthoritiesQuery: vi.fn(() => ({
        data: {authorities: ['ROLE_ADMIN', 'ROLE_USER']},
    })),
    useUpdateUserMutation: vi.fn((options: {onSuccess: () => void}) => ({
        mutate: (vars: unknown) => {
            hoisted.updateUserMutate(vars);
            options.onSuccess();
        },
    })),
    useUsersQuery: vi.fn(() => ({
        data: {
            users: {
                content: [
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
                number: 0,
                size: 20,
                totalElements: 2,
                totalPages: 1,
            },
        },
    })),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: vi.fn(() => ({
        invalidateQueries: vi.fn(),
    })),
}));

describe('useEditUserDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('initial state', () => {
        it('returns authorities from query', () => {
            const {result} = renderHook(() => useEditUserDialog());

            expect(result.current.authorities).toEqual(['ROLE_ADMIN', 'ROLE_USER']);
        });

        it('returns initial edit state as closed', () => {
            const {result} = renderHook(() => useEditUserDialog());

            expect(result.current.open).toBe(false);
            expect(result.current.editRole).toBeNull();
            expect(result.current.editUser).toBeNull();
        });

        it('returns updateDisabled as true when not open', () => {
            const {result} = renderHook(() => useEditUserDialog());

            expect(result.current.updateDisabled).toBe(true);
        });
    });

    describe('open dialog', () => {
        it('opens dialog and sets user and role', () => {
            const {result} = renderHook(() => useEditUserDialog());

            act(() => {
                result.current.handleEditUserDialogOpen('admin');
            });

            expect(result.current.open).toBe(true);
            expect(result.current.editRole).toBe('ROLE_ADMIN');
            expect(result.current.editUser?.login).toBe('admin');
        });

        it('sets correct role for different user', () => {
            const {result} = renderHook(() => useEditUserDialog());

            act(() => {
                result.current.handleEditUserDialogOpen('user');
            });

            expect(result.current.open).toBe(true);
            expect(result.current.editRole).toBe('ROLE_USER');
            expect(result.current.editUser?.login).toBe('user');
        });

        it('falls back to first authority when user has no role', () => {
            const {result} = renderHook(() => useEditUserDialog());

            act(() => {
                result.current.handleEditUserDialogOpen('unknown');
            });

            expect(result.current.open).toBe(true);
            expect(result.current.editRole).toBe('ROLE_ADMIN');
            expect(result.current.editUser).toBeNull();
        });

        it('sets updateDisabled to false when dialog is open with role', () => {
            const {result} = renderHook(() => useEditUserDialog());

            act(() => {
                result.current.handleEditUserDialogOpen('admin');
            });

            expect(result.current.updateDisabled).toBe(false);
        });
    });

    describe('close dialog', () => {
        it('closes dialog', () => {
            const {result} = renderHook(() => useEditUserDialog());

            act(() => {
                result.current.handleEditUserDialogOpen('admin');
            });

            act(() => {
                result.current.handleEditUserDialogClose();
            });

            expect(result.current.open).toBe(false);
        });
    });

    describe('update role', () => {
        it('updates edit role', () => {
            const {result} = renderHook(() => useEditUserDialog());

            act(() => {
                result.current.handleEditUserDialogOpen('admin');
            });

            act(() => {
                result.current.setEditRole('ROLE_USER');
            });

            expect(result.current.editRole).toBe('ROLE_USER');
        });

        it('sets updateDisabled to true when role is null', () => {
            const {result} = renderHook(() => useEditUserDialog());

            act(() => {
                result.current.handleEditUserDialogOpen('admin');
            });

            act(() => {
                result.current.setEditRole(null);
            });

            expect(result.current.updateDisabled).toBe(true);
        });
    });

    describe('handle update', () => {
        it('calls update mutation with correct parameters', () => {
            const {result} = renderHook(() => useEditUserDialog());

            act(() => {
                result.current.handleEditUserDialogOpen('admin');
                result.current.setEditRole('ROLE_USER');
            });

            act(() => {
                result.current.handleEditUserDialogUpdate();
            });

            expect(hoisted.updateUserMutate).toHaveBeenCalledWith({
                login: 'admin',
                role: 'ROLE_USER',
            });
        });

        it('does not call update mutation when dialog is not open', () => {
            const {result} = renderHook(() => useEditUserDialog());

            act(() => {
                result.current.handleEditUserDialogUpdate();
            });

            expect(hoisted.updateUserMutate).not.toHaveBeenCalled();
        });

        it('does not call update mutation when editRole is null', () => {
            const {result} = renderHook(() => useEditUserDialog());

            act(() => {
                result.current.handleEditUserDialogOpen('admin');
                result.current.setEditRole(null);
            });

            act(() => {
                result.current.handleEditUserDialogUpdate();
            });

            expect(hoisted.updateUserMutate).not.toHaveBeenCalled();
        });

        it('closes dialog after successful update', () => {
            const {result} = renderHook(() => useEditUserDialog());

            act(() => {
                result.current.handleEditUserDialogOpen('admin');
            });

            act(() => {
                result.current.handleEditUserDialogUpdate();
            });

            expect(result.current.open).toBe(false);
        });
    });
});
