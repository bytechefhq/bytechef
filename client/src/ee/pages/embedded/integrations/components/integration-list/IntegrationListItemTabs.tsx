import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {ReactNode} from 'react';

export type IntegrationListItemTabType = 'actions' | 'configurations' | 'tools' | 'workflows';

interface IntegrationListItemTabsProps {
    actionCount?: number;
    actions?: ReactNode;
    actionsContent: ReactNode;
    activeTab: IntegrationListItemTabType;
    configurationsContent: ReactNode;
    onTabChange: (tab: IntegrationListItemTabType) => void;
    toolCount?: number;
    toolsContent: ReactNode;
    workflowsContent: ReactNode;
}

const TabCount = ({count}: {count?: number}) =>
    count === undefined ? null : <span className="ml-1.5 text-xs text-muted-foreground">{count}</span>;

const IntegrationListItemTabs = ({
    actionCount,
    actions,
    actionsContent,
    activeTab,
    configurationsContent,
    onTabChange,
    toolCount,
    toolsContent,
    workflowsContent,
}: IntegrationListItemTabsProps) => (
    <Tabs onValueChange={(value) => onTabChange(value as IntegrationListItemTabType)} value={activeTab}>
        <div className="flex items-center justify-between">
            <TabsList>
                <TabsTrigger value="workflows">Workflows</TabsTrigger>

                <TabsTrigger value="configurations">Configurations</TabsTrigger>

                <TabsTrigger value="actions">
                    Actions
                    <TabCount count={actionCount} />
                </TabsTrigger>

                <TabsTrigger value="tools">
                    Tools
                    <TabCount count={toolCount} />
                </TabsTrigger>
            </TabsList>

            {actions}
        </div>

        <TabsContent className="pt-2" value="workflows">
            {workflowsContent}
        </TabsContent>

        <TabsContent className="pt-2" value="configurations">
            {configurationsContent}
        </TabsContent>

        <TabsContent className="pt-2" value="actions">
            {actionsContent}
        </TabsContent>

        <TabsContent className="pt-2" value="tools">
            {toolsContent}
        </TabsContent>
    </Tabs>
);

export default IntegrationListItemTabs;
