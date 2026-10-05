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
    onAddComponentClick: () => void;
    onAddWorkflowsClick: () => void;
}

export interface McpServerTabsProps {
    connectContent: ReactNode;
    mcpComponentDialog: ComponentType<McpServerComponentDialogProps>;
    mcpServer: McpServer;
    toolsContent: (toolsContentProps: McpServerToolsContentProps) => ReactNode;
    workflowDialog: ComponentType<McpServerWorkflowDialogProps>;
}

const McpServerTabs = ({
    connectContent,
    mcpComponentDialog: McpComponentDialog,
    mcpServer,
    toolsContent,
    workflowDialog: WorkflowDialog,
}: McpServerTabsProps) => {
    const [activeTab, setActiveTab] = useState('tools');
    const [activeToolsTab, setActiveToolsTab] = useState<McpServerToolsTabType>('components');
    const [showMcpComponentDialog, setShowMcpComponentDialog] = useState(false);
    const [showWorkflowDialog, setShowWorkflowDialog] = useState(false);

    const isComponentsTab = activeToolsTab === 'components';

    const handleAddClick = () => {
        if (isComponentsTab) {
            setShowMcpComponentDialog(true);
        } else {
            setShowWorkflowDialog(true);
        }
    };

    return (
        <>
            <Tabs onValueChange={setActiveTab} value={activeTab}>
                <TabsList>
                    <TabsTrigger value="tools">Tools</TabsTrigger>

                    <TabsTrigger value="connect">Connect</TabsTrigger>
                </TabsList>

                <TabsContent className="flex flex-col gap-3 pt-2" value="tools">
                    <div className="flex items-center justify-between">
                        <Tabs
                            onValueChange={(value) => setActiveToolsTab(value as McpServerToolsTabType)}
                            value={activeToolsTab}
                        >
                            <TabsList aria-label="Tool type">
                                <TabsTrigger value="components">Components</TabsTrigger>

                                <TabsTrigger value="workflows">Workflows</TabsTrigger>
                            </TabsList>
                        </Tabs>

                        <Button
                            label={isComponentsTab ? 'Add Component' : 'Add Workflows'}
                            onClick={handleAddClick}
                            size="sm"
                            variant="secondary"
                        />
                    </div>

                    {toolsContent({
                        activeToolsTab,
                        onAddComponentClick: () => setShowMcpComponentDialog(true),
                        onAddWorkflowsClick: () => setShowWorkflowDialog(true),
                    })}
                </TabsContent>

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
