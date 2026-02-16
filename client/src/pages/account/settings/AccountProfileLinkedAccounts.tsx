import Badge from '@/components/Badge/Badge';
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
    AlertDialogTrigger,
} from '@/components/ui/alert-dialog';
import {useToast} from '@/hooks/use-toast';
import {LinkIcon, UnlinkIcon} from 'lucide-react';
import {useCallback, useEffect, useState} from 'react';

interface LinkedAccountI {
    authProvider: string;
    hasPassword: boolean;
    providerId: string | null;
}

const AccountProfileLinkedAccounts = () => {
    const [linkedAccount, setLinkedAccount] = useState<LinkedAccountI | null>(null);
    const {toast} = useToast();

    const fetchLinkedAccounts = useCallback(async () => {
        try {
            const response = await fetch('/api/account/linked-accounts');

            if (response.ok) {
                const data = await response.json();

                setLinkedAccount(data);
            } else {
                toast({description: 'Failed to fetch linked accounts.', variant: 'destructive'});
            }
        } catch {
            toast({description: 'Failed to fetch linked accounts.', variant: 'destructive'});
        }
    }, [toast]);

    useEffect(() => {
        fetchLinkedAccounts();
    }, [fetchLinkedAccounts]);

    const handleUnlink = async (provider: string) => {
        const response = await fetch(`/api/account/linked-accounts/${provider}`, {
            method: 'DELETE',
        });

        if (response.ok) {
            toast({description: 'Provider has been unlinked.'});

            fetchLinkedAccounts();
        } else {
            toast({description: 'Failed to unlink provider.', variant: 'destructive'});
        }
    };

    const isLinked = linkedAccount !== null && linkedAccount.authProvider !== 'LOCAL';

    return (
        <div className="py-12">
            <h2 className="text-base font-semibold leading-7 text-gray-900">Linked Accounts</h2>

            <p className="mt-1 text-sm leading-6 text-gray-500">
                Manage external identity providers linked to your account.
            </p>

            <div className="mt-6 space-y-4">
                {linkedAccount && (
                    <div className="flex items-center justify-between rounded-lg border p-4">
                        <div className="flex items-center gap-3">
                            <LinkIcon className="size-5 text-muted-foreground" />

                            <div>
                                <p className="text-sm font-medium">
                                    {isLinked ? linkedAccount.authProvider : 'No provider linked'}
                                </p>

                                {isLinked && linkedAccount.providerId && (
                                    <p className="text-xs text-muted-foreground">{linkedAccount.providerId}</p>
                                )}
                            </div>

                            {isLinked && <Badge label={linkedAccount.authProvider} styleType="secondary-filled" />}
                        </div>

                        {isLinked && (
                            <AlertDialog>
                                <AlertDialogTrigger asChild>
                                    <Button disabled={!linkedAccount.hasPassword} label="Unlink" variant="outline" />
                                </AlertDialogTrigger>

                                <AlertDialogContent>
                                    <AlertDialogHeader>
                                        <AlertDialogTitle>Unlink Provider</AlertDialogTitle>

                                        <AlertDialogDescription>
                                            Are you sure you want to unlink {linkedAccount.authProvider}? You will need
                                            to use your password to log in.
                                        </AlertDialogDescription>
                                    </AlertDialogHeader>

                                    <AlertDialogFooter>
                                        <AlertDialogCancel>Cancel</AlertDialogCancel>

                                        <AlertDialogAction onClick={() => handleUnlink(linkedAccount.authProvider)}>
                                            Unlink
                                        </AlertDialogAction>
                                    </AlertDialogFooter>
                                </AlertDialogContent>
                            </AlertDialog>
                        )}

                        {!isLinked && (
                            <div className="flex gap-2">
                                <Button
                                    label="Link Google"
                                    onClick={() => {
                                        window.location.href = '/oauth2/authorization/google';
                                    }}
                                    variant="outline"
                                />

                                <Button
                                    label="Link GitHub"
                                    onClick={() => {
                                        window.location.href = '/oauth2/authorization/github';
                                    }}
                                    variant="outline"
                                />
                            </div>
                        )}
                    </div>
                )}

                {isLinked && !linkedAccount.hasPassword && (
                    <div className="flex items-center gap-2 rounded-lg border border-amber-200 bg-amber-50 p-3">
                        <UnlinkIcon className="size-4 text-amber-600" />

                        <p className="text-sm text-amber-700">
                            Set a password before unlinking your provider to maintain account access.
                        </p>
                    </div>
                )}
            </div>
        </div>
    );
};

export default AccountProfileLinkedAccounts;
