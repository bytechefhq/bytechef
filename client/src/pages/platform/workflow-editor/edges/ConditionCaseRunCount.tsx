import useConditionCaseCount from '../hooks/useConditionCaseCount';

interface ConditionCaseRunCountProps {
    conditionCase: string;
    conditionName: string;
}

export default function ConditionCaseRunCount({conditionCase, conditionName}: ConditionCaseRunCountProps) {
    const conditionCaseCount = useConditionCaseCount(conditionName, conditionCase);

    if (conditionCaseCount < 2) {
        return null;
    }

    return (
        <span className="ml-1 font-semibold" title={`Ran ${conditionCaseCount} times in this run`}>
            ×{conditionCaseCount}
        </span>
    );
}
