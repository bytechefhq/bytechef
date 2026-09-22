import {GeneratePropertyValueInput, useGeneratePropertyValueMutation} from '@/shared/middleware/graphql';

export function useGeneratePropertyValue() {
    const {isPending, mutateAsync} = useGeneratePropertyValueMutation();

    const generate = async (input: GeneratePropertyValueInput) => {
        const data = await mutateAsync({input});

        return data.generatePropertyValue;
    };

    return {generate, isPending};
}
