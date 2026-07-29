import Button from '@/components/Button/Button';
import Header from '@/shared/layout/Header';
import {ArrowLeftIcon} from 'lucide-react';
import {ReactNode} from 'react';

interface KnowledgeBaseHeaderProps {
    knowledgeBaseName: string | undefined;
    onBackClick: () => void;
    right?: ReactNode;
}

const KnowledgeBaseHeader = ({knowledgeBaseName, onBackClick, right}: KnowledgeBaseHeaderProps) => {
    return (
        <Header
            centerTitle
            position="main"
            right={right}
            title={
                <div className="flex items-center space-x-2">
                    <Button
                        aria-label="Back"
                        icon={<ArrowLeftIcon className="size-5" />}
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
