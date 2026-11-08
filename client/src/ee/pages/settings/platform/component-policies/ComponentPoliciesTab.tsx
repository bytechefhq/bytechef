import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';

import ComponentVisibilityTab from './components/ComponentVisibilityTab';

const ComponentPoliciesTab = () => {
    return (
        <Tabs className="size-full px-6 pb-4" defaultValue="component-visibility">
            <TabsList>
                <TabsTrigger value="component-visibility">Component Visibility</TabsTrigger>
            </TabsList>

            <TabsContent value="component-visibility">
                <ComponentVisibilityTab />
            </TabsContent>
        </Tabs>
    );
};

export default ComponentPoliciesTab;
