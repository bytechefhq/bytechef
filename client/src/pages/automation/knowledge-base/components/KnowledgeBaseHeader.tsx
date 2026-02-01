import Button from '@/components/Button/Button';
import Header from '@/shared/layout/Header';
import {ArrowLeft} from 'lucide-react';

interface KnowledgeBaseHeaderProps {
    knowledgeBaseName: string | undefined;
    onBackClick: () => void;
}

const KnowledgeBaseHeader = ({knowledgeBaseName, onBackClick}: KnowledgeBaseHeaderProps) => {
    return (
        <Header
            centerTitle={true}
            position="main"
            title={
                <div className="flex items-center space-x-2">
                    <Button
                        aria-label="Back"
                        icon={<ArrowLeft className="size-5" />}
                        onClick={onBackClick}
                        size="icon"
                        variant="ghost"
                    />

                    <span>{knowledgeBaseName || 'Loading...'}</span>
                </div>
            }
        />
    );
};

export default KnowledgeBaseHeader;
