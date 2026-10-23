import {useIdentityProviderDialogStore} from '@/ee/pages/settings/platform/identity-providers/stores/useIdentityProviderDialogStore';
import {IdentityProviderType} from '@/shared/middleware/graphql';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useIdentityProviderDialog from '../hooks/useIdentityProviderDialog';

vi.mock('@/shared/middleware/graphql', () => ({
    useCreateIdentityProviderMutation: () => ({isPending: false, mutate: vi.fn()}),
    useUpdateIdentityProviderMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useQueryClient: () => ({invalidateQueries: vi.fn()}),
}));

const samlIdentityProvider = {
    autoProvision: false,
    defaultAuthority: 'ROLE_ADMIN',
    domains: ['acme.example.com'],
    enabled: true,
    enforced: true,
    id: '7',
    metadataUri: 'https://idp.acme.example.com/metadata',
    mfaRequired: false,
    name: 'Acme SAML',
    registrationId: 'saml-7',
    scimEnabled: false,
    type: 'SAML',
} as IdentityProviderType;

describe('useIdentityProviderDialog', () => {
    beforeEach(() => {
        useIdentityProviderDialogStore.setState({identityProvider: null, open: false});
    });

    it('fills the dialog form when another component opens it for editing', () => {
        const {result: tableResult} = renderHook(() => useIdentityProviderDialog());
        const {result: dialogResult} = renderHook(() => useIdentityProviderDialog());

        act(() => {
            tableResult.current.handleOpenEdit(samlIdentityProvider);
        });

        expect(dialogResult.current.open).toBe(true);
        expect(dialogResult.current.isEditing).toBe(true);
        expect(dialogResult.current.providerType).toBe('SAML');
        expect(dialogResult.current.name).toBe('Acme SAML');
        expect(dialogResult.current.metadataUri).toBe('https://idp.acme.example.com/metadata');
        expect(dialogResult.current.domains).toEqual(['acme.example.com']);
        expect(dialogResult.current.defaultAuthority).toBe('ROLE_ADMIN');
    });

    it('clears an earlier edit when another component opens it for creating', () => {
        const {result: tableResult} = renderHook(() => useIdentityProviderDialog());
        const {result: pageResult} = renderHook(() => useIdentityProviderDialog());
        const {result: dialogResult} = renderHook(() => useIdentityProviderDialog());

        act(() => {
            tableResult.current.handleOpenEdit(samlIdentityProvider);
        });

        act(() => {
            dialogResult.current.handleClose();
        });

        act(() => {
            pageResult.current.handleOpenCreate();
        });

        expect(dialogResult.current.isEditing).toBe(false);
        expect(dialogResult.current.providerType).toBe('OIDC');
        expect(dialogResult.current.name).toBe('');
        expect(dialogResult.current.domains).toEqual([]);
    });
});
