import {ComponentDefinitionApi as EmbeddedComponentDefinitionApi} from '@/ee/shared/middleware/embedded/configuration';
import {ComponentDefinitionApi as AutomationComponentDefinitionApi} from '@/shared/middleware/automation/configuration';
import {
    ComponentDefinitionApi,
    ComponentDefinitionBasic,
    ConnectionDefinitionApi,
} from '@/shared/middleware/platform/configuration';
import {ComponentDefinitionKeys} from '@/shared/queries/platform/componentDefinitions.queries';
import {ConnectDefinitionKeys} from '@/shared/queries/platform/connectionDefinitions.queries';
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
        };
    }, [components, queryClient]);
}
