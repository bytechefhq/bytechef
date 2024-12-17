import {Card, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import {usePasswordResetStore} from '@/pages/account/public/stores/usePasswordResetStore';
import PublicLayoutContainer from '@/shared/layout/PublicLayoutContainer';
import {MailCheck} from 'lucide-react';
import {useEffect, useState} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';

const PasswordResetEmailSent = () => {
    const {reset, resetPasswordFailure, resetPasswordInit} = usePasswordResetStore();

    const [disabled, setDisabled] = useState(false);

    const [countdown, setCountdown] = useState(60);

    const location = useLocation();

    const navigate = useNavigate();

    useEffect(() => {
        let timer: ReturnType<typeof setInterval> | undefined;

        if (disabled) {
            timer = setInterval(() => {
                setCountdown((prev) => {
                    if (prev <= 1) {
                        clearInterval(timer);
                        setDisabled(false);
                        setCountdown(60);
                        return 60;
                    }
                    return prev - 1;
                });
            }, 1000);
        }

        return () => clearInterval(timer);
    }, [disabled]);

    function handleResendEmail() {
        resetPasswordInit(location.state.email);

        setDisabled(true);
    }

    useEffect(() => {
        if (resetPasswordFailure) {
            navigate('/account-error', {state: {error: 'Something went wrong. Try again.'}});
        }

        reset();
    }, [resetPasswordFailure, navigate, reset]);

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
                        folder
                    </CardDescription>
                </CardHeader>

                <div className="flex justify-center gap-1 text-sm">
                    <span className="text-content-neutral-secondary">
                        {disabled ? `Mail sent. Wait ${countdown} sec to send again.` : `Didn't get an email?`}
                    </span>

                    <button
                        className="font-bold text-content-neutral-primary underline hover:text-content-neutral-secondary"
                        disabled={disabled}
                        onClick={handleResendEmail}
                    >
                        {!disabled && 'Click to resend'}
                    </button>
                </div>
            </Card>
        </PublicLayoutContainer>
    );
};

export default PasswordResetEmailSent;
