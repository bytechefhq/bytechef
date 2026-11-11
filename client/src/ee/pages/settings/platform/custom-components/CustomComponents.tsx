import EmptyState from '@/components/EmptyState/EmptyState';
import PageLoader from '@/components/PageLoader';
import CustomComponentList from '@/ee/pages/settings/platform/custom-components/components/CustomComponentList';
import UploadCustomComponentDialog from '@/ee/pages/settings/platform/custom-components/components/UploadCustomComponentDialog';
import {CUSTOM_COMPONENTS_DOCUMENTATION_URL} from '@/pages/settings/settingsDocumentationUrls';
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
                <Header
                    centerTitle={true}
                    description="Extend the component catalog with your own components."
                    position="main"
                    right={customComponents && customComponents.length > 0 && <UploadCustomComponentDialog />}
                    title="Custom Components"
                />
            }
            leftSidebarOpen={false}
        >
            <PageLoader errors={[customComponentsError]} loading={customComponentsLoading}>
                {customComponents && customComponents?.length > 0 ? (
                    <CustomComponentList customComponents={customComponents} />
                ) : (
                    <EmptyState
                        actions={<UploadCustomComponentDialog />}
                        description="Upload a JavaScript, Python or Ruby file, or a Java jar, to add your own component to the catalog."
                        docsLink={{href: CUSTOM_COMPONENTS_DOCUMENTATION_URL, label: 'How custom components work'}}
                        icon={<Link2Icon />}
                        title="No Custom Components"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default CustomComponents;
