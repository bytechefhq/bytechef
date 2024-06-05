import {Card, CardContent, CardHeader, CardTitle} from '@/components/ui/card';
import {useActivateStore} from '@/pages/account/public/stores/useActivateStore';
import PublicLayoutContainer from '@/shared/layout/PublicLayoutContainer';
import React, {useEffect} from 'react';
import {Link, useSearchParams} from 'react-router-dom';

const successAlert = (
    <div className="space-x-2">
        <span>Your user account has been activated.</span>

        <Link className="ml-auto inline-block text-sm underline" to="/login">
            Please sign in.
        </Link>
    </div>
);

const failureAlert = (
    <div className="space-y-2 text-destructive">
        <strong>Your user could not be activated.</strong>

        <div>Please use the registration form to sign up.</div>
    </div>
);

export const Activate = () => {
    const {activate, activationFailure, activationSuccess} = useActivateStore();

    const [searchParams] = useSearchParams();

    useEffect(() => {
        const key = searchParams.get('key');

        activate(key);

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return (
        <PublicLayoutContainer>
            <Card className="mx-auto max-w-lg shadow-none">
                <CardHeader>
                    <CardTitle className="text-xl">Activation</CardTitle>
                </CardHeader>

                <CardContent>
                    {activationSuccess ? successAlert : undefined}

                    {activationFailure ? failureAlert : undefined}
                </CardContent>
            </Card>
        </PublicLayoutContainer>
    );
};

export default Activate;
