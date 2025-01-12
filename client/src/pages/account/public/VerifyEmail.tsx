import {Card, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import {useRegisterStore} from '@/pages/account/public/stores/useRegisterStore';
import PublicLayoutContainer from '@/shared/layout/PublicLayoutContainer';
import {MailCheck} from 'lucide-react';
import {useEffect, useState} from 'react';
import {useLocation} from 'react-router-dom';

const VerifyEmail = () => {
    const [disabled, setDisabled] = useState(false);
    const [countdown, setCountdown] = useState(60);

    const {register} = useRegisterStore();

    const location = useLocation();

    useEffect(() => {
        let timer: ReturnType<typeof setInterval> | undefined;

        if (disabled) {
            timer = setInterval(() => {
                setCountdown((currentCountValue) => {
                    if (currentCountValue <= 1) {
                        clearInterval(timer);
                        setDisabled(false);
                        setCountdown(60);

                        return 60;
                    }

                    return currentCountValue - 1;
                });
            }, 1000);
        }

        return () => clearInterval(timer);
    }, [disabled]);

    function handleResendEmail() {
        register(location.state.email, location.state.password);

        setDisabled(true);
    }

    return (
        <PublicLayoutContainer>
            <Card className="mx-auto flex max-w-sm flex-col gap-6 rounded-xl p-6 text-start shadow-none">
                <MailCheck className="mx-auto size-12" />

                <CardHeader className="p-0">
                    <CardTitle className="self-center text-xl font-bold text-content-neutral-primary">
                        Verify your email address
                    </CardTitle>

                    <CardDescription className="self-center text-content-neutral-secondary">
                        We sent an email to {location.state.email}.
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

export default VerifyEmail;
