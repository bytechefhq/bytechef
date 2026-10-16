import Header from '@/shared/layout/Header';

interface KnowledgeBaseHeaderProps {
    knowledgeBaseName: string | undefined;
}

const KnowledgeBaseHeader = ({knowledgeBaseName}: KnowledgeBaseHeaderProps) => {
    return <Header centerTitle position="main" title={<span>{knowledgeBaseName || 'Loading...'}</span>} />;
};

export default KnowledgeBaseHeader;
