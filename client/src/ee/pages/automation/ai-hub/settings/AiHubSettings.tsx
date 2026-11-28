import ToolApprovals from '@/ee/pages/automation/ai-hub/settings/ToolApprovals';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';

const AiHubSettings = () => (
    <LayoutContainer
        header={
            <Header
                centerTitle
                description="Configure how the AI Hub works in this workspace."
                position="main"
                title="AI Hub"
            />
        }
        leftSidebarOpen={false}
    >
        <div className="flex w-full flex-1 flex-col gap-8 px-4 py-6 3xl:mx-auto 3xl:w-4/5">
            <ToolApprovals />
        </div>
    </LayoutContainer>
);

export default AiHubSettings;
