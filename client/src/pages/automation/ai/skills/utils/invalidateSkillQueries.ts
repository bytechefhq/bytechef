import {type QueryClient} from '@tanstack/react-query';

/* Invalidates every skill-related query (list, metadata, file paths, file contents) so views refetch
   after an external actor — e.g. the AI Copilot — modifies skills server-side. */
export default function invalidateSkillQueries(queryClient: QueryClient) {
    queryClient.invalidateQueries({queryKey: ['aiSkills']});
    queryClient.invalidateQueries({queryKey: ['aiSkill']});
    queryClient.invalidateQueries({queryKey: ['aiSkillFilePaths']});
    queryClient.invalidateQueries({queryKey: ['aiSkillFileContent']});
}
