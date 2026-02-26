import {getCookie} from '@/shared/util/cookie-utils';
import {useCallback, useEffect, useState} from 'react';
import {toast} from 'sonner';

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

    const fetchMfaStatus = useCallback(async () => {
        try {
            const response = await fetch('/api/account/mfa/status');

            if (response.ok) {
                const data: MfaStatusResponseI = await response.json();

                setMfaState(data.totpEnabled ? 'enabled' : 'disabled');
            } else {
                toast.error('Failed to load MFA status.');
            }
        } catch {
            toast.error('Failed to load MFA status.');
        }
    }, []);

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
                toast.error('Failed to start MFA setup.');
            }
        } catch {
            toast.error('Failed to start MFA setup.');
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
                toast('Two-factor authentication has been enabled.');

                setVerifyCode('');
                setQrCodeDataUrl('');
                setSecret('');
                setMfaState('enabled');
            } else {
                toast.error('Invalid verification code. Please try again.');
            }
        } catch {
            toast.error('Failed to enable two-factor authentication.');
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
                toast('Two-factor authentication has been disabled.');

                setDisableCode('');
                setDisablePassword('');
                setMfaState('disabled');
            } else {
                toast.error('Failed to disable 2FA. Check your password and code.');
            }
        } catch {
            toast.error('Failed to disable two-factor authentication.');
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
