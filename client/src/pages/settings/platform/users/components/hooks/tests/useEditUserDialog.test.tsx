import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useEditUserDialog from '../useEditUserDialog';

const hoisted = vi.hoisted(() => {
    return {
        storeState: {
            clearLoginToEdit: vi.fn(),
            editRole: null as string | null,
            loginToEdit: null as string | null,
            setEditRole: vi.fn(),
            setLoginToEdit: vi.fn(),
        },
        updateUserMutate: vi.fn(),
    };
});

vi.mock('@/pages/settings/platform/users/stores/useEditUserDialogStore', () => ({
    useEditUserDialogStore: vi.fn(() => {
        return {
            clearLoginToEdit: () => {
                hoisted.storeState.editRole = null;
                hoisted.storeState.loginToEdit = null;
                hoisted.storeState.clearLoginToEdit();
            },
            editRole: hoisted.storeState.editRole,
            loginToEdit: hoisted.storeState.loginToEdit,
            setEditRole: (role: string) => {
                hoisted.storeState.editRole = role;
                hoisted.storeState.setEditRole(role);
            },
            setLoginToEdit: (login: string) => {
                hoisted.storeState.loginToEdit = login;
                hoisted.storeState.setLoginToEdit(login);
            },
        };
    }),
}));

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
        hoisted.storeState.editRole = null;
        hoisted.storeState.loginToEdit = null;
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
        it('opens dialog', () => {
            const {rerender, result} = renderHook(() => useEditUserDialog());

            act(() => {
                result.current.handleOpen('admin');
            });

            rerender();

            expect(result.current.open).toBe(true);
            expect(result.current.editUser?.login).toBe('admin');
        });
    });

    describe('close dialog', () => {
        it('closes dialog', () => {
            hoisted.storeState.loginToEdit = 'admin';
            const {rerender, result} = renderHook(() => useEditUserDialog());

            act(() => {
                result.current.handleClose();
            });

            rerender();

            expect(result.current.open).toBe(false);
        });
    });

    describe('update role', () => {
        it('updates edit role', () => {
            hoisted.storeState.loginToEdit = 'admin';
            const {rerender, result} = renderHook(() => useEditUserDialog());

            act(() => {
                result.current.handleRoleChange('ROLE_USER');
            });

            rerender();

            expect(result.current.editRole).toBe('ROLE_USER');
        });
    });

    describe('handle update', () => {
        it('calls update mutation with correct parameters', () => {
            hoisted.storeState.loginToEdit = 'admin';
            hoisted.storeState.editRole = 'ROLE_USER';
            const {result} = renderHook(() => useEditUserDialog());

            act(() => {
                result.current.handleUpdate();
            });

            expect(hoisted.updateUserMutate).toHaveBeenCalledWith({
                login: 'admin',
                role: 'ROLE_USER',
            });
        });

        it('does not call update mutation when dialog is not open', () => {
            const {result} = renderHook(() => useEditUserDialog());

            act(() => {
                result.current.handleUpdate();
            });

            expect(hoisted.updateUserMutate).not.toHaveBeenCalled();
        });

        it('closes dialog after successful update', () => {
            hoisted.storeState.loginToEdit = 'admin';
            hoisted.storeState.editRole = 'ROLE_ADMIN';
            const {rerender, result} = renderHook(() => useEditUserDialog());

            act(() => {
                result.current.handleUpdate();
            });

            rerender();

            expect(result.current.open).toBe(false);
        });
    });
});
