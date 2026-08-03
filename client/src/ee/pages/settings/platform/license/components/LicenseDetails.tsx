import Button from '@/components/Button/Button';
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import {Badge} from '@/components/ui/badge';
import LicenseUpload from '@/ee/pages/settings/platform/license/components/LicenseUpload';
import {LicenceQuery, useDeleteLicenceMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {RefreshCwIcon, Trash2Icon} from 'lucide-react';
import {useMemo, useState} from 'react';

type LicenceType = NonNullable<LicenceQuery['licence']>;

interface LicenseDetailsProps {
    licence: LicenceType;
}

type StatusInfoType = {
    label: string;
    variant: 'default' | 'destructive' | 'outline' | 'secondary';
};

const STATUS_MAP: Record<string, StatusInfoType> = {
    EXPIRED: {label: 'Expired', variant: 'destructive'},
    GRACE: {label: 'In grace', variant: 'secondary'},
    INVALID: {label: 'Invalid', variant: 'destructive'},
    VALID: {label: 'Valid', variant: 'default'},
};

const LicenseDetails = ({licence}: LicenseDetailsProps) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showReplaceForm, setShowReplaceForm] = useState(false);

    const queryClient = useQueryClient();

    const {mutate: deleteLicence} = useDeleteLicenceMutation();

    const statusInfo = useMemo<StatusInfoType>(
        () => STATUS_MAP[licence.status] ?? {label: licence.status, variant: 'outline'},
        [licence.status]
    );

    const formattedIssuedAt = useMemo(
        () => (licence.issuedAt ? new Date(licence.issuedAt).toLocaleDateString() : '—'),
        [licence.issuedAt]
    );

    const formattedExpiresAt = useMemo(
        () => (licence.expiresAt ? new Date(licence.expiresAt).toLocaleDateString() : '—'),
        [licence.expiresAt]
    );

    const handleDelete = () => {
        deleteLicence(
            {},
            {
                onSuccess: () => {
                    queryClient.invalidateQueries({queryKey: ['licence']});
                    setShowDeleteDialog(false);
                },
            }
        );
    };

    if (showReplaceForm) {
        return (
            <div className="space-y-4">
                <div className="flex items-center justify-between">
                    <h3 className="text-sm font-medium">Replace License</h3>

                    <Button onClick={() => setShowReplaceForm(false)} size="sm" variant="ghost">
                        Cancel
                    </Button>
                </div>

                <LicenseUpload />
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div className="rounded-lg border p-6">
                <div className="mb-4 flex items-center justify-between">
                    <h3 className="text-base font-semibold">License Details</h3>

                    <Badge variant={statusInfo.variant}>{statusInfo.label}</Badge>
                </div>

                <dl className="grid grid-cols-2 gap-x-6 gap-y-4 text-sm">
                    <div>
                        <dt className="font-medium text-content-neutral-secondary">Holder Name</dt>

                        <dd className="mt-1">{licence.holderName || '—'}</dd>
                    </div>

                    <div>
                        <dt className="font-medium text-content-neutral-secondary">Holder Email</dt>

                        <dd className="mt-1">{licence.holderEmail || '—'}</dd>
                    </div>

                    <div>
                        <dt className="font-medium text-content-neutral-secondary">Issued At</dt>

                        <dd className="mt-1">{formattedIssuedAt}</dd>
                    </div>

                    <div>
                        <dt className="font-medium text-content-neutral-secondary">Expires At</dt>

                        <dd className="mt-1">{formattedExpiresAt}</dd>
                    </div>

                    <div>
                        <dt className="font-medium text-content-neutral-secondary">Allowed Jobs</dt>

                        <dd className="mt-1">
                            {licence.allowedJobs == null ? 'Unlimited' : String(licence.allowedJobs)}
                        </dd>
                    </div>

                    <div>
                        <dt className="font-medium text-content-neutral-secondary">Jobs Used This Month</dt>

                        <dd className="mt-1">{String(licence.currentMonthJobUsage || 0)}</dd>
                    </div>

                    <div>
                        <dt className="font-medium text-content-neutral-secondary">Max Users</dt>

                        <dd className="mt-1">{licence.maxUsers == null ? 'Unlimited' : String(licence.maxUsers)}</dd>
                    </div>
                </dl>

                {licence.features.length > 0 && (
                    <div className="mt-4">
                        <p className="mb-2 text-sm font-medium text-content-neutral-secondary">Features</p>

                        <div className="flex flex-wrap gap-2">
                            {licence.features.map((feature) => (
                                <Badge className="cursor-default" key={feature} variant="outline">
                                    {feature}
                                </Badge>
                            ))}
                        </div>
                    </div>
                )}
            </div>

            <div className="flex gap-2">
                <Button onClick={() => setShowReplaceForm(true)} variant="outline">
                    <RefreshCwIcon className="mr-2 size-4" />
                    Replace License
                </Button>

                <Button onClick={() => setShowDeleteDialog(true)} variant="destructive">
                    <Trash2Icon className="mr-2 size-4" />
                    Remove License
                </Button>
            </div>

            <AlertDialog onOpenChange={setShowDeleteDialog} open={showDeleteDialog}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Remove License</AlertDialogTitle>

                        <AlertDialogDescription>
                            Are you sure you want to remove the current license? The platform will revert to the
                            Community Edition.
                        </AlertDialogDescription>
                    </AlertDialogHeader>

                    <AlertDialogFooter>
                        <AlertDialogCancel onClick={() => setShowDeleteDialog(false)}>Cancel</AlertDialogCancel>

                        <AlertDialogAction className="bg-red-600" onClick={handleDelete}>
                            Remove
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </div>
    );
};

export default LicenseDetails;
