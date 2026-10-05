import Button from '@/components/Button/Button';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {McpServer} from '@/shared/middleware/graphql';
import {ComponentType, ReactNode, useState} from 'react';

export type McpServerToolsTabType = 'components' | 'workflows';

export interface McpServerComponentDialogProps {
    mcpServerId: string;
    onOpenChange: (open: boolean) => void;
    open: boolean;
}

export interface McpServerWorkflowDialogProps {
    mcpServer: McpServer;
    onClose: () => void;
}

export interface McpServerToolsContentProps {
    activeToolsTab: McpServerToolsTabType;
    mcpServer: McpServer;
    onAddComponentClick: () => void;
    onAddWorkflowsClick: () => void;
}

export interface McpServerTabsProps {
    connectContent: ReactNode;
    mcpComponentDialog: ComponentType<McpServerComponentDialogProps>;
    mcpServer: McpServer;
    toolsContent: ComponentType<McpServerToolsContentProps>;
    workflowDialog: ComponentType<McpServerWorkflowDialogProps>;
}

const McpServerTabs = ({
    connectContent,
    mcpComponentDialog: McpComponentDialog,
    mcpServer,
    toolsContent: ToolsContent,
    workflowDialog: WorkflowDialog,
}: McpServerTabsProps) => {
    const [activeTab, setActiveTab] = useState<McpServerToolsTabType | 'connect'>('components');
    const [showMcpComponentDialog, setShowMcpComponentDialog] = useState(false);
    const [showWorkflowDialog, setShowWorkflowDialog] = useState(false);

    const isComponentsTab = activeTab === 'components';

    const handleAddClick = () => {
        if (isComponentsTab) {
            setShowMcpComponentDialog(true);
        } else {
            setShowWorkflowDialog(true);
        }
    };

    return (
        <>
            <Tabs onValueChange={(value) => setActiveTab(value as McpServerToolsTabType | 'connect')} value={activeTab}>
                <div className="flex items-center justify-between">
                    <TabsList>
                        <TabsTrigger value="components">Components</TabsTrigger>

                        <TabsTrigger value="workflows">Workflows</TabsTrigger>

                        <TabsTrigger value="connect">Connect</TabsTrigger>
                    </TabsList>

                    {activeTab !== 'connect' && (
                        <Button
                            label={isComponentsTab ? 'Add Component' : 'Add Workflows'}
                            onClick={handleAddClick}
                            size="sm"
                            variant="secondary"
                        />
                    )}
                </div>

                {(['components', 'workflows'] as const).map((toolsTab) => (
                    <TabsContent className="pt-2" key={toolsTab} value={toolsTab}>
                        <ToolsContent
                            activeToolsTab={toolsTab}
                            mcpServer={mcpServer}
                            onAddComponentClick={() => setShowMcpComponentDialog(true)}
                            onAddWorkflowsClick={() => setShowWorkflowDialog(true)}
                        />
                    </TabsContent>
                ))}

                <TabsContent className="max-w-(--breakpoint-lg) pt-3" value="connect">
                    {connectContent}
                </TabsContent>
            </Tabs>

            {showMcpComponentDialog && (
                <McpComponentDialog
                    mcpServerId={mcpServer.id}
                    onOpenChange={setShowMcpComponentDialog}
                    open={showMcpComponentDialog}
                />
            )}

            {showWorkflowDialog && (
                <WorkflowDialog mcpServer={mcpServer} onClose={() => setShowWorkflowDialog(false)} />
            )}
        </>
    );
};

export default McpServerTabs;
