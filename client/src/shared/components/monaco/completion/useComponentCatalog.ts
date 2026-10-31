import {ComponentDefinitionApi as EmbeddedComponentDefinitionApi} from '@/ee/shared/middleware/embedded/configuration';
import {ComponentDefinitionApi as AutomationComponentDefinitionApi} from '@/shared/middleware/automation/configuration';
import {
    ComponentPropertyDisplayConditionsDocument,
    type ComponentPropertyDisplayConditionsQuery,
    type ComponentPropertyDisplayConditionsQueryVariables,
} from '@/shared/middleware/graphql';
import {ComponentOperationType} from '@/shared/middleware/graphql-types';
import {fetcher} from '@/shared/middleware/graphqlFetcher';
import {
    ActionDefinitionApi,
    ComponentDefinitionApi,
    ComponentDefinitionBasic,
    ConnectionDefinitionApi,
    TriggerDefinitionApi,
} from '@/shared/middleware/platform/configuration';
import {ActionDefinitionKeys} from '@/shared/queries/platform/actionDefinitions.queries';
import {ComponentDefinitionKeys} from '@/shared/queries/platform/componentDefinitions.queries';
import {ConnectDefinitionKeys} from '@/shared/queries/platform/connectionDefinitions.queries';
import {TriggerDefinitionKeys} from '@/shared/queries/platform/triggerDefinitions.queries';
import {DEFINITION_STALE_TIME} from '@/shared/queries/queryConstants';
import {useQuery, useQueryClient} from '@tanstack/react-query';
import {useMemo} from 'react';

import type {ComponentCatalogType} from './completionTypes';

const COMPONENT_LIST_REQUEST = {actionDefinitions: true, triggerDefinitions: true};

export function useComponentCatalog(options?: {embedded?: boolean}): ComponentCatalogType {
    const embedded = !!options?.embedded;

    const queryClient = useQueryClient();

    const {data: components} = useQuery<ComponentDefinitionBasic[], Error>({
        queryFn: () =>
            embedded
                ? (new EmbeddedComponentDefinitionApi().getComponentDefinitions(
                      COMPONENT_LIST_REQUEST
                  ) as unknown as Promise<ComponentDefinitionBasic[]>)
                : (new AutomationComponentDefinitionApi().getComponentDefinitions(
                      COMPONENT_LIST_REQUEST
                  ) as unknown as Promise<ComponentDefinitionBasic[]>),
        queryKey: [
            ...ComponentDefinitionKeys.componentDefinitions,
            'completionCatalog',
            embedded ? 'embedded' : 'platform',
        ],
        staleTime: DEFINITION_STALE_TIME,
    });

    return useMemo(() => {
        const componentList = components ?? [];

        const resolveVersion = (componentName: string, componentVersion?: number): number | undefined =>
            componentVersion ?? componentList.find((component) => component.name === componentName)?.version;

        return {
            components: componentList,
            getConnectionDefinition: async (componentName, componentVersion) => {
                const resolvedVersion = resolveVersion(componentName, componentVersion);

                if (resolvedVersion === undefined) {
                    return undefined;
                }

                return queryClient.fetchQuery({
                    queryFn: () =>
                        new ConnectionDefinitionApi().getComponentConnectionDefinition({
                            componentName,
                            componentVersion: resolvedVersion,
                        }),
                    queryKey: ConnectDefinitionKeys.connectionDefinition({
                        componentName,
                        componentVersion: resolvedVersion,
                    }),
                    staleTime: DEFINITION_STALE_TIME,
                });
            },
            getDefinition: async (componentName, componentVersion) => {
                const resolvedVersion = resolveVersion(componentName, componentVersion);

                if (resolvedVersion === undefined) {
                    return undefined;
                }

                const request = {componentName, componentVersion: resolvedVersion};

                return queryClient.fetchQuery({
                    queryFn: () => new ComponentDefinitionApi().getComponentDefinition(request),
                    queryKey: ComponentDefinitionKeys.componentDefinition(request),
                    staleTime: DEFINITION_STALE_TIME,
                });
            },
            getDisplayConditions: async (componentName, componentVersion, operationName, operationKind, parameters) => {
                const resolvedVersion = resolveVersion(componentName, componentVersion);

                if (resolvedVersion === undefined) {
                    return {};
                }

                const variables = {
                    componentName,
                    componentVersion: resolvedVersion,
                    operationName,
                    operationType:
                        operationKind === 'trigger' ? ComponentOperationType.Trigger : ComponentOperationType.Action,
                    parameters,
                };

                const data = await queryClient.fetchQuery({
                    queryFn: fetcher<
                        ComponentPropertyDisplayConditionsQuery,
                        ComponentPropertyDisplayConditionsQueryVariables
                    >(ComponentPropertyDisplayConditionsDocument, variables),
                    queryKey: ['componentPropertyDisplayConditions', variables],
                    staleTime: DEFINITION_STALE_TIME,
                });

                return (data.componentPropertyDisplayConditions ?? {}) as Record<string, boolean>;
            },
            getOperationProperties: async (componentName, componentVersion, operationName, operationKind) => {
                const resolvedVersion = resolveVersion(componentName, componentVersion);

                if (resolvedVersion === undefined) {
                    return [];
                }

                if (operationKind === 'trigger') {
                    const request = {componentName, componentVersion: resolvedVersion, triggerName: operationName};

                    const triggerDefinition = await queryClient.fetchQuery({
                        queryFn: () => new TriggerDefinitionApi().getComponentTriggerDefinition(request),
                        queryKey: TriggerDefinitionKeys.triggerDefinition(request),
                        staleTime: DEFINITION_STALE_TIME,
                    });

                    return triggerDefinition.properties ?? [];
                }

                const request = {actionName: operationName, componentName, componentVersion: resolvedVersion};

                const actionDefinition = await queryClient.fetchQuery({
                    queryFn: () => new ActionDefinitionApi().getComponentActionDefinition(request),
                    queryKey: ActionDefinitionKeys.actionDefinition(request),
                    staleTime: DEFINITION_STALE_TIME,
                });

                return actionDefinition.properties ?? [];
            },
        };
    }, [components, queryClient]);
}
