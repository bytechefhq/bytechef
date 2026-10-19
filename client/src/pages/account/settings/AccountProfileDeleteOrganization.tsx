import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
} from '@/components/Dialog';
import {Input} from '@/components/Input/Input';
import LoadingIcon from '@/components/LoadingIcon';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useIsTenantAdmin} from '@/shared/hooks/useIsTenantAdmin';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {getCookie} from '@/shared/util/cookie-utils';
import {useQueryClient} from '@tanstack/react-query';
import {Trash2Icon} from 'lucide-react';
import {useEffect, useState} from 'react';
import {toast} from 'sonner';
import {useShallow} from 'zustand/react/shallow';

const DeleteOrganizationSection = ({deletionGracePeriodDays}: {deletionGracePeriodDays: number}) => {
    const [confirmation, setConfirmation] = useState('');
    const [isPending, setIsPending] = useState(false);
    const [password, setPassword] = useState('');
    const [passwordRequired, setPasswordRequired] = useState(true);
    const [showDialog, setShowDialog] = useState(false);

    const {account, clearAuthentication, logout} = useAuthenticationStore(
        useShallow((state) => ({
            account: state.account,
            clearAuthentication: state.clearAuthentication,
            logout: state.logout,
        }))
    );

    const analytics = useAnalytics();

    const queryClient = useQueryClient();

    const email = account?.email || account?.login || '';

    const confirmed = confirmation.trim().toLowerCase() === email.toLowerCase();

    const handleOpenChange = (open: boolean) => {
        setShowDialog(open);

        if (!open) {
            setConfirmation('');
            setPassword('');
        }
    };

    const handleDelete = async () => {
        setIsPending(true);

        try {
            const response = await fetch('/api/account/organization/delete', {
                body: JSON.stringify({confirmation, password: passwordRequired ? password : null}),
                headers: {
                    'Content-Type': 'application/json',
                    'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
                },
                method: 'POST',
            });

            if (!response.ok) {
                return;
            }

            const {purgeAfter} = (await response.json()) as {purgeAfter: string};

            toast(`Your organization will be deleted on ${new Date(purgeAfter).toLocaleDateString()}.`);

            analytics.reset();

            await queryClient.cancelQueries();

            await logout();

            clearAuthentication();

            queryClient.clear();
        } finally {
            setIsPending(false);
        }
    };

    useEffect(() => {
        fetch('/api/account/linked-accounts')
            .then((response) => (response.ok ? response.json() : undefined))
            .then((linkedAccount: {authProvider?: string} | undefined) => {
                if (linkedAccount?.authProvider) {
                    setPasswordRequired(linkedAccount.authProvider === 'LOCAL');
                }
            })
            .catch(() => undefined);
    }, []);

    return (
        <div className="py-12">
            <h2 className="text-base leading-7 font-semibold text-content-destructive-primary">Delete organization</h2>

            <p className="mt-1 text-sm leading-6 text-content-neutral-secondary">
                Permanently delete this organization and everything in it: workflows, connections, executions, files and
                members. Billing stops immediately. After {deletionGracePeriodDays} days the data is deleted for good.
            </p>

            <div className="mt-6">
                <Button
                    icon={<Trash2Icon className="size-4" />}
                    label="Delete organization"
                    onClick={() => setShowDialog(true)}
                    variant="destructive"
                />
            </div>

            <Dialog onOpenChange={handleOpenChange} open={showDialog}>
                <DialogContent>
                    <DialogMain>
                        <DialogHeader
                            description="This permanently deletes the organization and everything in it, and signs you out."
                            title="Delete Organization"
                        />

                        <DialogBody className="space-y-3">
                            <fieldset className="space-y-2 border-0 p-0">
                                <label className="text-sm font-medium" htmlFor="deleteOrganizationConfirmation">
                                    Type <span className="font-semibold">{email}</span> to confirm
                                </label>

                                <Input
                                    autoComplete="off"
                                    id="deleteOrganizationConfirmation"
                                    onChange={(event) => setConfirmation(event.target.value)}
                                    value={confirmation}
                                />
                            </fieldset>

                            {passwordRequired && (
                                <fieldset className="space-y-2 border-0 p-0">
                                    <label className="text-sm font-medium" htmlFor="deleteOrganizationPassword">
                                        Password
                                    </label>

                                    <Input
                                        autoComplete="current-password"
                                        id="deleteOrganizationPassword"
                                        onChange={(event) => setPassword(event.target.value)}
                                        placeholder="Enter your password"
                                        type="password"
                                        value={password}
                                    />
                                </fieldset>
                            )}
                        </DialogBody>

                        <DialogFooter>
                            <DialogCancelButton />

                            <Button
                                disabled={!confirmed || (passwordRequired && !password) || isPending}
                                icon={isPending ? <LoadingIcon /> : undefined}
                                label="Delete organization"
                                onClick={handleDelete}
                                variant="destructive"
                            />
                        </DialogFooter>
                    </DialogMain>
                </DialogContent>
            </Dialog>
        </div>
    );
};

const AccountProfileDeleteOrganization = () => {
    const tenant = useApplicationInfoStore((state) => state.tenant);

    const isTenantAdmin = useIsTenantAdmin();

    if (!isTenantAdmin || tenant.mode !== 'multi') {
        return null;
    }

    return <DeleteOrganizationSection deletionGracePeriodDays={tenant.deletionGracePeriodDays} />;
};

export default AccountProfileDeleteOrganization;
