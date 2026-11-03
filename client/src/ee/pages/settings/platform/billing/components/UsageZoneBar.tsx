import {useMemo} from 'react';

interface UsageZoneBarPropsI {
    jobsExecuted: number;
    productUnitLimit: number;
}

const UsageZoneBar = ({jobsExecuted, productUnitLimit}: UsageZoneBarPropsI) => {
    const {blueFill, blueZoneWidth, orangeFill, orangeZoneWidth} = useMemo(() => {
        const totalRange = Math.max(jobsExecuted, productUnitLimit);

        if (totalRange <= 0) {
            return {blueFill: 0, blueZoneWidth: 100, orangeFill: 0, orangeZoneWidth: 0};
        }

        return {
            blueFill: productUnitLimit > 0 ? Math.min(jobsExecuted / productUnitLimit, 1) * 100 : 0,
            blueZoneWidth: (productUnitLimit / totalRange) * 100,
            orangeFill: jobsExecuted > productUnitLimit ? 100 : 0,
            orangeZoneWidth: (Math.max(0, jobsExecuted - productUnitLimit) / totalRange) * 100,
        };
    }, [productUnitLimit, jobsExecuted]);

    return (
        <>
            <div className="h-full bg-muted" style={{width: `${blueZoneWidth}%`}}>
                <div className="h-full bg-blue-500" style={{width: `${blueFill}%`}} />
            </div>

            {orangeZoneWidth > 0 && (
                <div className="h-full bg-muted" style={{width: `${orangeZoneWidth}%`}}>
                    <div className="h-full bg-orange-500" style={{width: `${orangeFill}%`}} />
                </div>
            )}
        </>
    );
};

export default UsageZoneBar;
