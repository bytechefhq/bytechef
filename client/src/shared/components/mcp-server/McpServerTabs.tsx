import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {ReactNode, useState} from 'react';

import McpServerAddToolsButton, {McpServerAddToolsButtonProps} from './McpServerAddToolsButton';

export interface McpServerTabsProps extends McpServerAddToolsButtonProps {
    connectContent: ReactNode;
    toolsContent: ReactNode;
}

const McpServerTabs = ({
    connectContent,
    mcpComponentDialog,
    mcpServer,
    toolsContent,
    workflowDialog,
}: McpServerTabsProps) => {
    const [activeTab, setActiveTab] = useState('tools');

    return (
        <Tabs onValueChange={setActiveTab} value={activeTab}>
            <div className="flex items-center justify-between">
                <TabsList>
                    <TabsTrigger value="tools">Tools</TabsTrigger>

                    <TabsTrigger value="connect">Connect</TabsTrigger>
                </TabsList>

                {activeTab === 'tools' && (
                    <McpServerAddToolsButton
                        mcpComponentDialog={mcpComponentDialog}
                        mcpServer={mcpServer}
                        workflowDialog={workflowDialog}
                    />
                )}
            </div>

            <TabsContent className="pt-2" value="tools">
                {toolsContent}
            </TabsContent>

            <TabsContent className="max-w-(--breakpoint-lg) pt-3" value="connect">
                {connectContent}
            </TabsContent>
        </Tabs>
    );
};

export default McpServerTabs;
