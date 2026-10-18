import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {useGenerateIdentityProviderScimApiKeyMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {KeyRoundIcon} from 'lucide-react';
import {useState} from 'react';

interface IdentityProviderScimSectionPropsI {
    identityProviderId: string;
    scimEnabled: boolean;
}

const IdentityProviderScimSection = ({identityProviderId, scimEnabled}: IdentityProviderScimSectionPropsI) => {
    const [scimApiKey, setScimApiKey] = useState<string | null>(null);

    const queryClient = useQueryClient();

    const generateScimApiKeyMutation = useGenerateIdentityProviderScimApiKeyMutation({
        onSuccess: (data) => {
            setScimApiKey(data.generateIdentityProviderScimApiKey);

            queryClient.invalidateQueries({queryKey: ['identityProviders']});
        },
    });

    const scimBaseUrl = `${window.location.origin}/api/scim/v2`;

    return (
        <fieldset className="space-y-2 border-0 p-0">
            <label className="text-sm font-medium">SCIM Provisioning</label>

            <Input aria-label="SCIM base URL" readOnly value={scimBaseUrl} />

            {scimApiKey && (
                <>
                    <Input aria-label="SCIM token" readOnly value={scimApiKey} />

                    <p className="text-xs text-muted-foreground">
                        Copy this token into your identity provider now. It will not be shown again.
                    </p>
                </>
            )}

            <Button
                disabled={generateScimApiKeyMutation.isPending}
                icon={<KeyRoundIcon className="size-4" />}
                label={scimEnabled || scimApiKey ? 'Rotate SCIM Token' : 'Generate SCIM Token'}
                onClick={() => generateScimApiKeyMutation.mutate({id: identityProviderId})}
                variant="outline"
            />

            <p className="text-xs text-muted-foreground">
                Your identity provider uses the base URL and this bearer token to create, update and deactivate users.
                Rotating the token invalidates the previous one.
            </p>
        </fieldset>
    );
};

export default IdentityProviderScimSection;
