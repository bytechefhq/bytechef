import Button from '@/components/Button/Button';
import AiAgentEvalTestCard from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/tests/AiAgentEvalTestCard';
import CreateTestDialog from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/tests/CreateTestDialog';
import useAiAgentEvalsTestsTab from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/tests/hooks/useAiAgentEvalsTestsTab';
import {FlaskConicalIcon, Loader2Icon, PlusIcon} from 'lucide-react';
import {useState} from 'react';

interface EvalsTestsTabProps {
    workflowId: string;
    workflowNodeName: string;
}

const EvalsTestsTab = ({workflowId, workflowNodeName}: EvalsTestsTabProps) => {
    const [showCreateDialog, setShowCreateDialog] = useState(false);

    const {
        evalTests,
        handleCreateScenario,
        handleCreateTest,
        handleDeleteScenario,
        handleDeleteTest,
        handleUpdateScenario,
        isLoading,
    } = useAiAgentEvalsTestsTab(workflowId, workflowNodeName);

    if (isLoading) {
        return (
            <div className="flex flex-1 items-center justify-center py-12">
                <Loader2Icon className="size-5 animate-spin text-gray-400" />
            </div>
        );
    }

    return (
        <div className="flex flex-1 flex-col gap-3 px-4">
            {evalTests.length > 0 ? (
                <>
                    <div className="flex items-center justify-end">
                        <Button
                            icon={<PlusIcon />}
                            label="New Test"
                            onClick={() => setShowCreateDialog(true)}
                            size="sm"
                        />
                    </div>

                    {evalTests.map((test) => (
                        <AiAgentEvalTestCard
                            key={test.id}
                            onCreateScenario={handleCreateScenario}
                            onDeleteScenario={handleDeleteScenario}
                            onDeleteTest={handleDeleteTest}
                            onUpdateScenario={handleUpdateScenario}
                            test={test}
                            workflowId={workflowId}
                            workflowNodeName={workflowNodeName}
                        />
                    ))}
                </>
            ) : (
                <div className="flex flex-1 flex-col items-center justify-center gap-3 py-16">
                    <div className="flex size-12 items-center justify-center rounded-full bg-emerald-100">
                        <FlaskConicalIcon className="size-6 text-emerald-600" />
                    </div>

                    <h3 className="text-sm font-semibold">Tests</h3>

                    <p className="max-w-sm text-center text-xs text-gray-500">
                        Create tests with scenarios to evaluate your agent.
                        <br />
                        Each test groups related scenarios that can be run together.
                    </p>

                    <Button
                        className="mt-2"
                        icon={<PlusIcon />}
                        label="New Test"
                        onClick={() => setShowCreateDialog(true)}
                    />
                </div>
            )}

            {showCreateDialog && (
                <CreateTestDialog onClose={() => setShowCreateDialog(false)} onCreate={handleCreateTest} />
            )}
        </div>
    );
};

export default EvalsTestsTab;
