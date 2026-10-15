import {useRun} from '@/pages/platform/workflow-editor/hooks/useRun';

const RunDisabledReasons = () => {
    const {runDisabledReasons} = useRun();

    return (
        <div className="flex flex-col gap-1">
            <span>The workflow cannot be executed yet:</span>

            <ul className="list-disc pl-4">
                {runDisabledReasons.map((runDisabledReason) => (
                    <li key={runDisabledReason.key}>{runDisabledReason.message}</li>
                ))}
            </ul>

            {runDisabledReasons.some((runDisabledReason) => runDisabledReason.key !== 'tasks') && (
                <span>Inputs and connections are set in the workflow test configuration.</span>
            )}
        </div>
    );
};

export default RunDisabledReasons;
