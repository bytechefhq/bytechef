import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
    AlertDialogTrigger,
} from '@/components/ui/alert-dialog';
import {Input} from '@/components/ui/input';
import {useToast} from '@/hooks/use-toast';
import {getCookie} from '@/shared/util/cookie-utils';
import {ShieldCheckIcon, ShieldOffIcon} from 'lucide-react';
import {useCallback, useEffect, useState} from 'react';

interface MfaSetupResponseI {
    qrCodeDataUrl: string;
    secret: string;
}

interface MfaStatusResponseI {
    totpEnabled: boolean;
}

type MfaStateType = 'disabled' | 'enabled' | 'setup';

const AccountProfileMfa = () => {
    const [disableCode, setDisableCode] = useState('');
    const [disablePassword, setDisablePassword] = useState('');
    const [loading, setLoading] = useState(false);
    const [mfaState, setMfaState] = useState<MfaStateType>('disabled');
    const [qrCodeDataUrl, setQrCodeDataUrl] = useState('');
    const [secret, setSecret] = useState('');
    const [verifyCode, setVerifyCode] = useState('');

    const {toast} = useToast();

    const fetchMfaStatus = useCallback(async () => {
        try {
            const response = await fetch('/api/account/mfa/status');

            if (response.ok) {
                const data: MfaStatusResponseI = await response.json();

                setMfaState(data.totpEnabled ? 'enabled' : 'disabled');
            } else {
                toast({description: 'Failed to load MFA status.', variant: 'destructive'});
            }
        } catch {
            toast({description: 'Failed to load MFA status.', variant: 'destructive'});
        }
    }, [toast]);

    useEffect(() => {
        fetchMfaStatus();
    }, [fetchMfaStatus]);

    const handleSetup = async () => {
        setLoading(true);

        try {
            const response = await fetch('/api/account/mfa/setup', {
                headers: {'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || ''},
                method: 'POST',
            });

            if (response.ok) {
                const data: MfaSetupResponseI = await response.json();

                setQrCodeDataUrl(data.qrCodeDataUrl);
                setSecret(data.secret);
                setMfaState('setup');
            } else {
                toast({description: 'Failed to start MFA setup.', variant: 'destructive'});
            }
        } catch {
            toast({description: 'Failed to start MFA setup.', variant: 'destructive'});
        } finally {
            setLoading(false);
        }
    };

    const handleEnable = async () => {
        setLoading(true);

        try {
            const response = await fetch('/api/account/mfa/enable', {
                body: JSON.stringify({code: verifyCode}),
                headers: {
                    'Content-Type': 'application/json',
                    'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
                },
                method: 'POST',
            });

            if (response.ok) {
                toast({description: 'Two-factor authentication has been enabled.'});

                setVerifyCode('');
                setQrCodeDataUrl('');
                setSecret('');
                setMfaState('enabled');
            } else {
                toast({description: 'Invalid verification code. Please try again.', variant: 'destructive'});
            }
        } catch {
            toast({description: 'Failed to enable two-factor authentication.', variant: 'destructive'});
        } finally {
            setLoading(false);
        }
    };

    const handleDisable = async () => {
        setLoading(true);

        try {
            const response = await fetch('/api/account/mfa/disable', {
                body: JSON.stringify({code: disableCode, password: disablePassword}),
                headers: {
                    'Content-Type': 'application/json',
                    'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
                },
                method: 'POST',
            });

            if (response.ok) {
                toast({description: 'Two-factor authentication has been disabled.'});

                setDisableCode('');
                setDisablePassword('');
                setMfaState('disabled');
            } else {
                toast({description: 'Failed to disable 2FA. Check your password and code.', variant: 'destructive'});
            }
        } catch {
            toast({description: 'Failed to disable two-factor authentication.', variant: 'destructive'});
        } finally {
            setLoading(false);
        }
    };

    const handleCancelSetup = () => {
        setMfaState('disabled');
        setVerifyCode('');
        setQrCodeDataUrl('');
        setSecret('');
    };

    return (
        <div className="py-12">
            <h2 className="text-base font-semibold leading-7 text-gray-900">Two-Factor Authentication</h2>

            <p className="mt-1 text-sm leading-6 text-gray-500">
                Add an extra layer of security to your account using an authenticator app.
            </p>

            <div className="mt-6">
                {mfaState === 'disabled' && (
                    <Button
                        disabled={loading}
                        icon={loading ? <LoadingIcon /> : undefined}
                        label="Set up 2FA"
                        onClick={handleSetup}
                        variant="outline"
                    />
                )}

                {mfaState === 'setup' && (
                    <div className="space-y-4">
                        <p className="text-sm text-gray-700">
                            Scan this QR code with your authenticator app (Google Authenticator, Authy, etc.):
                        </p>

                        {qrCodeDataUrl && (
                            <div className="flex justify-center rounded-lg border p-4">
                                <img alt="TOTP QR Code" className="size-48" src={qrCodeDataUrl} />
                            </div>
                        )}

                        <div className="space-y-2">
                            <p className="text-xs text-muted-foreground">
                                Or enter this secret manually in your authenticator app:
                            </p>

                            <code className="block break-all rounded bg-muted px-3 py-2 font-mono text-sm">
                                {secret}
                            </code>
                        </div>

                        <fieldset className="space-y-2 border-0 p-0">
                            <label className="text-sm font-medium" htmlFor="verifyCode">
                                Verification Code
                            </label>

                            <Input
                                autoFocus
                                id="verifyCode"
                                inputMode="numeric"
                                maxLength={6}
                                onChange={(event) => setVerifyCode(event.target.value)}
                                pattern="[0-9]*"
                                placeholder="Enter 6-digit code"
                                value={verifyCode}
                            />
                        </fieldset>

                        <div className="flex gap-2">
                            <Button
                                disabled={verifyCode.length !== 6 || loading}
                                icon={loading ? <LoadingIcon /> : undefined}
                                label="Verify & Enable"
                                onClick={handleEnable}
                            />

                            <Button disabled={loading} label="Cancel" onClick={handleCancelSetup} variant="outline" />
                        </div>
                    </div>
                )}

                {mfaState === 'enabled' && (
                    <div className="space-y-4">
                        <div className="flex items-center gap-2 rounded-lg border border-green-200 bg-green-50 p-3">
                            <ShieldCheckIcon className="size-4 text-green-600" />

                            <p className="text-sm text-green-700">Two-factor authentication is enabled.</p>
                        </div>

                        <AlertDialog>
                            <AlertDialogTrigger asChild>
                                <Button
                                    icon={<ShieldOffIcon className="size-4" />}
                                    label="Disable 2FA"
                                    variant="outline"
                                />
                            </AlertDialogTrigger>

                            <AlertDialogContent>
                                <AlertDialogHeader>
                                    <AlertDialogTitle>Disable Two-Factor Authentication</AlertDialogTitle>

                                    <AlertDialogDescription>
                                        Enter your current password and a TOTP code to disable two-factor
                                        authentication.
                                    </AlertDialogDescription>
                                </AlertDialogHeader>

                                <div className="space-y-3 py-2">
                                    <fieldset className="space-y-2 border-0 p-0">
                                        <label className="text-sm font-medium" htmlFor="disablePassword">
                                            Password
                                        </label>

                                        <Input
                                            id="disablePassword"
                                            onChange={(event) => setDisablePassword(event.target.value)}
                                            placeholder="Enter your password"
                                            type="password"
                                            value={disablePassword}
                                        />
                                    </fieldset>

                                    <fieldset className="space-y-2 border-0 p-0">
                                        <label className="text-sm font-medium" htmlFor="disableCode">
                                            Authentication Code
                                        </label>

                                        <Input
                                            id="disableCode"
                                            inputMode="numeric"
                                            maxLength={6}
                                            onChange={(event) => setDisableCode(event.target.value)}
                                            pattern="[0-9]*"
                                            placeholder="Enter 6-digit code"
                                            value={disableCode}
                                        />
                                    </fieldset>
                                </div>

                                <AlertDialogFooter>
                                    <AlertDialogCancel
                                        onClick={() => {
                                            setDisableCode('');
                                            setDisablePassword('');
                                        }}
                                    >
                                        Cancel
                                    </AlertDialogCancel>

                                    <AlertDialogAction
                                        disabled={disableCode.length !== 6 || !disablePassword || loading}
                                        onClick={handleDisable}
                                    >
                                        Disable 2FA
                                    </AlertDialogAction>
                                </AlertDialogFooter>
                            </AlertDialogContent>
                        </AlertDialog>
                    </div>
                )}
            </div>
        </div>
    );
};

export default AccountProfileMfa;
