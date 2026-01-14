import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useDeleteUserAlertDialog from '../useDeleteUserAlertDialog';

const hoisted = vi.hoisted(() => {
    return {
        deleteUserMutate: vi.fn(),
    };
});

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
    });

    describe('initial state', () => {
        it('returns initial state as closed', () => {
            const {result} = renderHook(() => useDeleteUserAlertDialog());

            expect(result.current.open).toBe(false);
            expect(result.current.deleteLogin).toBeNull();
        });
    });

    describe('open dialog', () => {
        it('opens dialog with login', () => {
            const {result} = renderHook(() => useDeleteUserAlertDialog());

            act(() => {
                result.current.handleOpen('admin');
            });

            expect(result.current.open).toBe(true);
            expect(result.current.deleteLogin).toBe('admin');
        });

        it('opens dialog with null login', () => {
            const {result} = renderHook(() => useDeleteUserAlertDialog());

            act(() => {
                result.current.handleOpen(null);
            });

            expect(result.current.open).toBe(false);
            expect(result.current.deleteLogin).toBeNull();
        });
    });

    describe('close dialog', () => {
        it('closes dialog', () => {
            const {result} = renderHook(() => useDeleteUserAlertDialog());

            act(() => {
                result.current.handleOpen('admin');
            });

            act(() => {
                result.current.handleClose();
            });

            expect(result.current.open).toBe(false);
            expect(result.current.deleteLogin).toBeNull();
        });
    });

    describe('handle delete', () => {
        it('calls delete mutation with correct login', () => {
            const {result} = renderHook(() => useDeleteUserAlertDialog());

            act(() => {
                result.current.handleOpen('admin');
            });

            act(() => {
                result.current.handleDelete();
            });

            expect(hoisted.deleteUserMutate).toHaveBeenCalledWith({login: 'admin'});
        });

        it('does not call delete mutation when deleteLogin is null', () => {
            const {result} = renderHook(() => useDeleteUserAlertDialog());

            act(() => {
                result.current.handleDelete();
            });

            expect(hoisted.deleteUserMutate).not.toHaveBeenCalled();
        });

        it('closes dialog after successful delete', () => {
            const {result} = renderHook(() => useDeleteUserAlertDialog());

            act(() => {
                result.current.handleOpen('admin');
            });

            act(() => {
                result.current.handleDelete();
            });

            expect(result.current.open).toBe(false);
            expect(result.current.deleteLogin).toBeNull();
        });
    });
});
