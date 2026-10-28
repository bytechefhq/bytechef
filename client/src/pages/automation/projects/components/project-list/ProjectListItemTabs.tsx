import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {ReactNode} from 'react';

export type ProjectListItemTabType = 'deployments' | 'workflows';

interface ProjectListItemTabsProps {
    actions?: ReactNode;
    activeTab: ProjectListItemTabType;
    deploymentsContent: ReactNode;
    onTabChange: (tab: ProjectListItemTabType) => void;
    workflowsContent: ReactNode;
}

const ProjectListItemTabs = ({
    actions,
    activeTab,
    deploymentsContent,
    onTabChange,
    workflowsContent,
}: ProjectListItemTabsProps) => (
    <Tabs onValueChange={(value) => onTabChange(value as ProjectListItemTabType)} value={activeTab}>
        <div className="flex items-center justify-between">
            <TabsList>
                <TabsTrigger value="workflows">Workflows</TabsTrigger>

                <TabsTrigger value="deployments">Deployments</TabsTrigger>
            </TabsList>

            {actions}
        </div>

        <TabsContent className="pt-2" value="workflows">
            {workflowsContent}
        </TabsContent>

        <TabsContent className="pt-2" value="deployments">
            {deploymentsContent}
        </TabsContent>
    </Tabs>
);

export default ProjectListItemTabs;
