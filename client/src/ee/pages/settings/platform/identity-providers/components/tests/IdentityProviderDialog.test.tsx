import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import IdentityProviderDialog from '../IdentityProviderDialog';

const hoisted = vi.hoisted(() => ({
    handleAddAuthorityMapping: vi.fn(),
    handleOpenChange: vi.fn(),
    handleSave: vi.fn(),
    setIsMcpEmbedded: vi.fn(),
    setIsMcpManagement: vi.fn(),
    state: {
        authorityMappings: [] as {authority: string; externalGroup: string}[],
        isEditing: false,
        isMcpAutomation: false,
        isMcpEmbedded: false,
        name: 'Acme SSO',
        open: true,
        providerType: 'OIDC' as 'OIDC' | 'SAML',
    },
}));

vi.mock('../hooks/useIdentityProviderDialog', () => ({
    default: () => ({
        authoritiesClaim: '',
        authorityMappings: hoisted.state.authorityMappings,
        certificate: '',
        clientId: 'client-id',
        clientSecret: 'client-secret',
        domainInput: '',
        domains: ['acme.com'],
        editingProviderId: undefined,
        handleAddAuthorityMapping: hoisted.handleAddAuthorityMapping,
        handleAddDomain: vi.fn(),
        handleOpenChange: hoisted.handleOpenChange,
        handleRemoveAuthorityMapping: vi.fn(),
        handleRemoveDomain: vi.fn(),
        handleSave: hoisted.handleSave,
        handleUpdateAuthorityMapping: vi.fn(),
        isAutoProvision: false,
        isEditing: hoisted.state.isEditing,
        isEnabled: true,
        isEnforced: false,
        isMcpAutomation: hoisted.state.isMcpAutomation,
        isMcpEmbedded: hoisted.state.isMcpEmbedded,
        isMcpManagement: false,
        isMfaRequired: false,
        isValidateMcpAudience: false,
        issuerUri: 'https://accounts.example.com',
        metadataUri: '',
        mfaMethod: undefined,
        name: hoisted.state.name,
        open: hoisted.state.open,
        providerType: hoisted.state.providerType,
        scopes: '',
        setAuthoritiesClaim: vi.fn(),
        setCertificate: vi.fn(),
        setClientId: vi.fn(),
        setClientSecret: vi.fn(),
        setDomainInput: vi.fn(),
        setIsAutoProvision: vi.fn(),
        setIsEnabled: vi.fn(),
        setIsEnforced: vi.fn(),
        setIsMcpAutomation: vi.fn(),
        setIsMcpEmbedded: hoisted.setIsMcpEmbedded,
        setIsMcpManagement: hoisted.setIsMcpManagement,
        setIsMfaRequired: vi.fn(),
        setIsValidateMcpAudience: vi.fn(),
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
    hoisted.state.authorityMappings = [];
    hoisted.state.isEditing = false;
    hoisted.state.isMcpAutomation = false;
    hoisted.state.isMcpEmbedded = false;
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

    describe('MCP trust', () => {
        it('should offer the automation, management and embedded MCP surfaces for an OIDC provider', () => {
            render(<IdentityProviderDialog />);

            expect(screen.getByLabelText('Use for automation MCP servers')).toBeInTheDocument();
            expect(screen.getByLabelText('Use for management MCP servers')).toBeInTheDocument();
            expect(screen.getByLabelText('Use for embedded MCP servers')).toBeInTheDocument();
        });

        it('should not offer MCP surfaces for a SAML provider', () => {
            hoisted.state.providerType = 'SAML';

            render(<IdentityProviderDialog />);

            expect(screen.queryByLabelText('Use for automation MCP servers')).not.toBeInTheDocument();
            expect(screen.queryByLabelText('Use for management MCP servers')).not.toBeInTheDocument();
            expect(screen.queryByLabelText('Use for embedded MCP servers')).not.toBeInTheDocument();
        });

        it('should hide the audience, groups claim and mappings until an MCP surface is enabled', () => {
            render(<IdentityProviderDialog />);

            expect(screen.queryByLabelText('Require token audience to match the MCP endpoint')).not.toBeInTheDocument();
            expect(screen.queryByText('Groups Claim')).not.toBeInTheDocument();
            expect(screen.queryByRole('button', {name: 'Add mapping'})).not.toBeInTheDocument();
        });

        it('should show the audience, groups claim and existing mappings once an MCP surface is enabled', () => {
            hoisted.state.authorityMappings = [{authority: 'ROLE_SALES', externalGroup: 'sales'}];
            hoisted.state.isMcpAutomation = true;

            render(<IdentityProviderDialog />);

            expect(screen.getByLabelText('Require token audience to match the MCP endpoint')).toBeInTheDocument();
            expect(screen.getByText('Groups Claim')).toBeInTheDocument();
            expect(screen.getByDisplayValue('sales')).toBeInTheDocument();
            expect(screen.getByDisplayValue('ROLE_SALES')).toBeInTheDocument();
        });

        it('should enable the management surface when its checkbox is clicked', async () => {
            const user = userEvent.setup();

            render(<IdentityProviderDialog />);

            await user.click(screen.getByLabelText('Use for management MCP servers'));

            expect(hoisted.setIsMcpManagement).toHaveBeenCalledWith(true);
        });

        it('should enable the embedded surface when its checkbox is clicked', async () => {
            const user = userEvent.setup();

            render(<IdentityProviderDialog />);

            await user.click(screen.getByLabelText('Use for embedded MCP servers'));

            expect(hoisted.setIsMcpEmbedded).toHaveBeenCalledWith(true);
        });

        it('should show the audience, groups claim and mappings when only the embedded surface is enabled', () => {
            hoisted.state.isMcpEmbedded = true;

            render(<IdentityProviderDialog />);

            expect(screen.getByLabelText('Require token audience to match the MCP endpoint')).toBeInTheDocument();
            expect(screen.getByText('Groups Claim')).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Add mapping'})).toBeInTheDocument();
        });

        it('should add an authority mapping row when requested', async () => {
            const user = userEvent.setup();

            hoisted.state.isMcpAutomation = true;

            render(<IdentityProviderDialog />);

            await user.click(screen.getByRole('button', {name: 'Add mapping'}));

            expect(hoisted.handleAddAuthorityMapping).toHaveBeenCalledTimes(1);
        });
    });
});
