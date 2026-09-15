import {act} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

import {useInviteUserDialogStore} from '../useInviteUserDialogStore';

describe('useInviteUserDialogStore', () => {
    beforeEach(() => {
        act(() => {
            useInviteUserDialogStore.getState().reset();
        });
    });

    describe('initial state', () => {
        it('has open as false', () => {
            const state = useInviteUserDialogStore.getState();

            expect(state.open).toBe(false);
        });

        it('has empty inviteEmail', () => {
            const state = useInviteUserDialogStore.getState();

            expect(state.inviteEmail).toBe('');
        });

        it('has inviteRole as null', () => {
            const state = useInviteUserDialogStore.getState();

            expect(state.inviteRole).toBeNull();
        });

        it('has no inviteWorkspaces', () => {
            const state = useInviteUserDialogStore.getState();

            expect(state.inviteWorkspaces).toEqual([]);
        });
    });

    describe('setOpen', () => {
        it('sets open to true', () => {
            act(() => {
                useInviteUserDialogStore.getState().setOpen();
            });

            expect(useInviteUserDialogStore.getState().open).toBe(true);
        });
    });

    describe('setInviteEmail', () => {
        it('sets the email', () => {
            act(() => {
                useInviteUserDialogStore.getState().setInviteEmail('user@example.com');
            });

            expect(useInviteUserDialogStore.getState().inviteEmail).toBe('user@example.com');
        });
    });

    describe('setInviteRole', () => {
        it('sets the role', () => {
            act(() => {
                useInviteUserDialogStore.getState().setInviteRole('ROLE_ADMIN');
            });

            expect(useInviteUserDialogStore.getState().inviteRole).toBe('ROLE_ADMIN');
        });
    });

    describe('toggleInviteWorkspace', () => {
        it('adds a workspace with the given role', () => {
            act(() => {
                useInviteUserDialogStore.getState().toggleInviteWorkspace('1', 'EDITOR');
            });

            expect(useInviteUserDialogStore.getState().inviteWorkspaces).toEqual([
                {roleName: 'EDITOR', workspaceId: '1'},
            ]);
        });

        it('removes a workspace already selected', () => {
            act(() => {
                useInviteUserDialogStore.getState().toggleInviteWorkspace('1', 'EDITOR');
                useInviteUserDialogStore.getState().toggleInviteWorkspace('1', 'EDITOR');
            });

            expect(useInviteUserDialogStore.getState().inviteWorkspaces).toEqual([]);
        });

        it('keeps workspaces independent of one another', () => {
            act(() => {
                useInviteUserDialogStore.getState().toggleInviteWorkspace('1', 'EDITOR');
                useInviteUserDialogStore.getState().toggleInviteWorkspace('2', 'VIEWER');
                useInviteUserDialogStore.getState().toggleInviteWorkspace('1', 'EDITOR');
            });

            expect(useInviteUserDialogStore.getState().inviteWorkspaces).toEqual([
                {roleName: 'VIEWER', workspaceId: '2'},
            ]);
        });
    });

    describe('setInviteWorkspaceRole', () => {
        it('changes the role of one workspace without touching the others', () => {
            act(() => {
                useInviteUserDialogStore.getState().toggleInviteWorkspace('1', 'EDITOR');
                useInviteUserDialogStore.getState().toggleInviteWorkspace('2', 'EDITOR');
                useInviteUserDialogStore.getState().setInviteWorkspaceRole('2', 'ADMIN');
            });

            expect(useInviteUserDialogStore.getState().inviteWorkspaces).toEqual([
                {roleName: 'EDITOR', workspaceId: '1'},
                {roleName: 'ADMIN', workspaceId: '2'},
            ]);
        });
    });

    describe('reset', () => {
        it('clears every field including the selected workspaces', () => {
            act(() => {
                useInviteUserDialogStore.getState().setOpen();
                useInviteUserDialogStore.getState().setInviteEmail('user@example.com');
                useInviteUserDialogStore.getState().setInviteRole('ROLE_ADMIN');
                useInviteUserDialogStore.getState().toggleInviteWorkspace('1', 'EDITOR');
            });

            act(() => {
                useInviteUserDialogStore.getState().reset();
            });

            const state = useInviteUserDialogStore.getState();

            expect(state.open).toBe(false);
            expect(state.inviteEmail).toBe('');
            expect(state.inviteRole).toBeNull();
            expect(state.inviteWorkspaces).toEqual([]);
        });
    });
});
