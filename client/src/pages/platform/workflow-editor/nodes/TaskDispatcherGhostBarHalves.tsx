import {twMerge} from 'tailwind-merge';

import {GhostBarSideStatusesI} from './resolveGhostBarSideStatuses';

const HALF_COLOR_CLASS_NAMES: Record<string, string> = {
    COMPLETED: 'bg-green-500',
    FAILED: 'bg-red-500',
};

interface TaskDispatcherGhostBarHalvesProps extends GhostBarSideStatusesI {
    isFlippedRingBar: boolean;
}

/** The two halves of a ghost bar, each painted with the status of the side that hangs off its own handle. */
export default function TaskDispatcherGhostBarHalves({
    isFlippedRingBar,
    leftStatus,
    rightStatus,
}: TaskDispatcherGhostBarHalvesProps) {
    const [startStatus, endStatus] = isFlippedRingBar ? [rightStatus, leftStatus] : [leftStatus, rightStatus];

    return (
        <>
            <span
                className={twMerge('flex-1', startStatus && HALF_COLOR_CLASS_NAMES[startStatus])}
                data-ghost-bar-half="start"
            />

            <span
                className={twMerge('flex-1', endStatus && HALF_COLOR_CLASS_NAMES[endStatus])}
                data-ghost-bar-half="end"
            />
        </>
    );
}
