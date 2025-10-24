import Button from '@/components/Button/Button';
import {Card, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import {useResendEmail} from '@/pages/account/public/hooks/useResendEmail';
import PublicLayoutContainer from '@/shared/layout/PublicLayoutContainer';
import {MailCheck} from 'lucide-react';
import {useLocation, useNavigate} from 'react-router-dom';

const STORAGE_KEY_PREFIX = 'verifyEmail_';

const VerifyEmail = () => {
    const {countdown, disabled, resendActivationEmail, startCountdown} = useResendEmail(STORAGE_KEY_PREFIX, 60);

    const location = useLocation();

    const navigate = useNavigate();

    function handleResendEmail() {
        resendActivationEmail(location.state.email).then(
            () => {
                startCountdown();
            },
            () => {
                navigate('/account-error', {state: {fromInternalFlow: true}});
            }
        );
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

                <div className="flex items-center justify-center text-sm">
                    <span className="text-content-neutral-secondary">
                        {disabled ? `Mail sent. Wait ${countdown} sec to send again.` : `Didn't get an email?`}
                    </span>

                    {!disabled && (
                        <Button
                            className="px-1"
                            disabled={disabled}
                            label="Click to resend"
                            onClick={handleResendEmail}
                            variant="link"
                        />
                    )}
                </div>
            </Card>
        </PublicLayoutContainer>
    );
};

export default VerifyEmail;
