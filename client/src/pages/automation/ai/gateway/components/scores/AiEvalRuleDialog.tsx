import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useCreateAiEvalRuleMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

import {AiEvalScoreConfigType} from '../../types';

interface AiEvalRuleDialogProps {
    onClose: () => void;
    scoreConfigs: AiEvalScoreConfigType[];
}

const AiEvalRuleDialog = ({onClose, scoreConfigs}: AiEvalRuleDialogProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const queryClient = useQueryClient();

    const [delaySeconds, setDelaySeconds] = useState('0');
    const [enabled, setEnabled] = useState(false);
    const [model, setModel] = useState('');
    const [name, setName] = useState('');
    const [promptTemplate, setPromptTemplate] = useState(
        'Evaluate the following LLM interaction.\n\nInput: {{input}}\n\nOutput: {{output}}\n\nRespond with a score from 0.0 to 1.0.'
    );
    const [samplingRate, setSamplingRate] = useState('1.0');
    const [scoreConfigId, setScoreConfigId] = useState('');

    const createMutation = useCreateAiEvalRuleMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiEvalRules']});
            onClose();
        },
    });

    const handleSubmit = () => {
        createMutation.mutate({
            delaySeconds: parseInt(delaySeconds) || undefined,
            enabled,
            model,
            name,
            promptTemplate,
            samplingRate: parseFloat(samplingRate),
            scoreConfigId,
            workspaceId: currentWorkspaceId != null ? String(currentWorkspaceId) : '',
        });
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="w-full max-w-lg rounded-lg bg-background p-6 shadow-lg">
                <h3 className="mb-4 text-lg font-semibold">New Eval Rule</h3>

                <fieldset className="space-y-4 border-0">
                    <div>
                        <label className="mb-1 block text-sm font-medium" htmlFor="evalRuleName">
                            Name
                        </label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            id="evalRuleName"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="e.g., Relevance check on production"
                            value={name}
                        />
                    </div>

                    <div>
                        <label className="mb-1 block text-sm font-medium" htmlFor="evalRuleScoreConfig">
                            Score Config
                        </label>

                        <select
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            id="evalRuleScoreConfig"
                            onChange={(event) => setScoreConfigId(event.target.value)}
                            value={scoreConfigId}
                        >
                            <option value="">Select a score config...</option>

                            {scoreConfigs.map((config) => (
                                <option key={config.id} value={config.id}>
                                    {config.name}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div>
                        <label className="mb-1 block text-sm font-medium" htmlFor="evalRuleModel">
                            Model
                        </label>

                        <input
                            className="w-full rounded-md border px-3 py-2 text-sm"
                            id="evalRuleModel"
                            onChange={(event) => setModel(event.target.value)}
                            placeholder="e.g., openai/gpt-4o-mini"
                            value={model}
                        />
                    </div>

                    <div>
                        <label className="mb-1 block text-sm font-medium" htmlFor="evalRulePromptTemplate">
                            Prompt Template
                        </label>

                        <textarea
                            className="w-full rounded-md border px-3 py-2 font-mono text-sm"
                            id="evalRulePromptTemplate"
                            onChange={(event) => setPromptTemplate(event.target.value)}
                            rows={5}
                            value={promptTemplate}
                        />

                        <p className="mt-1 text-xs text-muted-foreground">
                            Available variables: {'{{input}}'}, {'{{output}}'}, {'{{metadata}}'}
                        </p>
                    </div>

                    <div className="flex gap-4">
                        <div className="flex-1">
                            <label className="mb-1 block text-sm font-medium" htmlFor="evalRuleSamplingRate">
                                Sampling Rate (0.0 - 1.0)
                            </label>

                            <input
                                className="w-full rounded-md border px-3 py-2 text-sm"
                                id="evalRuleSamplingRate"
                                max="1"
                                min="0"
                                onChange={(event) => setSamplingRate(event.target.value)}
                                step="0.01"
                                type="number"
                                value={samplingRate}
                            />
                        </div>

                        <div className="flex-1">
                            <label className="mb-1 block text-sm font-medium" htmlFor="evalRuleDelaySeconds">
                                Delay (seconds)
                            </label>

                            <input
                                className="w-full rounded-md border px-3 py-2 text-sm"
                                id="evalRuleDelaySeconds"
                                min="0"
                                onChange={(event) => setDelaySeconds(event.target.value)}
                                type="number"
                                value={delaySeconds}
                            />
                        </div>
                    </div>

                    <div className="flex items-center gap-2">
                        <input
                            checked={enabled}
                            id="evalRuleEnabled"
                            onChange={(event) => setEnabled(event.target.checked)}
                            type="checkbox"
                        />

                        <label className="text-sm font-medium" htmlFor="evalRuleEnabled">
                            Enable immediately
                        </label>
                    </div>
                </fieldset>

                <div className="mt-6 flex justify-end gap-2">
                    <button
                        className="rounded-md px-3 py-1.5 text-sm text-muted-foreground hover:bg-muted"
                        onClick={onClose}
                    >
                        Cancel
                    </button>

                    <button
                        className="rounded-md bg-primary px-3 py-1.5 text-sm text-primary-foreground hover:bg-primary/90"
                        disabled={!name || !model || !scoreConfigId || !promptTemplate}
                        onClick={handleSubmit}
                    >
                        Create
                    </button>
                </div>
            </div>
        </div>
    );
};

export default AiEvalRuleDialog;
