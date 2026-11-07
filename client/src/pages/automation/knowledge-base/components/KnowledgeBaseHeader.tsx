import Header from '@/shared/layout/Header';
import {ReactNode} from 'react';

interface KnowledgeBaseHeaderProps {
    knowledgeBaseName: string | undefined;
    right?: ReactNode;
}

const KnowledgeBaseHeader = ({knowledgeBaseName, right}: KnowledgeBaseHeaderProps) => {
    return (
        <Header centerTitle position="main" right={right} title={<span>{knowledgeBaseName || 'Loading...'}</span>} />
    );
};

export default KnowledgeBaseHeader;
