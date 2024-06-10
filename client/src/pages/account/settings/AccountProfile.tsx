import AccountProfileDetails from '@/pages/account/settings/AccountProfileDetails';
import AccountProfilePassword from '@/pages/account/settings/AccountProfilePassword';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import React from 'react';

const AccountProfile = () => {
    return (
        <LayoutContainer
            header={<Header centerTitle={true} position="main" title="Your profile" />}
            leftSidebarOpen={false}
        >
            <div className="w-full p-4 2xl:mx-auto 2xl:w-4/5">
                <div className="max-w-xl divide-y divide-muted">
                    <AccountProfileDetails />

                    <AccountProfilePassword />
                </div>
            </div>
        </LayoutContainer>
    );
};

export default AccountProfile;
