import Switch from '@/components/Switch/Switch';
import {
    ComponentOperationPoliciesQuery,
    ComponentOperationType,
    useComponentOperationPoliciesQuery,
    useUpdateComponentOperationPolicyMutation,
} from '@/shared/middleware/graphql';
import {
    ComponentDefinitionKeys,
    useGetComponentDefinitionQuery,
} from '@/shared/queries/platform/componentDefinitions.queries';
import {useQueryClient} from '@tanstack/react-query';
import {twMerge} from 'tailwind-merge';

interface ComponentOperationPolicyListProps {
    componentEnabled: boolean;
    componentName: string;
    componentVersion: number;
}

interface OperationBasicI {
    name: string;
    title?: string;
}

const ComponentOperationPolicyList = ({
    componentEnabled,
    componentName,
    componentVersion,
}: ComponentOperationPolicyListProps) => {
    const queryClient = useQueryClient();

    const {data: componentDefinition} = useGetComponentDefinitionQuery({componentName, componentVersion});

    const operationPoliciesQueryKey = ['ComponentOperationPolicies', {componentName}];

    const {data: operationPoliciesData} = useComponentOperationPoliciesQuery({componentName});

    const operationPolicies = operationPoliciesData?.componentOperationPolicies ?? [];

    const updateComponentOperationPolicyMutation = useUpdateComponentOperationPolicyMutation<
        unknown,
        {previous?: ComponentOperationPoliciesQuery}
    >({
        onError: (_error, _variables, context) => {
            if (context?.previous) {
                queryClient.setQueryData(operationPoliciesQueryKey, context.previous);
            }
        },
        onMutate: async ({enabled, operationName, operationType}) => {
            await queryClient.cancelQueries({queryKey: operationPoliciesQueryKey});

            const previous = queryClient.getQueryData<ComponentOperationPoliciesQuery>(operationPoliciesQueryKey);

            queryClient.setQueryData<ComponentOperationPoliciesQuery>(operationPoliciesQueryKey, (current) => {
                if (!current) {
                    return current;
                }

                const remainingComponentOperationPolicies = current.componentOperationPolicies.filter(
                    (operationPolicy) =>
                        !(
                            operationPolicy.operationType === operationType &&
                            operationPolicy.operationName === operationName
                        )
                );

                return {
                    componentOperationPolicies: enabled
                        ? remainingComponentOperationPolicies
                        : [...remainingComponentOperationPolicies, {componentName, operationName, operationType}],
                };
            });

            return {previous};
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: operationPoliciesQueryKey});

            queryClient.invalidateQueries({
                queryKey: ComponentDefinitionKeys.componentDefinition({componentName, componentVersion}),
            });
        },
    });

    const disabledOperationKeys = new Set(
        operationPolicies.map((operationPolicy) => `${operationPolicy.operationType}#${operationPolicy.operationName}`)
    );

    const buildOperationRows = (
        operationType: ComponentOperationType,
        definitionOperations: OperationBasicI[]
    ): OperationBasicI[] => {
        const definitionOperationNames = new Set(definitionOperations.map((operation) => operation.name));

        const deniedOnlyOperationRows = operationPolicies
            .filter(
                (operationPolicy) =>
                    operationPolicy.operationType === operationType &&
                    !definitionOperationNames.has(operationPolicy.operationName)
            )
            .map((operationPolicy) => ({name: operationPolicy.operationName}));

        return [...definitionOperations, ...deniedOnlyOperationRows];
    };

    const renderOperations = (operationType: ComponentOperationType, label: string, operations: OperationBasicI[]) =>
        operations.length > 0 && (
            <div className="flex flex-col gap-1" key={operationType}>
                <span className="text-xs font-semibold text-muted-foreground uppercase">{label}</span>

                {operations.map((operation) => (
                    <div className="flex items-center justify-between py-1" key={operation.name}>
                        <span className={twMerge('text-sm', !componentEnabled && 'text-muted-foreground')}>
                            {operation.title || operation.name}
                        </span>

                        <Switch
                            aria-label={operation.title || operation.name}
                            checked={!disabledOperationKeys.has(`${operationType}#${operation.name}`)}
                            disabled={!componentEnabled}
                            onCheckedChange={(checked) =>
                                updateComponentOperationPolicyMutation.mutate({
                                    componentName,
                                    enabled: checked,
                                    operationName: operation.name,
                                    operationType,
                                })
                            }
                        />
                    </div>
                ))}
            </div>
        );

    return (
        <div className="flex flex-col gap-3 border-t px-4 py-3 pl-13">
            {renderOperations(
                ComponentOperationType.Action,
                'Actions',
                buildOperationRows(ComponentOperationType.Action, componentDefinition?.actions ?? [])
            )}

            {renderOperations(
                ComponentOperationType.Trigger,
                'Triggers',
                buildOperationRows(ComponentOperationType.Trigger, componentDefinition?.triggers ?? [])
            )}
        </div>
    );
};

export default ComponentOperationPolicyList;
