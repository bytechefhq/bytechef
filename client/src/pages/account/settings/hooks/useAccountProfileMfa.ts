import {useToast} from '@/hooks/use-toast';
import {getCookie} from '@/shared/util/cookie-utils';
import {useCallback, useEffect, useState} from 'react';

interface MfaSetupResponseI {
    qrCodeDataUrl: string;
    secret: string;
}

interface MfaStatusResponseI {
    totpEnabled: boolean;
}

export type MfaStateType = 'disabled' | 'enabled' | 'setup';

export function useAccountProfileMfa() {
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

    useEffect(() => {
        fetchMfaStatus();
    }, [fetchMfaStatus]);

    return {
        disableCode,
        disablePassword,
        handleCancelSetup,
        handleDisable,
        handleEnable,
        handleSetup,
        loading,
        mfaState,
        qrCodeDataUrl,
        secret,
        setDisableCode,
        setDisablePassword,
        setVerifyCode,
        verifyCode,
    };
}
