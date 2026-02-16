import Button from '@/components/Button/Button';
import {Badge} from '@/components/ui/badge';
import {Checkbox} from '@/components/ui/checkbox';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Textarea} from '@/components/ui/textarea';
import {DownloadIcon, XIcon} from 'lucide-react';
import {useMemo} from 'react';

import useIdentityProviderDialog from './hooks/useIdentityProviderDialog';

const IdentityProviderDialog = () => {
    const {
        clientId,
        clientSecret,
        defaultAuthority,
        domainInput,
        domains,
        editingProviderId,
        handleAddDomain,
        handleClose,
        handleOpenChange,
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
    } = useIdentityProviderDialog();

    const saveDisabled = useMemo(() => {
        if (!name || domains.length === 0) {
            return true;
        }

        if (providerType === 'OIDC') {
            return !issuerUri || !clientId;
        }

        return !metadataUri;
    }, [clientId, domains.length, issuerUri, metadataUri, name, providerType]);

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-lg">
                <div className="flex flex-col gap-4">
                    <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                        <DialogTitle>{isEditing ? 'Edit Identity Provider' : 'Add Identity Provider'}</DialogTitle>

                        <DialogCloseButton />
                    </DialogHeader>

                    <p className="text-sm text-muted-foreground">
                        Configure an OIDC or SAML identity provider for Single Sign-On.
                    </p>

                    <div className="space-y-4">
                        <fieldset className="space-y-2 border-0 p-0">
                            <label className="text-sm font-medium">Type</label>

                            <Select disabled={isEditing} onValueChange={setProviderType} value={providerType}>
                                <SelectTrigger>
                                    <SelectValue placeholder="Select type" />
                                </SelectTrigger>

                                <SelectContent>
                                    <SelectItem value="OIDC">OIDC</SelectItem>

                                    <SelectItem value="SAML">SAML</SelectItem>
                                </SelectContent>
                            </Select>
                        </fieldset>

                        <fieldset className="space-y-2 border-0 p-0">
                            <label className="text-sm font-medium">Name</label>

                            <Input
                                onChange={(event) => setName(event.target.value)}
                                placeholder="Acme Corp SSO"
                                value={name}
                            />
                        </fieldset>

                        {providerType === 'OIDC' && (
                            <>
                                <fieldset className="space-y-2 border-0 p-0">
                                    <label className="text-sm font-medium">Issuer URI</label>

                                    <Input
                                        onChange={(event) => setIssuerUri(event.target.value)}
                                        placeholder="https://accounts.google.com"
                                        value={issuerUri}
                                    />

                                    <p className="text-xs text-muted-foreground">
                                        The OIDC issuer URI. Auto-discovery will fetch endpoints from
                                        .well-known/openid-configuration.
                                    </p>
                                </fieldset>

                                <fieldset className="space-y-2 border-0 p-0">
                                    <label className="text-sm font-medium">Client ID</label>

                                    <Input
                                        onChange={(event) => setClientId(event.target.value)}
                                        placeholder="your-client-id"
                                        value={clientId}
                                    />
                                </fieldset>

                                <fieldset className="space-y-2 border-0 p-0">
                                    <label className="text-sm font-medium">Client Secret</label>

                                    <Input
                                        onChange={(event) => setClientSecret(event.target.value)}
                                        placeholder={
                                            isEditing ? 'Leave blank to keep current secret' : 'your-client-secret'
                                        }
                                        type="password"
                                        value={clientSecret}
                                    />
                                </fieldset>

                                <fieldset className="space-y-2 border-0 p-0">
                                    <label className="text-sm font-medium">Scopes</label>

                                    <Input
                                        onChange={(event) => setScopes(event.target.value)}
                                        placeholder="openid,profile,email"
                                        value={scopes}
                                    />
                                </fieldset>
                            </>
                        )}

                        {providerType === 'SAML' && (
                            <>
                                <fieldset className="space-y-2 border-0 p-0">
                                    <label className="text-sm font-medium">Metadata URI</label>

                                    <Input
                                        onChange={(event) => setMetadataUri(event.target.value)}
                                        placeholder="https://idp.example.com/metadata"
                                        value={metadataUri}
                                    />

                                    <p className="text-xs text-muted-foreground">
                                        The SAML IdP metadata URL. Used to auto-configure SSO endpoints and
                                        certificates.
                                    </p>
                                </fieldset>

                                <fieldset className="space-y-2 border-0 p-0">
                                    <label className="text-sm font-medium">Signing Certificate (optional)</label>

                                    <Textarea
                                        className="font-mono text-xs"
                                        onChange={(event) => setSigningCertificate(event.target.value)}
                                        placeholder="-----BEGIN CERTIFICATE-----&#10;...&#10;-----END CERTIFICATE-----"
                                        rows={4}
                                        value={signingCertificate}
                                    />

                                    <p className="text-xs text-muted-foreground">
                                        PEM-encoded IdP signing certificate. Only needed if not included in the
                                        metadata.
                                    </p>
                                </fieldset>

                                <fieldset className="space-y-2 border-0 p-0">
                                    <label className="text-sm font-medium">NameID Format (optional)</label>

                                    <Select onValueChange={setNameIdFormat} value={nameIdFormat}>
                                        <SelectTrigger>
                                            <SelectValue placeholder="Default (from metadata)" />
                                        </SelectTrigger>

                                        <SelectContent>
                                            <SelectItem value="urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress">
                                                Email Address
                                            </SelectItem>

                                            <SelectItem value="urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified">
                                                Unspecified
                                            </SelectItem>

                                            <SelectItem value="urn:oasis:names:tc:SAML:2.0:nameid-format:persistent">
                                                Persistent
                                            </SelectItem>

                                            <SelectItem value="urn:oasis:names:tc:SAML:2.0:nameid-format:transient">
                                                Transient
                                            </SelectItem>
                                        </SelectContent>
                                    </Select>
                                </fieldset>

                                {isEditing && (
                                    <fieldset className="space-y-2 border-0 p-0">
                                        <label className="text-sm font-medium">SP Metadata</label>

                                        <Button
                                            icon={<DownloadIcon className="size-4" />}
                                            label="Download SP Metadata"
                                            onClick={() => {
                                                window.open(`/api/saml2/metadata/saml-${editingProviderId}`, '_blank');
                                            }}
                                            variant="outline"
                                        />

                                        <p className="text-xs text-muted-foreground">
                                            Download the Service Provider metadata XML to configure your IdP.
                                        </p>
                                    </fieldset>
                                )}
                            </>
                        )}

                        <fieldset className="space-y-2 border-0 p-0">
                            <label className="text-sm font-medium">Email Domains</label>

                            <div className="flex gap-2">
                                <Input
                                    onChange={(event) => setDomainInput(event.target.value)}
                                    onKeyDown={(event) => {
                                        if (event.key === 'Enter') {
                                            event.preventDefault();
                                            handleAddDomain();
                                        }
                                    }}
                                    placeholder="acme.com"
                                    value={domainInput}
                                />

                                <Button onClick={handleAddDomain} type="button" variant="outline">
                                    Add
                                </Button>
                            </div>

                            {domains.length > 0 && (
                                <div className="flex flex-wrap gap-1 pt-1">
                                    {domains.map((domain) => (
                                        <Badge className="gap-1" key={domain} variant="secondary">
                                            {domain}

                                            <button
                                                className="ml-1 rounded-full hover:bg-muted"
                                                onClick={() => handleRemoveDomain(domain)}
                                                type="button"
                                            >
                                                <XIcon className="size-3" />
                                            </button>
                                        </Badge>
                                    ))}
                                </div>
                            )}

                            <p className="text-xs text-muted-foreground">
                                Users with these email domains will be redirected to this SSO provider.
                            </p>
                        </fieldset>

                        <fieldset className="space-y-2 border-0 p-0">
                            <label className="text-sm font-medium">Default Role</label>

                            <Select onValueChange={setDefaultAuthority} value={defaultAuthority}>
                                <SelectTrigger>
                                    <SelectValue placeholder="Select role" />
                                </SelectTrigger>

                                <SelectContent>
                                    <SelectItem value="ROLE_USER">ROLE_USER</SelectItem>

                                    <SelectItem value="ROLE_ADMIN">ROLE_ADMIN</SelectItem>
                                </SelectContent>
                            </Select>
                        </fieldset>

                        <div className="space-y-3 pt-2">
                            <div className="flex items-center space-x-2">
                                <Checkbox
                                    checked={isAutoProvision}
                                    id="autoProvision"
                                    onCheckedChange={(checked) => setIsAutoProvision(checked === true)}
                                />

                                <label className="text-sm font-normal" htmlFor="autoProvision">
                                    Auto-provision users on first SSO login
                                </label>
                            </div>

                            <div className="flex items-center space-x-2">
                                <Checkbox
                                    checked={isEnforced}
                                    id="enforced"
                                    onCheckedChange={(checked) => setIsEnforced(checked === true)}
                                />

                                <label className="text-sm font-normal" htmlFor="enforced">
                                    Enforce SSO (block password login for matching domains)
                                </label>
                            </div>

                            <div className="flex items-center space-x-2">
                                <Checkbox
                                    checked={isEnabled}
                                    id="enabled"
                                    onCheckedChange={(checked) => setIsEnabled(checked === true)}
                                />

                                <label className="text-sm font-normal" htmlFor="enabled">
                                    Enabled
                                </label>
                            </div>

                            <div className="flex items-center space-x-2">
                                <Checkbox
                                    checked={isMfaRequired}
                                    id="mfaRequired"
                                    onCheckedChange={(checked) => setIsMfaRequired(checked === true)}
                                />

                                <label className="text-sm font-normal" htmlFor="mfaRequired">
                                    Require MFA (policy only)
                                </label>
                            </div>

                            {isMfaRequired && (
                                <fieldset className="space-y-2 border-0 p-0 pl-6">
                                    <label className="text-sm font-medium">MFA Method</label>

                                    <Select disabled onValueChange={setMfaMethod} value={mfaMethod}>
                                        <SelectTrigger>
                                            <SelectValue placeholder="Select method" />
                                        </SelectTrigger>

                                        <SelectContent>
                                            <SelectItem value="TOTP">TOTP</SelectItem>

                                            <SelectItem value="EMAIL">Email</SelectItem>
                                        </SelectContent>
                                    </Select>

                                    <p className="text-xs text-muted-foreground">
                                        MFA enforcement is a policy flag only. Actual MFA implementation is planned for
                                        a future release.
                                    </p>
                                </fieldset>
                            )}
                        </div>
                    </div>

                    <DialogFooter>
                        <DialogClose asChild>
                            <Button onClick={handleClose} variant="outline">
                                Cancel
                            </Button>
                        </DialogClose>

                        <Button disabled={saveDisabled} onClick={handleSave}>
                            {isEditing ? 'Save' : 'Create'}
                        </Button>
                    </DialogFooter>
                </div>
            </DialogContent>
        </Dialog>
    );
};

export default IdentityProviderDialog;
