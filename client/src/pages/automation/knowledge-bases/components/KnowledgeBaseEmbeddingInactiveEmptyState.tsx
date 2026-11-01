import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import {KNOWLEDGE_BASES_DOCUMENTATION_URL} from '@/pages/automation/journey/automationJourneySteps';
import {AI_PROVIDERS_PATH} from '@/pages/automation/knowledge-bases/components/KnowledgeBasesEmptyState';
import {SparklesIcon} from 'lucide-react';
import {useNavigate} from 'react-router-dom';

/* Shown on an existing knowledge base when the environment has lost its embedding model. */
const KnowledgeBaseEmbeddingInactiveEmptyState = () => {
    const navigate = useNavigate();

    return (
        <EmptyState
            actions={<Button label="Go to AI Providers" onClick={() => void navigate(AI_PROVIDERS_PATH)} />}
            description="Documents in a knowledge base are searched by meaning, which needs an embedding model. Enable OpenAI, Mistral or Ollama for this environment to upload and search documents again."
            docsLink={{href: KNOWLEDGE_BASES_DOCUMENTATION_URL, label: 'How knowledge bases work'}}
            icon={<SparklesIcon />}
            title="Turn on an embedding model"
        />
    );
};

export default KnowledgeBaseEmbeddingInactiveEmptyState;
