import {EvaluatorFunctionDefinition, useEvaluatorFunctionDefinitionsQuery} from '@/shared/middleware/graphql';

// The evaluator function catalog is static per deployment, so fetch it once and never refetch.
export function useEvaluatorFunctionDefinitions(): EvaluatorFunctionDefinition[] {
    const {data} = useEvaluatorFunctionDefinitionsQuery({}, {staleTime: Infinity});

    return data?.evaluatorFunctionDefinitions ?? [];
}
