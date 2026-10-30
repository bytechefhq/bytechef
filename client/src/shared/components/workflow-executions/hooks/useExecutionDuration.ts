import {useEffect, useState} from 'react';

const TICK_INTERVAL_MS = 1000;

interface UseExecutionDurationPropsI {
    endDate?: Date;
    running: boolean;
    startDate?: Date;
}

export const useExecutionDuration = ({endDate, running, startDate}: UseExecutionDurationPropsI) => {
    const [now, setNow] = useState(() => Date.now());

    const ticking = running && !!startDate && !endDate;

    useEffect(() => {
        if (!ticking) {
            return;
        }

        const interval = setInterval(() => setNow(Date.now()), TICK_INTERVAL_MS);

        return () => clearInterval(interval);
    }, [ticking]);

    if (!startDate) {
        return undefined;
    }

    if (endDate) {
        return Math.round(endDate.getTime() - startDate.getTime());
    }

    return ticking ? Math.max(Math.round(now - startDate.getTime()), 0) : undefined;
};

export const formatExecutionDuration = (duration: number | undefined) =>
    duration === undefined ? '-' : `${duration}ms`;
