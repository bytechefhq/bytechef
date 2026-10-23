import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import {Textarea} from '@/components/ui/textarea';
import {useRegenerateIdentityProviderServiceProviderCertificateMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {RefreshCwIcon} from 'lucide-react';
import {useState} from 'react';

interface IdentityProviderServiceProviderCertificateSectionPropsI {
    identityProviderId: string;
    serviceProviderCertificate: string | null;
}

const IdentityProviderServiceProviderCertificateSection = ({
    identityProviderId,
    serviceProviderCertificate,
}: IdentityProviderServiceProviderCertificateSectionPropsI) => {
    const [certificate, setCertificate] = useState<string | null>(serviceProviderCertificate);
    const [confirmOpen, setConfirmOpen] = useState(false);

    const queryClient = useQueryClient();

    const regenerateCertificateMutation = useRegenerateIdentityProviderServiceProviderCertificateMutation({
        onSuccess: (data) => {
            setCertificate(
                data.regenerateIdentityProviderServiceProviderCertificate.serviceProviderCertificate || null
            );
            setConfirmOpen(false);

            queryClient.invalidateQueries({queryKey: ['identityProviders']});
        },
    });

    return (
        <fieldset className="space-y-2 border-0 p-0">
            <label className="text-sm font-medium">SP Signing Certificate</label>

            {certificate ? (
                <Textarea
                    aria-label="SP signing certificate"
                    className="font-mono text-xs"
                    readOnly
                    rows={4}
                    value={certificate}
                />
            ) : (
                <p className="text-xs text-muted-foreground">Created on the first sign-in or SP metadata download.</p>
            )}

            <Button
                disabled={regenerateCertificateMutation.isPending}
                icon={<RefreshCwIcon className="size-4" />}
                label="Regenerate Certificate"
                onClick={() => setConfirmOpen(true)}
                variant="outline"
            />

            <p className="text-xs text-muted-foreground">
                ByteChef signs SAML requests with this certificate&apos;s key. Register it with identity providers that
                verify request signatures.
            </p>

            <AlertDialog
                confirmLabel="Regenerate"
                description="ByteChef will sign requests with a new key at once. Identity providers that verify request signatures reject sign-ins until you register the new certificate with them."
                isPending={regenerateCertificateMutation.isPending}
                onCancel={() => setConfirmOpen(false)}
                onConfirm={() => regenerateCertificateMutation.mutate({id: identityProviderId})}
                open={confirmOpen}
                title="Regenerate the SP signing certificate?"
            />
        </fieldset>
    );
};

export default IdentityProviderServiceProviderCertificateSection;
