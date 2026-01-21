import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useDeleteUserAlertDialog from '../useDeleteUserAlertDialog';

const hoisted = vi.hoisted(() => {
    return {
        deleteUserMutate: vi.fn(),
        storeState: {
            handleClose: vi.fn(),
            handleOpen: vi.fn(),
            loginToDelete: null as string | null,
        },
    };
});

vi.mock('@/pages/settings/platform/users/stores/useDeleteUserDialogStore', () => ({
    useDeleteUserDialogStore: vi.fn(() => {
        return {
            handleClose: () => {
                hoisted.storeState.loginToDelete = null;
                hoisted.storeState.handleClose();
            },
            handleOpen: (login: string | null) => {
                hoisted.storeState.loginToDelete = login;
                hoisted.storeState.handleOpen(login);
            },
            loginToDelete: hoisted.storeState.loginToDelete,
        };
    }),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useDeleteUserMutation: vi.fn((options: {onSuccess: () => void}) => ({
        mutate: (vars: unknown) => {
            hoisted.deleteUserMutate(vars);
            options.onSuccess();
        },
    })),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: vi.fn(() => ({
        invalidateQueries: vi.fn(),
    })),
}));

describe('useDeleteUserAlertDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        hoisted.storeState.loginToDelete = null;
    });

    describe('initial state', () => {
        it('returns initial state as closed', () => {
            const {result} = renderHook(() => useDeleteUserAlertDialog());

            expect(result.current.open).toBe(false);
        });
    });

    describe('open dialog', () => {
        it('opens dialog with login', () => {
            const {rerender, result} = renderHook(() => useDeleteUserAlertDialog());

            act(() => {
                result.current.handleOpen('admin');
            });

            rerender();

            expect(result.current.open).toBe(true);
        });

        it('opens dialog with null login', () => {
            const {rerender, result} = renderHook(() => useDeleteUserAlertDialog());

            act(() => {
                result.current.handleOpen(null);
            });

            rerender();

            expect(result.current.open).toBe(false);
        });
    });

    describe('close dialog', () => {
        it('closes dialog', () => {
            hoisted.storeState.loginToDelete = 'admin';
            const {rerender, result} = renderHook(() => useDeleteUserAlertDialog());

            act(() => {
                result.current.handleClose();
            });

            rerender();

            expect(result.current.open).toBe(false);
        });
    });

    describe('handle delete', () => {
        it('calls delete mutation with correct login', () => {
            hoisted.storeState.loginToDelete = 'admin';
            const {result} = renderHook(() => useDeleteUserAlertDialog());

            act(() => {
                result.current.handleDelete();
            });

            expect(hoisted.deleteUserMutate).toHaveBeenCalledWith({login: 'admin'});
        });

        it('does not call delete mutation when loginToDelete is null', () => {
            const {result} = renderHook(() => useDeleteUserAlertDialog());

            act(() => {
                result.current.handleDelete();
            });

            expect(hoisted.deleteUserMutate).not.toHaveBeenCalled();
        });

        it('closes dialog after successful delete', () => {
            hoisted.storeState.loginToDelete = 'admin';
            const {rerender, result} = renderHook(() => useDeleteUserAlertDialog());

            act(() => {
                result.current.handleDelete();
            });

            rerender();

            expect(result.current.open).toBe(false);
        });
    });
});
