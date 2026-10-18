import AccountProfileDetails from '@/pages/account/settings/AccountProfileDetails';
import AccountProfileLinkedAccounts from '@/pages/account/settings/AccountProfileLinkedAccounts';
import AccountProfileMfa from '@/pages/account/settings/AccountProfileMfa';
import AccountProfilePassword from '@/pages/account/settings/AccountProfilePassword';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import React from 'react';

const AccountProfile = () => {
    const socialLoginEnabled = useApplicationInfoStore((state) => state.socialLogin.enabled);

    const isFeatureFlagEnabled = useFeatureFlagsStore();

    const identityProvidersEnabled = isFeatureFlagEnabled('ff-1040');

    return (
        <LayoutContainer
            header={<Header centerTitle={true} position="main" title="Your profile" />}
            leftSidebarOpen={false}
        >
            <div className="w-full self-start p-4 pt-0 3xl:mx-auto 3xl:w-4/5">
                <div className="max-w-xl divide-y divide-muted">
                    <AccountProfileDetails />

                    <AccountProfilePassword />

                    {identityProvidersEnabled && <AccountProfileMfa />}

                    {socialLoginEnabled && <AccountProfileLinkedAccounts />}
                </div>
            </div>
        </LayoutContainer>
    );
};

export default AccountProfile;
