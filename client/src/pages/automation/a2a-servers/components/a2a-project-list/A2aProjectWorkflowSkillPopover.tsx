import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {Label} from '@/components/ui/label';
import {PopoverContent} from '@/components/ui/popover';
import {Textarea} from '@/components/ui/textarea';
import {useUpdateA2aProjectWorkflowParametersMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {XIcon} from 'lucide-react';
import {SubmitEvent, useState} from 'react';

interface A2aProjectWorkflowSkillPopoverProps {
    a2aProjectWorkflowId: string;
    onClose: () => void;
    skillDescription?: string | null;
    skillName?: string | null;
    workflowLabel: string;
}

const A2aProjectWorkflowSkillPopover = ({
    a2aProjectWorkflowId,
    onClose,
    skillDescription,
    skillName,
    workflowLabel,
}: A2aProjectWorkflowSkillPopoverProps) => {
    const [description, setDescription] = useState(skillDescription ?? '');
    const [name, setName] = useState(skillName ?? '');

    const queryClient = useQueryClient();

    const updateA2aProjectWorkflowParametersMutation = useUpdateA2aProjectWorkflowParametersMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['a2aProjectWorkflowsByA2aProjectId']});

            onClose();
        },
    });

    const handleSubmit = (event: SubmitEvent<HTMLFormElement>) => {
        event.preventDefault();

        updateA2aProjectWorkflowParametersMutation.mutate({
            id: a2aProjectWorkflowId,
            input: {skillDescription: description, skillName: name},
        });
    };

    const nameInputId = `a2a-skill-name-${a2aProjectWorkflowId}`;
    const descriptionInputId = `a2a-skill-description-${a2aProjectWorkflowId}`;

    return (
        <PopoverContent
            align="end"
            className="flex max-h-(--radix-popover-content-available-height) w-md flex-col p-0"
            onInteractOutside={(event) => event.preventDefault()}
            side="bottom"
        >
            <div className="flex items-center justify-between p-3">
                <div>
                    <h4 className="text-sm font-medium">{`Edit ${workflowLabel} Skill`}</h4>

                    <p className="text-xs text-muted-foreground">
                        Configure the name and description this workflow is advertised with as an A2A skill.
                    </p>
                </div>

                <Button
                    aria-label="Close"
                    className="text-muted-foreground hover:text-foreground"
                    icon={<XIcon className="size-4" />}
                    onClick={onClose}
                    size="iconXs"
                    variant="ghost"
                />
            </div>

            <form className="flex min-h-0 flex-col" noValidate onSubmit={handleSubmit}>
                <fieldset className="min-h-0 flex-1 space-y-4 overflow-y-auto border-0 p-3">
                    <div className="flex flex-col gap-2">
                        <Label htmlFor={nameInputId}>Skill Name</Label>

                        <Input
                            id={nameInputId}
                            onChange={(event) => setName(event.target.value)}
                            placeholder="Defaults to workflow name"
                            value={name}
                        />
                    </div>

                    <div className="flex flex-col gap-2">
                        <Label htmlFor={descriptionInputId}>Skill Description</Label>

                        <Textarea
                            id={descriptionInputId}
                            onChange={(event) => setDescription(event.target.value)}
                            placeholder="Defaults to workflow description"
                            value={description}
                        />
                    </div>
                </fieldset>

                <div className="flex justify-end gap-2 p-3">
                    <Button label="Cancel" onClick={onClose} type="button" variant="outline" />

                    <Button
                        disabled={updateA2aProjectWorkflowParametersMutation.isPending}
                        label="Save"
                        type="submit"
                    />
                </div>
            </form>
        </PopoverContent>
    );
};

export default A2aProjectWorkflowSkillPopover;
