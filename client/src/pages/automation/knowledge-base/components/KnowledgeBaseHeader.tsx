import {Breadcrumb, BreadcrumbItem, BreadcrumbList} from '@/components/ui/breadcrumb';
import ListPageBreadcrumbItem from '@/shared/components/ListPageBreadcrumbItem';
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
                <Breadcrumb className="min-w-0">
                    <BreadcrumbList className="flex-nowrap">
                        <ListPageBreadcrumbItem label="Knowledge Bases" to="/automation/knowledge-bases" />

                        <BreadcrumbItem className="min-w-0 text-content-neutral-primary">
                            <h1 className="truncate">{knowledgeBaseName || 'Loading...'}</h1>
                        </BreadcrumbItem>
                    </BreadcrumbList>
                </Breadcrumb>
            }
        />
    );
};

export default KnowledgeBaseHeader;
