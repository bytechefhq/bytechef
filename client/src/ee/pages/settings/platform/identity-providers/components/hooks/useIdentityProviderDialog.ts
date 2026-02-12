import {useIdentityProviderDialogStore} from '@/ee/pages/settings/platform/identity-providers/stores/useIdentityProviderDialogStore';
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
    issuerUri: string;
    name: string;
    open: boolean;
    scopes: string;
    setClientId: (value: string) => void;
    setClientSecret: (value: string) => void;
    setDefaultAuthority: (value: string) => void;
    setDomainInput: (value: string) => void;
    setIsAutoProvision: (value: boolean) => void;
    setIsEnabled: (value: boolean) => void;
    setIsEnforced: (value: boolean) => void;
    setIssuerUri: (value: string) => void;
    setName: (value: string) => void;
    setScopes: (value: string) => void;
}

export default function useIdentityProviderDialog(): UseIdentityProviderDialogI {
    const {identityProvider, open, reset, setIdentityProvider, setOpen} = useIdentityProviderDialogStore();

    const [name, setName] = useState('');
    const [issuerUri, setIssuerUri] = useState('');
    const [clientId, setClientId] = useState('');
    const [clientSecret, setClientSecret] = useState('');
    const [scopes, setScopes] = useState('openid,profile,email');
    const [domains, setDomains] = useState<string[]>([]);
    const [domainInput, setDomainInput] = useState('');
    const [isAutoProvision, setIsAutoProvision] = useState(true);
    const [defaultAuthority, setDefaultAuthority] = useState('ROLE_USER');
    const [isEnforced, setIsEnforced] = useState(false);
    const [isEnabled, setIsEnabled] = useState(true);

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
        setName('');
        setIssuerUri('');
        setClientId('');
        setClientSecret('');
        setScopes('openid,profile,email');
        setDomains([]);
        setDomainInput('');
        setIsAutoProvision(true);
        setDefaultAuthority('ROLE_USER');
        setIsEnforced(false);
        setIsEnabled(true);
    };

    const populateForm = (provider: IdentityProviderType) => {
        setName(provider.name);
        setIssuerUri(provider.issuerUri);
        setClientId(provider.clientId);
        setClientSecret('');
        setScopes(provider.scopes);
        setDomains([...provider.domains]);
        setDomainInput('');
        setIsAutoProvision(provider.autoProvision);
        setDefaultAuthority(provider.defaultAuthority);
        setIsEnforced(provider.enforced);
        setIsEnabled(provider.enabled);
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
            clientId,
            clientSecret: clientSecret || undefined,
            defaultAuthority,
            domains,
            enabled: isEnabled,
            enforced: isEnforced,
            issuerUri,
            name,
            scopes,
            type: 'OIDC',
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
        issuerUri,
        name,
        open,
        scopes,
        setClientId,
        setClientSecret,
        setDefaultAuthority,
        setDomainInput,
        setIsAutoProvision,
        setIsEnabled,
        setIsEnforced,
        setIssuerUri,
        setName,
        setScopes,
    };
}
