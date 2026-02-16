import {useIdentityProviderDialogStore} from '@/ee/pages/settings/platform/identity-providers/stores/useIdentityProviderDialogStore';
import {useToast} from '@/hooks/use-toast';
import {
    IdentityProviderInput,
    IdentityProviderType,
    useCreateIdentityProviderMutation,
    useUpdateIdentityProviderMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

interface UseIdentityProviderDialogI {
    clientId: string;
    clientSecret: string;
    defaultAuthority: string;
    domainInput: string;
    domains: string[];
    editingProviderId: string | null;
    handleAddDomain: () => void;
    handleClose: () => void;
    handleOpenChange: (open: boolean) => void;
    handleOpenCreate: () => void;
    handleOpenEdit: (identityProvider: IdentityProviderType) => void;
    handleRemoveDomain: (domain: string) => void;
    handleSave: () => void;
    isAutoProvision: boolean;
    isEditing: boolean;
    isEnabled: boolean;
    isEnforced: boolean;
    isMfaRequired: boolean;
    issuerUri: string;
    metadataUri: string;
    mfaMethod: string;
    name: string;
    nameIdFormat: string;
    open: boolean;
    providerType: string;
    scopes: string;
    setClientId: (value: string) => void;
    setClientSecret: (value: string) => void;
    setDefaultAuthority: (value: string) => void;
    setDomainInput: (value: string) => void;
    setIsAutoProvision: (value: boolean) => void;
    setIsEnabled: (value: boolean) => void;
    setIsEnforced: (value: boolean) => void;
    setIsMfaRequired: (value: boolean) => void;
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
    const [isMfaRequired, setIsMfaRequired] = useState(false);
    const [mfaMethod, setMfaMethod] = useState('TOTP');

    const queryClient = useQueryClient();
    const {toast} = useToast();

    const createMutation = useCreateIdentityProviderMutation({
        onError: () => {
            toast({description: 'Failed to create identity provider. Please try again.', variant: 'destructive'});
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['identityProviders']});
            handleClose();
        },
    });

    const updateMutation = useUpdateIdentityProviderMutation({
        onError: () => {
            toast({description: 'Failed to update identity provider. Please try again.', variant: 'destructive'});
        },
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
        setIsMfaRequired(false);
        setMfaMethod('TOTP');
    };

    const populateForm = (provider: IdentityProviderType) => {
        setProviderType(provider.type);
        setName(provider.name);
        setIssuerUri(provider.issuerUri);
        setClientId(provider.clientId);
        setClientSecret('');
        setScopes(provider.scopes);
        setMetadataUri(provider.metadataUri || '');
        setSigningCertificate(provider.signingCertificate || '');
        setNameIdFormat(provider.nameIdFormat || '');
        setDomains([...provider.domains]);
        setDomainInput('');
        setIsAutoProvision(provider.autoProvision);
        setDefaultAuthority(provider.defaultAuthority);
        setIsEnforced(provider.enforced);
        setIsEnabled(provider.enabled);
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

    const handleSave = () => {
        const input: IdentityProviderInput = {
            autoProvision: isAutoProvision,
            clientId: providerType === 'OIDC' ? clientId : undefined,
            clientSecret: providerType === 'OIDC' ? clientSecret || undefined : undefined,
            defaultAuthority,
            domains,
            enabled: isEnabled,
            enforced: isEnforced,
            issuerUri: providerType === 'OIDC' ? issuerUri : undefined,
            metadataUri: providerType === 'SAML' ? metadataUri : undefined,
            mfaMethod: isMfaRequired ? mfaMethod : undefined,
            mfaRequired: isMfaRequired,
            name,
            nameIdFormat: providerType === 'SAML' ? nameIdFormat || undefined : undefined,
            scopes: providerType === 'OIDC' ? scopes : undefined,
            signingCertificate: providerType === 'SAML' ? signingCertificate || undefined : undefined,
            type: providerType,
        };

        if (isEditing && identityProvider) {
            updateMutation.mutate({id: identityProvider.id, input});
        } else {
            createMutation.mutate({input});
        }
    };

    return {
        clientId,
        clientSecret,
        defaultAuthority,
        domainInput,
        domains,
        editingProviderId: identityProvider?.id || null,
        handleAddDomain,
        handleClose,
        handleOpenChange,
        handleOpenCreate,
        handleOpenEdit,
        handleRemoveDomain,
        handleSave,
        isAutoProvision,
        isEditing,
        isEnabled,
        isEnforced,
        isMfaRequired,
        issuerUri,
        metadataUri,
        mfaMethod,
        name,
        nameIdFormat,
        open,
        providerType,
        scopes,
        setClientId,
        setClientSecret,
        setDefaultAuthority,
        setDomainInput,
        setIsAutoProvision,
        setIsEnabled,
        setIsEnforced,
        setIsMfaRequired,
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
