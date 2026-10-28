import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {ReactNode} from 'react';

export type AutomationWorkflowProjectListItemTabType = 'configurations' | 'workflows';

interface AutomationWorkflowProjectListItemTabsProps {
    actions?: ReactNode;
    activeTab: AutomationWorkflowProjectListItemTabType;
    configurationsContent: ReactNode;
    onTabChange: (tab: AutomationWorkflowProjectListItemTabType) => void;
    workflowsContent: ReactNode;
}

const AutomationWorkflowProjectListItemTabs = ({
    actions,
    activeTab,
    configurationsContent,
    onTabChange,
    workflowsContent,
}: AutomationWorkflowProjectListItemTabsProps) => (
    <Tabs onValueChange={(value) => onTabChange(value as AutomationWorkflowProjectListItemTabType)} value={activeTab}>
        <div className="flex items-center justify-between">
            <TabsList>
                <TabsTrigger value="workflows">Workflows</TabsTrigger>

                <TabsTrigger value="configurations">Configurations</TabsTrigger>
            </TabsList>

            {actions}
        </div>

        <TabsContent className="pt-2" value="workflows">
            {workflowsContent}
        </TabsContent>

        <TabsContent className="pt-2" value="configurations">
            {configurationsContent}
        </TabsContent>
    </Tabs>
);

export default AutomationWorkflowProjectListItemTabs;
