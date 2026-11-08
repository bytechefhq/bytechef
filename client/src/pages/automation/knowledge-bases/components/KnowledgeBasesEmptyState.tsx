import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import JourneySteps, {type JourneyStepI} from '@/components/JourneySteps/JourneySteps';
import {KNOWLEDGE_BASES_DOCUMENTATION_URL} from '@/pages/automation/journey/automationJourneySteps';
import CreateKnowledgeBaseDialog from '@/pages/automation/knowledge-bases/components/CreateKnowledgeBaseDialog';
import {AI_PROVIDERS_LINK_LABEL, AI_PROVIDERS_SETTINGS_PATH} from '@/shared/constants';
import {BookOpenTextIcon} from 'lucide-react';
import {useMemo} from 'react';
import {useNavigate} from 'react-router-dom';

interface KnowledgeBasesEmptyStateProps {
    embeddingActive: boolean;
    workspaceId: string;
}

/*
 * A knowledge base can only ingest documents once the environment has an embedding model, so a new user is shown both
 * steps: turning on an embedding provider, then creating the first knowledge base.
 */
const KnowledgeBasesEmptyState = ({embeddingActive, workspaceId}: KnowledgeBasesEmptyStateProps) => {
    const navigate = useNavigate();

    const steps: JourneyStepI[] = useMemo(
        () => [
            {
                ...(!embeddingActive && {
                    action: (
                        <Button
                            label={AI_PROVIDERS_LINK_LABEL}
                            onClick={() => void navigate(AI_PROVIDERS_SETTINGS_PATH)}
                            size="sm"
                        />
                    ),
                    description: 'Enable OpenAI, Mistral or Ollama for this environment.',
                }),
                id: 'embeddingModel',
                label: 'Turn on an embedding model',
                status: embeddingActive ? 'complete' : 'current',
            },
            {
                ...(embeddingActive && {
                    action: (
                        <CreateKnowledgeBaseDialog
                            trigger={<Button label="Create Knowledge Base" size="sm" />}
                            workspaceId={workspaceId}
                        />
                    ),
                    description: 'Name it, then upload your documents.',
                }),
                id: 'createKnowledgeBase',
                label: 'Create a knowledge base',
                status: embeddingActive ? 'current' : 'upcoming',
            },
        ],
        [embeddingActive, navigate, workspaceId]
    );

    return (
        <EmptyState
            description="A knowledge base stores your documents and splits them into searchable pieces, so workflows and AI agents can find answers by meaning. Two steps to get there:"
            docsLink={{href: KNOWLEDGE_BASES_DOCUMENTATION_URL, label: 'How knowledge bases work'}}
            icon={<BookOpenTextIcon />}
            title="Create your first knowledge base"
        >
            <JourneySteps aria-label="Getting started" className="max-w-md" orientation="vertical" steps={steps} />
        </EmptyState>
    );
};

export default KnowledgeBasesEmptyState;
