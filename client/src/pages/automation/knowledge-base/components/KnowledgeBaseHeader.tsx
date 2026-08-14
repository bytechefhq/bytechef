import Header from '@/shared/layout/Header';
import {ReactNode} from 'react';

interface KnowledgeBaseHeaderProps {
    knowledgeBaseName: string | undefined;
    right?: ReactNode;
}

const KnowledgeBaseHeader = ({knowledgeBaseName, right}: KnowledgeBaseHeaderProps) => {
    return (
        <Header
            centerTitle
            position="main"
            right={right}
            title={
                <div className="flex items-center gap-1">
                    <span>{knowledgeBaseName || 'Loading...'}</span>
                </div>
            }
        />
    );
};

export default KnowledgeBaseHeader;
