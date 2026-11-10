import {type QueryClient} from '@tanstack/react-query';

/* Invalidates every skill-related query (list, metadata, tags, file paths, file contents) so views
   refetch after an external actor — e.g. the AI Copilot — modifies skills server-side.

   `aiSkillTags` is in here because the facet list is derived from the tags skills actually carry, so
   creating or deleting a skill can change it. Callers that only ever touch one facet should invalidate
   that facet directly rather than reach for this — a tag edit, for instance, cannot stale a file. */
export default function invalidateSkillQueries(queryClient: QueryClient) {
    void queryClient.invalidateQueries({queryKey: ['aiSkills']});
    void queryClient.invalidateQueries({queryKey: ['aiSkill']});
    void queryClient.invalidateQueries({queryKey: ['aiSkillTags']});
    void queryClient.invalidateQueries({queryKey: ['aiSkillFilePaths']});
    void queryClient.invalidateQueries({queryKey: ['aiSkillFileContent']});
}
