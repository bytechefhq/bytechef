import {Card, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import {useResendEmail} from '@/pages/account/public/hooks/useResendEmail';
import {usePasswordResetStore} from '@/pages/account/public/stores/usePasswordResetStore';
import PublicLayoutContainer from '@/shared/layout/PublicLayoutContainer';
import {MailCheck} from 'lucide-react';
import {useEffect} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';

const STORAGE_KEY_PREFIX = 'passwordReset_';

const PasswordResetEmailSent = () => {
    const {reset, resetPasswordFailure, resetPasswordInit} = usePasswordResetStore();

    const {countdown, disabled, startCountdown} = useResendEmail(STORAGE_KEY_PREFIX, 60);

    const location = useLocation();
    const navigate = useNavigate();

    useEffect(() => {
        if (resetPasswordFailure) {
            navigate('/account-error', {state: {fromInternalFlow: true}});
        }

        reset();
    }, [resetPasswordFailure, navigate, reset]);

    function handleResendEmail() {
        resetPasswordInit(location.state.email);

        startCountdown();
    }

    return (
        <PublicLayoutContainer>
            <Card className="mx-auto flex max-w-sm flex-col gap-6 rounded-xl p-6 text-start shadow-none">
                <MailCheck className="mx-auto size-12" />

                <CardHeader className="p-0">
                    <CardTitle className="self-center text-xl font-bold text-content-neutral-primary">
                        Please check your email
                    </CardTitle>

                    <CardDescription className="self-center text-content-neutral-secondary">
                        We sent an email to {location.state.email}. <br /> If you can&apos;t find it check the spam
                        folder.
                    </CardDescription>
                </CardHeader>

                <div className="flex justify-center gap-1 text-sm">
                    <span className="text-content-neutral-secondary">
                        {disabled ? `Mail sent. Wait ${countdown} sec to send again.` : `Didn't get an email?`}
                    </span>

                    {!disabled && (
                        <button
                            className="font-bold text-content-neutral-primary underline hover:text-content-neutral-secondary"
                            disabled={disabled}
                            onClick={handleResendEmail}
                        >
                            Click to resend
                        </button>
                    )}
                </div>
            </Card>
        </PublicLayoutContainer>
    );
};

export default PasswordResetEmailSent;
