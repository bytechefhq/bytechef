import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Checkbox} from '@/components/ui/checkbox';
import {Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
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
        <Dialog
            onOpenChange={(open) => {
                if (!open) {
                    onClose();
                }
            }}
            open
        >
            <DialogContent aria-describedby={undefined} className="max-w-lg">
                <DialogHeader>
                    <DialogTitle>New Eval Rule</DialogTitle>
                </DialogHeader>

                <fieldset className="space-y-4 border-0">
                    <div>
                        <Label className="mb-1 block" htmlFor="evalRuleName">
                            Name
                        </Label>

                        <Input
                            id="evalRuleName"
                            onChange={(event) => setName(event.target.value)}
                            placeholder="e.g., Relevance check on production"
                            value={name}
                        />
                    </div>

                    <div>
                        <Label className="mb-1 block" htmlFor="evalRuleScoreConfig">
                            Score Config
                        </Label>

                        <Select onValueChange={setScoreConfigId} value={scoreConfigId}>
                            <SelectTrigger id="evalRuleScoreConfig">
                                <SelectValue placeholder="Select a score config..." />
                            </SelectTrigger>

                            <SelectContent>
                                {scoreConfigs.map((config) => (
                                    <SelectItem key={config.id} value={config.id}>
                                        {config.name}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>

                    <div>
                        <Label className="mb-1 block" htmlFor="evalRuleModel">
                            Model
                        </Label>

                        <Input
                            id="evalRuleModel"
                            onChange={(event) => setModel(event.target.value)}
                            placeholder="e.g., openai/gpt-4o-mini"
                            value={model}
                        />
                    </div>

                    <div>
                        <Label className="mb-1 block" htmlFor="evalRulePromptTemplate">
                            Prompt Template
                        </Label>

                        <Textarea
                            className="font-mono"
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
                            <Label className="mb-1 block" htmlFor="evalRuleSamplingRate">
                                Sampling Rate (0.0 - 1.0)
                            </Label>

                            <Input
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
                            <Label className="mb-1 block" htmlFor="evalRuleDelaySeconds">
                                Delay (seconds)
                            </Label>

                            <Input
                                id="evalRuleDelaySeconds"
                                min="0"
                                onChange={(event) => setDelaySeconds(event.target.value)}
                                type="number"
                                value={delaySeconds}
                            />
                        </div>
                    </div>

                    <div className="flex items-center gap-2">
                        <Checkbox
                            checked={enabled}
                            id="evalRuleEnabled"
                            onCheckedChange={(checked) => setEnabled(checked === true)}
                        />

                        <Label htmlFor="evalRuleEnabled">Enable immediately</Label>
                    </div>
                </fieldset>

                <DialogFooter>
                    <Button label="Cancel" onClick={onClose} variant="outline" />

                    <Button
                        disabled={!name || !model || !scoreConfigId || !promptTemplate || createMutation.isPending}
                        label="Create"
                        onClick={handleSubmit}
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default AiEvalRuleDialog;
