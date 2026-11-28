import {type AiAgentChannelDefinitionsQuery, useAiAgentChannelDefinitionsQuery} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

export type AiAgentChannelDefinitionType = AiAgentChannelDefinitionsQuery['aiAgentChannelDefinitions'][number];

const NON_ADDABLE_CHANNEL_TYPES = ['whatsapp'];

export const useAiAgentChannelDefinitions = () => {
    const {data, isLoading} = useAiAgentChannelDefinitionsQuery();

    const definitions: AiAgentChannelDefinitionType[] = useMemo(() => data?.aiAgentChannelDefinitions ?? [], [data]);

    const addableDefinitions = useMemo(
        () =>
            definitions.filter(
                (definition) =>
                    !definition.pinned &&
                    !definition.schedule &&
                    !NON_ADDABLE_CHANNEL_TYPES.includes(definition.channelType)
            ),
        [definitions]
    );

    const definitionsByType: Record<string, AiAgentChannelDefinitionType> = useMemo(
        () => Object.fromEntries(definitions.map((definition) => [definition.channelType, definition])),
        [definitions]
    );

    return {addableDefinitions, definitions, definitionsByType, isLoading};
};
