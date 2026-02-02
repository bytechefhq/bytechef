import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import CustomComponentList from '@/ee/pages/settings/platform/custom-components/components/CustomComponentList';
import UploadCustomComponentDialog from '@/ee/pages/settings/platform/custom-components/components/UploadCustomComponentDialog';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useCustomComponentsQuery} from '@/shared/middleware/graphql';
import {Link2Icon} from 'lucide-react';

const CustomComponents = () => {
    const {
        data: customComponentsData,
        error: customComponentsError,
        isLoading: customComponentsLoading,
    } = useCustomComponentsQuery();

    const customComponents = customComponentsData?.customComponents;

    return (
        <LayoutContainer
            header={
                customComponents &&
                customComponents.length > 0 && (
                    <Header
                        centerTitle={true}
                        position="main"
                        right={<UploadCustomComponentDialog />}
                        title="Custom Components"
                    />
                )
            }
            leftSidebarOpen={false}
        >
            <PageLoader errors={[customComponentsError]} loading={customComponentsLoading}>
                {customComponents && customComponents?.length > 0 ? (
                    <CustomComponentList customComponents={customComponents} />
                ) : (
                    <EmptyList
                        button={<UploadCustomComponentDialog />}
                        icon={<Link2Icon className="size-12 text-gray-400" />}
                        message="You do not have any Custom Components created yet."
                        title="No Custom Components"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default CustomComponents;
