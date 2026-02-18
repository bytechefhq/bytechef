import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import {Card, CardContent, CardHeader, CardTitle} from '@/components/ui/card';
import {Input} from '@/components/ui/input';
import PublicLayoutContainer from '@/shared/layout/PublicLayoutContainer';
import {useState} from 'react';

interface MfaVerificationProps {
    onBack: () => void;
    onVerify: (code: string) => Promise<boolean>;
}

const MfaVerification = ({onBack, onVerify}: MfaVerificationProps) => {
    const [mfaCode, setMfaCode] = useState('');
    const [mfaError, setMfaError] = useState(false);
    const [mfaSubmitting, setMfaSubmitting] = useState(false);

    const handleSubmit = async () => {
        setMfaSubmitting(true);
        setMfaError(false);

        const success = await onVerify(mfaCode);

        if (!success) {
            setMfaCode('');
            setMfaError(true);
        }

        setMfaSubmitting(false);
    };

    return (
        <PublicLayoutContainer>
            <Card className="mx-auto max-w-sm rounded-xl p-6 text-start shadow-none">
                <CardHeader className="p-0 pb-6">
                    <CardTitle className="self-center text-xl font-semibold text-content-neutral-primary">
                        Two-Factor Authentication
                    </CardTitle>
                </CardHeader>

                <CardContent className="flex flex-col gap-4 p-0">
                    <p className="text-sm text-content-neutral-secondary">
                        Enter the 6-digit code from your authenticator app.
                    </p>

                    {mfaError && <p className="text-sm text-destructive">Invalid code. Please try again.</p>}

                    <fieldset className="space-y-2 border-0 p-0">
                        <Input
                            aria-label="MFA verification code"
                            autoFocus
                            inputMode="numeric"
                            maxLength={6}
                            onChange={(event) => setMfaCode(event.target.value)}
                            onKeyDown={(event) => {
                                if (event.key === 'Enter' && mfaCode.length === 6) {
                                    handleSubmit();
                                }
                            }}
                            pattern="[0-9]*"
                            placeholder="Enter 6-digit code"
                            value={mfaCode}
                        />
                    </fieldset>

                    <Button
                        className="w-full"
                        disabled={mfaCode.length !== 6 || mfaSubmitting}
                        icon={
                            mfaSubmitting ? (
                                <div aria-label="loading icon">
                                    <LoadingIcon />
                                </div>
                            ) : undefined
                        }
                        label="Verify"
                        onClick={handleSubmit}
                        size="lg"
                    />

                    <Button className="w-full" label="Back to login" onClick={onBack} size="lg" variant="link" />
                </CardContent>
            </Card>
        </PublicLayoutContainer>
    );
};

export default MfaVerification;
