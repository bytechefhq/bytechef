import Button from '@/components/Button/Button';
import AgentJudgeCard from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/judges/AgentJudgeCard';
import CreateJudgeDialog from '@/pages/platform/cluster-element-editor/ai-agent-evals/components/judges/CreateJudgeDialog';
import useAgentEvalsJudgesTab from '@/pages/platform/cluster-element-editor/ai-agent-evals/hooks/useAgentEvalsJudgesTab';
import {GavelIcon, Loader2Icon, PlusIcon} from 'lucide-react';
import {useState} from 'react';

import type {AgentJudgesQuery} from '@/shared/middleware/graphql';

type AgentJudgeListItemType = AgentJudgesQuery['agentJudges'][number];

interface EvalsJudgesTabProps {
    workflowId: string;
    workflowNodeName: string;
}

const EvalsJudgesTab = ({workflowId, workflowNodeName}: EvalsJudgesTabProps) => {
    const [editingJudge, setEditingJudge] = useState<AgentJudgeListItemType | null>(null);
    const [showCreateDialog, setShowCreateDialog] = useState(false);

    const {handleCreateJudge, handleDeleteJudge, handleUpdateJudge, isLoading, judges} = useAgentEvalsJudgesTab(
        workflowId,
        workflowNodeName
    );

    if (isLoading) {
        return (
            <div className="flex flex-1 items-center justify-center py-12">
                <Loader2Icon className="size-5 animate-spin text-gray-400" />
            </div>
        );
    }

    return (
        <div className="flex flex-1 flex-col gap-3 px-4">
            {judges.length > 0 ? (
                <>
                    <div className="flex items-center justify-end">
                        <Button
                            icon={<PlusIcon />}
                            label="New Judge"
                            onClick={() => setShowCreateDialog(true)}
                            size="sm"
                        />
                    </div>

                    {judges.map((judge) => (
                        <AgentJudgeCard
                            judge={judge}
                            key={judge.id}
                            onDelete={handleDeleteJudge}
                            onEdit={(selectedJudge) => setEditingJudge(selectedJudge)}
                        />
                    ))}
                </>
            ) : (
                <div className="flex flex-1 flex-col items-center justify-center gap-3 py-16">
                    <div className="flex size-12 items-center justify-center rounded-full bg-violet-100">
                        <GavelIcon className="size-6 text-violet-600" />
                    </div>

                    <h3 className="text-sm font-semibold">Agent-level Judges</h3>

                    <p className="max-w-sm text-center text-xs text-gray-500">
                        Agent-level judges run on every scenario automatically.
                        <br />
                        For scenario-specific judges, add them directly in the Tests tab.
                    </p>

                    <Button
                        className="mt-2"
                        icon={<PlusIcon />}
                        label="New Judge"
                        onClick={() => setShowCreateDialog(true)}
                    />
                </div>
            )}

            {showCreateDialog && (
                <CreateJudgeDialog onClose={() => setShowCreateDialog(false)} onCreate={handleCreateJudge} />
            )}

            {editingJudge && (
                <CreateJudgeDialog
                    editData={editingJudge}
                    onClose={() => setEditingJudge(null)}
                    onCreate={handleCreateJudge}
                    onUpdate={handleUpdateJudge}
                />
            )}
        </div>
    );
};

export default EvalsJudgesTab;
