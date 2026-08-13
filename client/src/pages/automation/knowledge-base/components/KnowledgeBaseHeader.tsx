import LeftSidebarButton from '@/pages/automation/project/components/project-header/components/LeftSidebarButton';
import Header from '@/shared/layout/Header';
import {ReactNode} from 'react';

interface KnowledgeBaseHeaderProps {
    knowledgeBaseName: string | undefined;
    onToggleLeftSidebar: () => void;
    right?: ReactNode;
}

const KnowledgeBaseHeader = ({knowledgeBaseName, onToggleLeftSidebar, right}: KnowledgeBaseHeaderProps) => {
    return (
        <Header
            centerTitle
            position="main"
            right={right}
            title={
                <div className="flex items-center gap-1">
                    <LeftSidebarButton onLeftSidebarOpenClick={onToggleLeftSidebar} tooltip="See knowledge bases" />

                    <span>{knowledgeBaseName || 'Loading...'}</span>
                </div>
            }
        />
    );
};

export default KnowledgeBaseHeader;
