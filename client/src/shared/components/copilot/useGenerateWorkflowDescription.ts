import {GenerateWorkflowDescriptionInput, useGenerateWorkflowDescriptionMutation} from '@/shared/middleware/graphql';

export function useGenerateWorkflowDescription() {
    const {isPending, mutateAsync} = useGenerateWorkflowDescriptionMutation();

    const generate = async (input: GenerateWorkflowDescriptionInput) => {
        const data = await mutateAsync({input});

        return data.generateWorkflowDescription;
    };

    return {generate, isPending};
}
