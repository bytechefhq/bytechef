import ComponentRulesTab from '@/ee/pages/settings/automation/ai/rules/ComponentRulesTab';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';

const ComponentRules = () => (
    <LayoutContainer
        header={
            <Header
                centerTitle
                description="Choose which tool calls AI agents in this workspace may make: block, tag, or require approval for a call based on its input."
                position="main"
                title="Agent Rules"
            />
        }
        leftSidebarOpen={false}
    >
        <div className="flex w-full flex-1 flex-col px-4 py-2 3xl:mx-auto 3xl:w-4/5">
            <ComponentRulesTab />
        </div>
    </LayoutContainer>
);

export default ComponentRules;
