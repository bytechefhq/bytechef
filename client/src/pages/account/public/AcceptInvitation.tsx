import Button from '@/components/Button/Button';
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import NewPasswordForm from '@/pages/account/public/components/NewPasswordForm';
import PublicLayoutContainer from '@/shared/layout/PublicLayoutContainer';
import {getXsrfToken} from '@/shared/util/cookie-utils';
import {CircleCheckBigIcon} from 'lucide-react';
import {useState} from 'react';
import {Link, useSearchParams} from 'react-router-dom';

type AcceptInvitationStatusType = 'failure' | 'idle' | 'success';

const AcceptInvitation = () => {
    const [status, setStatus] = useState<AcceptInvitationStatusType>('idle');

    const [searchParams] = useSearchParams();
    const key = searchParams.get('key');

    async function handleSubmit(newPassword: string) {
        const response = await fetch('/api/account/accept-invitation', {
            body: JSON.stringify({key, newPassword}),
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': await getXsrfToken(),
            },
            method: 'POST',
        });

        setStatus(response.ok ? 'success' : 'failure');
    }

    if (status === 'success') {
        return (
            <PublicLayoutContainer>
                <Card className="mx-auto flex max-w-sm flex-col gap-6 rounded-xl p-6 text-center shadow-none">
                    <CircleCheckBigIcon className="mx-auto size-12 text-success" />

                    <CardHeader className="p-0">
                        <CardTitle className="self-center text-xl font-bold text-content-neutral-primary">
                            Your account is ready
                        </CardTitle>

                        <CardDescription className="self-center text-center text-content-neutral-secondary">
                            Your password is set and your account is active. <br /> Click below to log in.
                        </CardDescription>
                    </CardHeader>

                    <Link to="/login">
                        <Button className="w-fit" label="Continue to Log in" size="lg" />
                    </Link>
                </Card>
            </PublicLayoutContainer>
        );
    }

    return (
        <PublicLayoutContainer>
            <Card className="mx-auto max-w-sm rounded-xl p-6 text-start shadow-none">
                <CardHeader className="p-0 pb-8 text-center">
                    <CardTitle className="text-xl font-bold text-content-neutral-primary">Set your password</CardTitle>

                    <CardDescription className="text-content-neutral-secondary">
                        Choose a password to activate your account.
                    </CardDescription>
                </CardHeader>

                <CardContent className="flex flex-col gap-6 p-0">
                    {status === 'failure' && (
                        <p className="text-sm text-destructive" role="alert">
                            This invitation link is invalid or has expired. Ask your administrator to invite you again.
                        </p>
                    )}

                    {key && <NewPasswordForm onSubmit={handleSubmit} submitLabel="Activate account" />}
                </CardContent>
            </Card>
        </PublicLayoutContainer>
    );
};

export default AcceptInvitation;
