import {useCallback, useEffect, useState} from 'react';

export function useResendEmail(storagePrefix: string, countdownSeconds: number = 60) {
    const [disabled, setDisabled] = useState(false);
    const [countdown, setCountdown] = useState(countdownSeconds);

    const clearLocalStorage = useCallback(() => {
        localStorage.removeItem(`${storagePrefix}resendDisabled`);

        localStorage.removeItem(`${storagePrefix}resendExpiry`);
    }, [storagePrefix]);

    useEffect(() => {
        const isResendDisabled = localStorage.getItem(`${storagePrefix}resendDisabled`);
        const resendExpiry = localStorage.getItem(`${storagePrefix}resendExpiry`);

        if (isResendDisabled === 'true' && resendExpiry) {
            const remainingTime = Math.floor((Number(resendExpiry) - Date.now()) / 1000);

            if (remainingTime > 0) {
                setDisabled(true);

                setCountdown(remainingTime);
            } else {
                clearLocalStorage();
            }
        }
    }, [clearLocalStorage, storagePrefix]);

    useEffect(() => {
        let timer: ReturnType<typeof setInterval> | undefined;

        if (disabled) {
            timer = setInterval(() => {
                setCountdown((currentCountValue) => {
                    if (currentCountValue <= 1) {
                        clearInterval(timer);

                        setDisabled(false);

                        setCountdown(countdownSeconds);

                        clearLocalStorage();

                        return countdownSeconds;
                    }

                    return currentCountValue - 1;
                });
            }, 1000);
        }

        return () => clearInterval(timer);
    }, [disabled, countdownSeconds, clearLocalStorage]);

    const startCountdown = useCallback(() => {
        setDisabled(true);

        localStorage.setItem(`${storagePrefix}resendDisabled`, 'true');
        localStorage.setItem(`${storagePrefix}resendExpiry`, (Date.now() + countdownSeconds * 1000).toString());
    }, [storagePrefix, countdownSeconds]);

    return {
        countdown,
        disabled,
        startCountdown,
    };
}
