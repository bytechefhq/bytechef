import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {
    useA2aProjectWorkflowsByA2aProjectIdQuery,
    useUpdateA2aProjectWorkflowParametersMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

interface A2aServerSkillsEditorProps {
    a2aProjectId: string;
}

const A2aServerSkillsEditor = ({a2aProjectId}: A2aServerSkillsEditorProps) => {
    const [editedNames, setEditedNames] = useState<Record<string, string>>({});
    const [editedDescriptions, setEditedDescriptions] = useState<Record<string, string>>({});

    const queryClient = useQueryClient();

    const {data} = useA2aProjectWorkflowsByA2aProjectIdQuery({a2aProjectId});

    const updateParametersMutation = useUpdateA2aProjectWorkflowParametersMutation();

    const a2aProjectWorkflows = data?.a2aProjectWorkflowsByA2aProjectId ?? [];

    const handleSave = (id: string, name: string, description: string) => {
        updateParametersMutation.mutate(
            {id, input: {skillDescription: description, skillName: name}},
            {
                onSuccess: () =>
                    queryClient.invalidateQueries({queryKey: ['a2aProjectWorkflowsByA2aProjectId']}),
            }
        );
    };

    if (a2aProjectWorkflows.length === 0) {
        return null;
    }

    return (
        <fieldset className="flex flex-col gap-3 border-0">
            <legend className="mb-1 text-sm font-medium">Skill details</legend>

            {a2aProjectWorkflows.filter((a2aProjectWorkflow) => a2aProjectWorkflow != null).map((a2aProjectWorkflow) => {
                const id = a2aProjectWorkflow!.id;
                const name = editedNames[id] ?? a2aProjectWorkflow!.skillName ?? '';
                const description = editedDescriptions[id] ?? a2aProjectWorkflow!.skillDescription ?? '';

                return (
                    <div className="flex flex-col gap-2 rounded-md border border-border/50 p-3" key={id}>
                        <span className="text-xs text-muted-foreground">
                            {a2aProjectWorkflow!.workflowLabel || a2aProjectWorkflow!.workflowId}
                        </span>

                        <Input
                            onChange={(event) => setEditedNames((prev) => ({...prev, [id]: event.target.value}))}
                            placeholder="Skill name (defaults to the workflow label)"
                            value={name}
                        />

                        <Input
                            onChange={(event) =>
                                setEditedDescriptions((prev) => ({...prev, [id]: event.target.value}))
                            }
                            placeholder="Skill description (defaults to the workflow description)"
                            value={description}
                        />

                        <div className="flex justify-end">
                            <Button
                                label="Save skill"
                                onClick={() => handleSave(id, name, description)}
                                size="sm"
                                type="button"
                                variant="outline"
                            />
                        </div>
                    </div>
                );
            })}
        </fieldset>
    );
};

export default A2aServerSkillsEditor;
