import {type AiAgentChannelDefinitionsQuery, useAiAgentChannelDefinitionsQuery} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

// Derived from the generated query rather than hand-written, so a field added to or renamed in the
// AiAgentChannelDefinition GraphQL type reaches every consumer as a type error instead of silently going missing.
export type AiAgentChannelDefinitionType = AiAgentChannelDefinitionsQuery['aiAgentChannelDefinitions'][number];

// whatsApp declares an agent channel server-side, but its request paths descend into a payload shape its trigger's
// declared output schema contradicts, and no live-webhook fixture confirms either spelling. Offering a channel whose
// message binding may never resolve would be worse than leaving it off the menu, so it is excluded here rather than
// through a server flag — the reason is one component's data quality, not a property of the channel kind. Existing
// whatsApp rows still render: only the add menu is affected. The channel returns to this menu once its request
// descriptor is confirmed against a live payload — see §7 of
// docs/superpowers/specs/2026-08-17-sdk-agent-channels-design.md, which records why the paths were reproduced
// verbatim rather than corrected, and names that confirmation as the follow-up.
const NON_ADDABLE_CHANNEL_TYPES = ['whatsapp'];

/**
 * Every channel an agent can be reached through, as the component registry declares it — the single source the channel
 * cards, their add menu, the approval-delivery picker and the deployment channel list read from, replacing the maps
 * that mirrored the registry by hand.
 */
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
