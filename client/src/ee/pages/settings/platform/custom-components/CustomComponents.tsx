import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import CreateCustomComponentDialog from '@/ee/pages/settings/platform/custom-components/components/CreateCustomComponentDialog';
import CustomComponentList from '@/ee/pages/settings/platform/custom-components/components/CustomComponentList';
import UploadCustomComponentDialog from '@/ee/pages/settings/platform/custom-components/components/UploadCustomComponentDialog';
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
                customComponents &&
                customComponents.length > 0 && (
                    <Header
                        centerTitle={true}
                        position="main"
                        right={<NewCustomComponentMenu />}
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
                        button={<NewCustomComponentMenu align="start" />}
                        icon={<Link2Icon className="size-12 text-content-neutral-tertiary" />}
                        message="You do not have any Custom Components created yet."
                        title="No Custom Components"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default CustomComponents;
