import {useToast} from '@/components/ui/use-toast';
import AccountProfileDetailsForm, {formSchema} from '@/pages/account/settings/AccountProfileDetailsForm';
import {useAccountStore} from '@/pages/account/settings/stores/useAccountStore';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import React, {useEffect} from 'react';
import {z} from 'zod';

const AccountProfileDetails = () => {
    const {reset, updateAccount, updateSuccess} = useAccountStore();
    const {account, getAccount} = useAuthenticationStore();

    const {toast} = useToast();

    const handleSubmit = ({email, firstName, lastName}: z.infer<typeof formSchema>) => {
        updateAccount({
            ...account,
            email,
            firstName,
            lastName,
        });
    };

    useEffect(() => {
        getAccount();

        return () => {
            reset();
        };

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useEffect(() => {
        if (updateSuccess) {
            toast({description: 'Account has been updated.'});

            getAccount();
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [updateSuccess]);

    return (
        <div className="pb-12">
            <h2 className="text-base font-semibold leading-7 text-gray-900">Profile</h2>

            {account && <AccountProfileDetailsForm account={account} handleSubmit={handleSubmit} />}
        </div>
    );
};

export default AccountProfileDetails;
