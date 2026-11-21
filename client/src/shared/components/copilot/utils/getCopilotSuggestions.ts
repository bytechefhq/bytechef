import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {type SuggestionConfig} from '@assistant-ui/react';

const AI_AGENT_SUGGESTIONS: SuggestionConfig[] = [
    {
        label: 'does and which tools it uses.',
        prompt: 'Explain what this agent does and which tools it uses.',
        title: 'Explain what this agent',
    },
    {
        label: "to this agent's instructions",
        prompt: "Suggest improvements to this agent's instructions.",
        title: 'Suggest improvements',
    },
    {
        label: 'would help this agent?',
        prompt: 'Which tools would help this agent do its job?',
        title: 'Which tools',
    },
    {
        label: 'on a schedule?',
        prompt: 'How do I run this agent on a schedule?',
        title: 'How do I run this agent',
    },
];

const DEFAULT_SUGGESTIONS: SuggestionConfig[] = [
    {
        label: 'does end-to-end.',
        prompt: 'Describe what this workflow does end-to-end.',
        title: 'Describe what this workflow',
    },
    {
        label: 'of this action are required?',
        prompt: 'Which properties of this action are required?',
        title: 'Which properties',
    },
    {
        label: 'that can send an email',
        prompt: 'Search for an action that can send an email',
        title: 'Search for an action',
    },
    {
        label: 'conditional branching in workflows?',
        prompt: 'How do I implement conditional branching in workflows?',
        title: 'How do I implement',
    },
];

export default function getCopilotSuggestions(source?: Source): SuggestionConfig[] {
    return source === Source.AI_AGENT ? AI_AGENT_SUGGESTIONS : DEFAULT_SUGGESTIONS;
}
