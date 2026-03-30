import Button from '@/components/Button/Button';
import AgentEvalTestCard from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/tests/AgentEvalTestCard';
import CreateTestDialog from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/tests/CreateTestDialog';
import useAgentEvalsTestsTab from '@/pages/platform/cluster-element-editor/ai-agent-evals/hooks/useAgentEvalsTestsTab';
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
    } = useAgentEvalsTestsTab(workflowId, workflowNodeName);

    if (isLoading) {
        return (
            <div className="flex flex-1 items-center justify-center py-12">
                <Loader2Icon className="size-5 animate-spin text-gray-400" />
            </div>
        );
    }

    return (
        <div className="flex flex-1 flex-col gap-3 px-4">
            {evalTests.length > 0 &&
                evalTests.map((test) => (
                    <AgentEvalTestCard
                        key={test.id}
                        onCreateScenario={handleCreateScenario}
                        onDeleteScenario={handleDeleteScenario}
                        onDeleteTest={handleDeleteTest}
                        onUpdateScenario={handleUpdateScenario}
                        test={test}
                    />
                ))}

            {evalTests.length > 0 && (
                <button
                    className="flex w-full items-center justify-center gap-1 rounded-md border border-dashed border-gray-300 py-2.5 text-sm text-gray-500 hover:border-gray-400 hover:text-gray-600"
                    onClick={() => setShowCreateDialog(true)}
                >
                    <PlusIcon className="size-4" />
                    New Test
                </button>
            )}

            {evalTests.length === 0 && (
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
