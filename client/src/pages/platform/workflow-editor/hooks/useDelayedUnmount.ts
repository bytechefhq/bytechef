import {useEffect, useState} from 'react';

export default function useDelayedUnmount(open: boolean, durationMs = 300): {mounted: boolean; visible: boolean} {
    const [mounted, setMounted] = useState(open);
    const [visible, setVisible] = useState(open);

    useEffect(() => {
        let outerRafId: number | undefined;
        let innerRafId: number | undefined;
        let timerId: ReturnType<typeof setTimeout> | undefined;

        if (open) {
            setMounted(true);

            outerRafId = requestAnimationFrame(() => {
                innerRafId = requestAnimationFrame(() => {
                    setVisible(true);
                });
            });
        } else {
            setVisible(false);

            timerId = setTimeout(() => setMounted(false), durationMs);
        }

        return () => {
            if (outerRafId !== undefined) {
                cancelAnimationFrame(outerRafId);
            }

            if (innerRafId !== undefined) {
                cancelAnimationFrame(innerRafId);
            }

            if (timerId !== undefined) {
                clearTimeout(timerId);
            }
        };
    }, [durationMs, open]);

    return {mounted, visible};
}
