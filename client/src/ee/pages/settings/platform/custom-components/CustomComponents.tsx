import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import PageLoader from '@/components/PageLoader';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import CreateCustomComponentDialog from '@/ee/pages/settings/platform/custom-components/components/CreateCustomComponentDialog';
import CustomComponentList from '@/ee/pages/settings/platform/custom-components/components/CustomComponentList';
import UploadCustomComponentDialog from '@/ee/pages/settings/platform/custom-components/components/UploadCustomComponentDialog';
import {CUSTOM_COMPONENTS_DOCUMENTATION_URL} from '@/pages/settings/settingsDocumentationUrls';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useCustomComponentsQuery} from '@/shared/middleware/graphql';
import {ChevronDownIcon, Link2Icon, PlusIcon, UploadIcon} from 'lucide-react';

interface NewCustomComponentMenuProps {
    align?: 'end' | 'start';
}

const NewCustomComponentMenu = ({align = 'end'}: NewCustomComponentMenuProps) => (
    <DropdownMenu>
        <DropdownMenuTrigger asChild>
            <Button>
                New Component
                <ChevronDownIcon className="ml-2 size-4" />
            </Button>
        </DropdownMenuTrigger>

        <DropdownMenuContent align={align}>
            <CreateCustomComponentDialog
                trigger={
                    <DropdownMenuItem onSelect={(event) => event.preventDefault()}>
                        <PlusIcon className="mr-2 size-4" />
                        New Component
                    </DropdownMenuItem>
                }
            />

            <UploadCustomComponentDialog
                trigger={
                    <DropdownMenuItem onSelect={(event) => event.preventDefault()}>
                        <UploadIcon className="mr-2 size-4" />
                        Import Component
                    </DropdownMenuItem>
                }
            />
        </DropdownMenuContent>
    </DropdownMenu>
);

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
                    right={customComponents && customComponents.length > 0 && <NewCustomComponentMenu />}
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
                        actions={<NewCustomComponentMenu align="start" />}
                        description="Upload a JavaScript or Python file, or a Java jar, to add your own component to the catalog."
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
