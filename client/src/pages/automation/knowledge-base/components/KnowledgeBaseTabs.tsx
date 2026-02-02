import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import KnowledgeBaseSearchInterface from '@/pages/automation/knowledge-base/components/KnowledgeBaseSearchInterface';
import UploadKnowledgeBaseDocumentDialog from '@/pages/automation/knowledge-base/components/UploadKnowledgeBaseDocumentDialog';
import KnowledgeBaseDocumentList from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/KnowledgeBaseDocumentList';
import {KnowledgeBaseDocument} from '@/shared/middleware/graphql';
import {FileTextIcon, SearchIcon} from 'lucide-react';

interface KnowledgeBaseTabsProps {
    documents: KnowledgeBaseDocument[];
    knowledgeBaseId: string;
}

const KnowledgeBaseTabs = ({documents, knowledgeBaseId}: KnowledgeBaseTabsProps) => {
    return (
        <Tabs className="flex-1" defaultValue="documents">
            <div className="mb-4 flex items-center justify-between">
                <TabsList>
                    <TabsTrigger value="documents">
                        <FileTextIcon className="mr-2 size-4" />
                        Documents
                    </TabsTrigger>

                    <TabsTrigger value="search">
                        <SearchIcon className="mr-2 size-4" />
                        Search
                    </TabsTrigger>
                </TabsList>

                <UploadKnowledgeBaseDocumentDialog knowledgeBaseId={knowledgeBaseId} />
            </div>

            <TabsContent className="flex-1" value="documents">
                <KnowledgeBaseDocumentList documents={documents} knowledgeBaseId={knowledgeBaseId} />
            </TabsContent>

            <TabsContent className="flex-1" value="search">
                <KnowledgeBaseSearchInterface knowledgeBaseId={knowledgeBaseId} />
            </TabsContent>
        </Tabs>
    );
};

export default KnowledgeBaseTabs;
