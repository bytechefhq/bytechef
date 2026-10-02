import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import IdentityProviderDialog from '../IdentityProviderDialog';

const hoisted = vi.hoisted(() => ({
    handleOpenChange: vi.fn(),
    handleSave: vi.fn(),
    state: {
        isEditing: false,
        name: 'Acme SSO',
        open: true,
        providerType: 'OIDC' as 'OIDC' | 'SAML',
    },
}));

vi.mock('../hooks/useIdentityProviderDialog', () => ({
    default: () => ({
        certificate: '',
        clientId: 'client-id',
        clientSecret: 'client-secret',
        domainInput: '',
        domains: ['acme.com'],
        editingProviderId: undefined,
        handleAddDomain: vi.fn(),
        handleOpenChange: hoisted.handleOpenChange,
        handleRemoveDomain: vi.fn(),
        handleSave: hoisted.handleSave,
        isAutoProvision: false,
        isEditing: hoisted.state.isEditing,
        isEnabled: true,
        isEnforced: false,
        isMfaRequired: false,
        issuerUri: 'https://accounts.example.com',
        metadataUri: '',
        mfaMethod: undefined,
        name: hoisted.state.name,
        open: hoisted.state.open,
        providerType: hoisted.state.providerType,
        scopes: '',
        setCertificate: vi.fn(),
        setClientId: vi.fn(),
        setClientSecret: vi.fn(),
        setDomainInput: vi.fn(),
        setIsAutoProvision: vi.fn(),
        setIsEnabled: vi.fn(),
        setIsEnforced: vi.fn(),
        setIsMfaRequired: vi.fn(),
        setIssuerUri: vi.fn(),
        setMetadataUri: vi.fn(),
        setMfaMethod: vi.fn(),
        setName: vi.fn(),
        setProviderType: vi.fn(),
        setScopes: vi.fn(),
    }),
}));

beforeEach(() => {
    windowResizeObserver();
    hoisted.state.isEditing = false;
    hoisted.state.name = 'Acme SSO';
    hoisted.state.open = true;
    hoisted.state.providerType = 'OIDC';
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('IdentityProviderDialog', () => {
    describe('rendering', () => {
        it('should render the add title when not editing', () => {
            render(<IdentityProviderDialog />);

            expect(screen.getByText('Add Identity Provider')).toBeInTheDocument();
        });

        it('should render the edit title when editing', () => {
            hoisted.state.isEditing = true;

            render(<IdentityProviderDialog />);

            expect(screen.getByText('Edit Identity Provider')).toBeInTheDocument();
        });

        it('should explain what the dialog configures', () => {
            render(<IdentityProviderDialog />);

            expect(
                screen.getByText('Configure an OIDC or SAML identity provider for Single Sign-On.')
            ).toBeInTheDocument();
        });

        it('should not render when closed', () => {
            hoisted.state.open = false;

            render(<IdentityProviderDialog />);

            expect(screen.queryByText('Add Identity Provider')).not.toBeInTheDocument();
        });
    });

    describe('dialog chrome', () => {
        it('should render the close, cancel and save controls', () => {
            render(<IdentityProviderDialog />);

            expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Create'})).toBeInTheDocument();
        });

        it('should label the primary action Save when editing', () => {
            hoisted.state.isEditing = true;

            render(<IdentityProviderDialog />);

            expect(screen.getByRole('button', {name: 'Save'})).toBeInTheDocument();
        });

        it('should keep the cancel button out of any form submission', () => {
            render(<IdentityProviderDialog />);

            expect(screen.getByRole('button', {name: 'Cancel'})).toHaveAttribute('type', 'button');
        });
    });

    describe('interactions', () => {
        // Cancel closes through DialogClose, which drives onOpenChange — the
        // dialog must not also wire its own close handler onto the button.
        it('should close exactly once when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(<IdentityProviderDialog />);

            await user.click(screen.getByRole('button', {name: 'Cancel'}));

            expect(hoisted.handleOpenChange).toHaveBeenCalledTimes(1);
            expect(hoisted.handleOpenChange).toHaveBeenCalledWith(false);
        });

        it('should close when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(<IdentityProviderDialog />);

            await user.click(screen.getByRole('button', {name: 'Close'}));

            expect(hoisted.handleOpenChange).toHaveBeenCalledWith(false);
        });

        it('should save when the primary action is clicked', async () => {
            const user = userEvent.setup();

            render(<IdentityProviderDialog />);

            await user.click(screen.getByRole('button', {name: 'Create'}));

            expect(hoisted.handleSave).toHaveBeenCalledTimes(1);
        });
    });
});
