import {useIdentityProviderDialogStore} from '@/ee/pages/settings/platform/identity-providers/stores/useIdentityProviderDialogStore';
import {
    AuthorityMappingInput,
    IdentityProviderInput,
    IdentityProviderType,
    useCreateIdentityProviderMutation,
    useUpdateIdentityProviderMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

interface UseIdentityProviderDialogI {
    authoritiesClaim: string;
    authorityMappings: AuthorityMappingInput[];
    clientId: string;
    clientSecret: string;
    defaultAuthority: string;
    domainInput: string;
    domains: string[];
    editingProviderId: string | null;
    handleAddAuthorityMapping: () => void;
    handleAddDomain: () => void;
    handleClose: () => void;
    handleOpenChange: (open: boolean) => void;
    handleOpenCreate: () => void;
    handleOpenEdit: (identityProvider: IdentityProviderType) => void;
    handleRemoveAuthorityMapping: (index: number) => void;
    handleRemoveDomain: (domain: string) => void;
    handleSave: () => void;
    handleUpdateAuthorityMapping: (index: number, field: 'authority' | 'externalGroup', value: string) => void;
    isAutoProvision: boolean;
    isEditing: boolean;
    isEnabled: boolean;
    isEnforced: boolean;
    isMcpEmbedded: boolean;
    isMcpAutomation: boolean;
    isMcpManagement: boolean;
    isMfaRequired: boolean;
    isValidateMcpAudience: boolean;
    issuerUri: string;
    metadataUri: string;
    mfaMethod: string;
    name: string;
    nameIdFormat: string;
    open: boolean;
    providerType: string;
    scopes: string;
    setAuthoritiesClaim: (value: string) => void;
    setClientId: (value: string) => void;
    setClientSecret: (value: string) => void;
    setDefaultAuthority: (value: string) => void;
    setDomainInput: (value: string) => void;
    setIsAutoProvision: (value: boolean) => void;
    setIsEnabled: (value: boolean) => void;
    setIsEnforced: (value: boolean) => void;
    setIsMcpEmbedded: (value: boolean) => void;
    setIsMcpAutomation: (value: boolean) => void;
    setIsMcpManagement: (value: boolean) => void;
    setIsMfaRequired: (value: boolean) => void;
    setIsValidateMcpAudience: (value: boolean) => void;
    setIssuerUri: (value: string) => void;
    setMetadataUri: (value: string) => void;
    setMfaMethod: (value: string) => void;
    setName: (value: string) => void;
    setNameIdFormat: (value: string) => void;
    setProviderType: (value: string) => void;
    setScopes: (value: string) => void;
    setSigningCertificate: (value: string) => void;
    signingCertificate: string;
}

export default function useIdentityProviderDialog(): UseIdentityProviderDialogI {
    const {identityProvider, open, reset, setIdentityProvider, setOpen} = useIdentityProviderDialogStore();

    const [providerType, setProviderType] = useState('OIDC');
    const [name, setName] = useState('');
    const [issuerUri, setIssuerUri] = useState('');
    const [clientId, setClientId] = useState('');
    const [clientSecret, setClientSecret] = useState('');
    const [scopes, setScopes] = useState('openid,profile,email');
    const [metadataUri, setMetadataUri] = useState('');
    const [signingCertificate, setSigningCertificate] = useState('');
    const [nameIdFormat, setNameIdFormat] = useState('');
    const [domains, setDomains] = useState<string[]>([]);
    const [domainInput, setDomainInput] = useState('');
    const [isAutoProvision, setIsAutoProvision] = useState(true);
    const [defaultAuthority, setDefaultAuthority] = useState('ROLE_USER');
    const [isEnforced, setIsEnforced] = useState(false);
    const [isEnabled, setIsEnabled] = useState(true);
    const [isMcpEmbedded, setIsMcpEmbedded] = useState(false);
    const [isMcpAutomation, setIsMcpAutomation] = useState(false);
    const [isMcpManagement, setIsMcpManagement] = useState(false);
    const [authorityMappings, setAuthorityMappings] = useState<AuthorityMappingInput[]>([]);
    const [authoritiesClaim, setAuthoritiesClaim] = useState('');
    const [isValidateMcpAudience, setIsValidateMcpAudience] = useState(false);
    const [isMfaRequired, setIsMfaRequired] = useState(false);
    const [mfaMethod, setMfaMethod] = useState('TOTP');

    const queryClient = useQueryClient();

    const createMutation = useCreateIdentityProviderMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['identityProviders']});
            handleClose();
        },
    });

    const updateMutation = useUpdateIdentityProviderMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['identityProviders']});
            handleClose();
        },
    });

    const isEditing = identityProvider !== null;

    const resetFormState = () => {
        setProviderType('OIDC');
        setName('');
        setIssuerUri('');
        setClientId('');
        setClientSecret('');
        setScopes('openid,profile,email');
        setMetadataUri('');
        setSigningCertificate('');
        setNameIdFormat('');
        setDomains([]);
        setDomainInput('');
        setIsAutoProvision(true);
        setDefaultAuthority('ROLE_USER');
        setIsEnforced(false);
        setIsEnabled(true);
        setIsMcpEmbedded(false);
        setIsMcpAutomation(false);
        setIsMcpManagement(false);
        setAuthorityMappings([]);
        setAuthoritiesClaim('');
        setIsValidateMcpAudience(false);
        setIsMfaRequired(false);
        setMfaMethod('TOTP');
    };

    const populateForm = (provider: IdentityProviderType) => {
        setProviderType(provider.type);
        setName(provider.name);
        setIssuerUri(provider.issuerUri || '');
        setClientId(provider.clientId || '');
        setClientSecret('');
        setScopes(provider.scopes || 'openid,profile,email');
        setMetadataUri(provider.metadataUri || '');
        setSigningCertificate(provider.signingCertificate || '');
        setNameIdFormat(provider.nameIdFormat || '');
        setDomains([...provider.domains]);
        setDomainInput('');
        setIsAutoProvision(provider.autoProvision);
        setDefaultAuthority(provider.defaultAuthority);
        setIsEnforced(provider.enforced);
        setIsEnabled(provider.enabled);
        setIsMcpEmbedded(provider.mcpEmbedded);
        setIsMcpAutomation(provider.mcpAutomation);
        setIsMcpManagement(provider.mcpManagement);
        setAuthorityMappings(
            provider.authorityMappings.map((authorityMapping) => ({
                authority: authorityMapping.authority,
                externalGroup: authorityMapping.externalGroup,
            }))
        );
        setAuthoritiesClaim(provider.authoritiesClaim || '');
        setIsValidateMcpAudience(provider.validateMcpAudience);
        setIsMfaRequired(provider.mfaRequired);
        setMfaMethod(provider.mfaMethod || 'TOTP');
    };

    const handleClose = () => {
        resetFormState();
        reset();
    };

    const handleOpenCreate = () => {
        resetFormState();
        setIdentityProvider(null);
        setOpen(true);
    };

    const handleOpenEdit = (provider: IdentityProviderType) => {
        populateForm(provider);
        setIdentityProvider(provider);
        setOpen(true);
    };

    const handleOpenChange = (openValue: boolean) => {
        if (!openValue) {
            handleClose();
        }
    };

    const handleAddDomain = () => {
        const trimmed = domainInput.trim().toLowerCase();

        if (trimmed && !domains.includes(trimmed)) {
            setDomains([...domains, trimmed]);
            setDomainInput('');
        }
    };

    const handleRemoveDomain = (domain: string) => {
        setDomains(domains.filter((existingDomain) => existingDomain !== domain));
    };

    const handleAddAuthorityMapping = () => {
        setAuthorityMappings([...authorityMappings, {authority: '', externalGroup: ''}]);
    };

    const handleRemoveAuthorityMapping = (index: number) => {
        setAuthorityMappings(authorityMappings.filter((_, mappingIndex) => mappingIndex !== index));
    };

    const handleUpdateAuthorityMapping = (index: number, field: 'authority' | 'externalGroup', value: string) => {
        setAuthorityMappings(
            authorityMappings.map((authorityMapping, mappingIndex) =>
                mappingIndex === index ? {...authorityMapping, [field]: value} : authorityMapping
            )
        );
    };

    const handleSave = () => {
        const isOidc = providerType === 'OIDC';

        const input: IdentityProviderInput = {
            authoritiesClaim: isOidc ? authoritiesClaim.trim() || undefined : undefined,
            authorityMappings: isOidc
                ? authorityMappings.filter(
                      (authorityMapping) => authorityMapping.externalGroup.trim() && authorityMapping.authority.trim()
                  )
                : [],
            autoProvision: isAutoProvision,
            clientId: isOidc ? clientId : undefined,
            clientSecret: isOidc ? clientSecret || undefined : undefined,
            defaultAuthority,
            domains,
            enabled: isEnabled,
            enforced: isEnforced,
            issuerUri: isOidc ? issuerUri : undefined,
            mcpAutomation: isOidc ? isMcpAutomation : false,
            mcpEmbedded: isOidc ? isMcpEmbedded : false,
            mcpManagement: isOidc ? isMcpManagement : false,
            metadataUri: providerType === 'SAML' ? metadataUri : undefined,
            mfaMethod: isMfaRequired ? mfaMethod : undefined,
            mfaRequired: isMfaRequired,
            name,
            nameIdFormat: providerType === 'SAML' ? nameIdFormat || undefined : undefined,
            scopes: isOidc ? scopes : undefined,
            signingCertificate: providerType === 'SAML' ? signingCertificate || undefined : undefined,
            type: providerType,
            validateMcpAudience: isOidc ? isValidateMcpAudience : false,
        };

        if (isEditing && identityProvider) {
            updateMutation.mutate({id: identityProvider.id, input});
        } else {
            createMutation.mutate({input});
        }
    };

    return {
        authoritiesClaim,
        authorityMappings,
        clientId,
        clientSecret,
        defaultAuthority,
        domainInput,
        domains,
        editingProviderId: identityProvider?.id || null,
        handleAddAuthorityMapping,
        handleAddDomain,
        handleClose,
        handleOpenChange,
        handleOpenCreate,
        handleOpenEdit,
        handleRemoveAuthorityMapping,
        handleRemoveDomain,
        handleSave,
        handleUpdateAuthorityMapping,
        isAutoProvision,
        isEditing,
        isEnabled,
        isEnforced,
        isMcpAutomation,
        isMcpEmbedded,
        isMcpManagement,
        isMfaRequired,
        isValidateMcpAudience,
        issuerUri,
        metadataUri,
        mfaMethod,
        name,
        nameIdFormat,
        open,
        providerType,
        scopes,
        setAuthoritiesClaim,
        setClientId,
        setClientSecret,
        setDefaultAuthority,
        setDomainInput,
        setIsAutoProvision,
        setIsEnabled,
        setIsEnforced,
        setIsMcpAutomation,
        setIsMcpEmbedded,
        setIsMcpManagement,
        setIsMfaRequired,
        setIsValidateMcpAudience,
        setIssuerUri,
        setMetadataUri,
        setMfaMethod,
        setName,
        setNameIdFormat,
        setProviderType,
        setScopes,
        setSigningCertificate,
        signingCertificate,
    };
}
