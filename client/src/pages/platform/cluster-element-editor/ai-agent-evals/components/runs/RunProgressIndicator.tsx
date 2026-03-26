import {twMerge} from 'tailwind-merge';

interface RunProgressIndicatorProps {
    completedScenarios: number;
    totalScenarios: number;
}

const RunProgressIndicator = ({completedScenarios, totalScenarios}: RunProgressIndicatorProps) => {
    const percentage = totalScenarios > 0 ? (completedScenarios / totalScenarios) * 100 : 0;

    return (
        <div className="flex items-center gap-2">
            <div className="h-2 w-24 overflow-hidden rounded-full bg-gray-200">
                <div
                    className={twMerge(
                        'h-full rounded-full transition-all',
                        percentage >= 100 ? 'bg-green-500' : 'bg-blue-500'
                    )}
                    style={{width: `${Math.min(percentage, 100)}%`}}
                />
            </div>

            <span className="text-xs text-gray-500">
                {completedScenarios} / {totalScenarios}
            </span>
        </div>
    );
};

export default RunProgressIndicator;
