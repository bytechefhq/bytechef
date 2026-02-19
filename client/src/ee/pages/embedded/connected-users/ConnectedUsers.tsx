import ComboBox from '@/components/ComboBox/ComboBox';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import TablePagination from '@/components/TablePagination';
import {Button} from '@/components/ui/button';
import {Calendar} from '@/components/ui/calendar';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import ConnectedUserTable from '@/ee/pages/embedded/connected-users/components/ConnectedUserTable';
import ConnectedUsersFilterTitle from '@/ee/pages/embedded/connected-users/components/ConnectedUsersFilterTitle';
import ConnectedUserSheet from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheet';
import {Integration} from '@/ee/shared/middleware/embedded/configuration';
import {ConnectedUserFromJSON, type CredentialStatus} from '@/ee/shared/middleware/embedded/connected-user';
import {useGetConnectedUsersQuery} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {useGetIntegrationsQuery} from '@/ee/shared/queries/embedded/integrations.queries';
import Footer from '@/shared/layout/Footer';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {cn} from '@/shared/util/cn-utils';
import {zodResolver} from '@hookform/resolvers/zod';
import {format} from 'date-fns';
import {CalendarIcon, UsersIcon} from 'lucide-react';
import {HTMLAttributes, useState} from 'react';
import {DateRange} from 'react-day-picker';
import {useForm} from 'react-hook-form';
import {useNavigate, useSearchParams} from 'react-router-dom';
import {z} from 'zod';

const formSchema = z.object({
    createDateRange: z.any().optional(),
    credentialStatus: z.string().optional(),
    integrationId: z.number().optional(),
    search: z.string().optional(),
});

export function DatePickerWithRange({
    className,
    onSelect,
    value,
}: {
    onSelect: (dateRange: DateRange | undefined) => void;
    value: DateRange | undefined;
} & HTMLAttributes<HTMLDivElement>) {
    const [date, setDate] = useState<DateRange | undefined>(value);

    return (
        <div className={cn('grid gap-2', className)}>
            <Popover>
                <PopoverTrigger asChild>
                    <Button
                        className={cn('justify-start text-left font-normal', !date && 'text-muted-foreground')}
                        id="date"
                        variant={'outline'}
                    >
                        <CalendarIcon className="mr-2 size-4" />

                        {date?.from ? (
                            date.to ? (
                                <>
                                    {format(date.from, 'LLL dd, y')} - {format(date.to, 'LLL dd, y')}
                                </>
                            ) : (
                                format(date.from, 'LLL dd, y')
                            )
                        ) : (
                            <span>Pick a date</span>
                        )}
                    </Button>
                </PopoverTrigger>

                <PopoverContent align="start" className="w-auto p-0">
                    <Calendar
                        defaultMonth={date?.from}
                        initialFocus
                        mode="range"
                        numberOfMonths={2}
                        onSelect={(dateRange) => {
                            setDate(dateRange);

                            onSelect(dateRange);
                        }}
                        selected={date}
                    />
                </PopoverContent>
            </Popover>
        </div>
    );
}

const IntegrationLabel = ({integration}: {integration: Integration}) => (
    <div className="flex items-center">
        <span className="mr-1">{integration.componentName}</span>

        <span className="text-xs text-gray-500">{integration?.tags?.map((tag) => tag.name).join(', ')}</span>
    </div>
);

const ConnectedUsers = () => {
    const [searchParams] = useSearchParams();

    const [pageNumber, setPageNumber] = useState<number | undefined>(
        searchParams.get('pageNumber') ? +searchParams.get('pageNumber')! : undefined
    );

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const navigate = useNavigate();

    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            createDateRange: {
                from: searchParams.get('createDateFrom') ? new Date(+searchParams.get('createDateFrom')!) : undefined,
                to: searchParams.get('createDateTo') ? new Date(+searchParams.get('createDateTo')!) : undefined,
            },
            credentialStatus: searchParams.get('credentialStatus')
                ? (searchParams.get('credentialStatus')! as CredentialStatus)
                : undefined,
            integrationId: searchParams.get('integrationId') ? +searchParams.get('integrationId')! : undefined,
            search: searchParams.get('search') ? searchParams.get('search')! : '',
        },
        resolver: zodResolver(formSchema),
    });

    const {
        data: connectedUsersPage,
        error: connectedUsersError,
        isLoading: connectedUsersLoading,
    } = useGetConnectedUsersQuery({
        createDateFrom: searchParams.get('createDateFrom') ? new Date(+searchParams.get('createDateFrom')!) : undefined,
        createDateTo: searchParams.get('createDateTo') ? new Date(+searchParams.get('createDateTo')!) : undefined,
        credentialStatus: searchParams.get('credentialStatus')
            ? (searchParams.get('credentialStatus')! as CredentialStatus)
            : undefined,
        environmentId: currentEnvironmentId,
        integrationId: searchParams.get('integrationId') ? +searchParams.get('integrationId')! : undefined,
        pageNumber: searchParams.get('pageNumber') ? +searchParams.get('pageNumber')! : undefined,
        search: searchParams.get('search') ? searchParams.get('search')! : undefined,
    });

    const connectedUsers = connectedUsersPage?.content?.map((connectedUser: object) =>
        ConnectedUserFromJSON(connectedUser)
    );

    const {data: integrations} = useGetIntegrationsQuery({includeAllFields: false});

    function filter(
        search?: string,
        credentialStatus?: string,
        integrationIdId?: number,
        createDateRange?: DateRange,
        pageNumber?: number
    ) {
        navigate(
            `/embedded/connected-users?search=${search ? search : ''}&credentialStatus=${credentialStatus ? credentialStatus : ''}&integrationIdId=${integrationIdId ? integrationIdId : ''}&createDateFrom=${createDateRange?.from ? createDateRange.from?.getTime() : ''}&createDateTo=${createDateRange?.to ? createDateRange.to?.getTime() : ''}&pageNumber=${pageNumber ? pageNumber : ''}`
        );
    }

    const handleFilterConnectedUsers = (values: z.infer<typeof formSchema>) => {
        filter(values.search, values.credentialStatus, values.integrationId, values.createDateRange, pageNumber);
    };

    const handlePaginationClick = (pageNumber: number) => {
        setPageNumber(pageNumber);

        filter(
            form.getValues().search,
            form.getValues().credentialStatus,
            form.getValues().integrationId,
            form.getValues().createDateRange,
            pageNumber
        );
    };

    return (
        <LayoutContainer
            footer={
                connectedUsersPage?.content &&
                connectedUsersPage.content.length > 0 && (
                    <Footer position="main">
                        <TablePagination
                            onClick={handlePaginationClick}
                            pageNumber={pageNumber ? pageNumber : 0}
                            pageSize={connectedUsersPage.size!}
                            totalElements={connectedUsersPage.totalElements!}
                            totalPages={connectedUsersPage.totalPages!}
                        />
                    </Footer>
                )
            }
            header={
                <Header
                    position="main"
                    title={
                        <ConnectedUsersFilterTitle
                            filterData={{
                                status: form.getValues('credentialStatus')!,
                            }}
                        />
                    }
                />
            }
            leftSidebarBody={
                <Form {...form}>
                    <form className="space-y-4 px-4" onSubmit={form.handleSubmit(handleFilterConnectedUsers)}>
                        <FormField
                            control={form.control}
                            name="search"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Search</FormLabel>

                                    <FormControl>
                                        <Input
                                            className="bg-background"
                                            placeholder="Name, Email or User ERC"
                                            {...field}
                                        />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={form.control}
                            name="credentialStatus"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Connection Status</FormLabel>

                                    <FormControl>
                                        <Select
                                            onValueChange={(value) => {
                                                field.onChange(value);

                                                form.handleSubmit(handleFilterConnectedUsers)();
                                            }}
                                            value={field.value}
                                        >
                                            <SelectTrigger className="bg-background">
                                                <SelectValue placeholder="Choose Status..." />
                                            </SelectTrigger>

                                            <SelectContent>
                                                <SelectItem value="VALID">Valid</SelectItem>

                                                <SelectItem value="INVALID">Invalid</SelectItem>
                                            </SelectContent>
                                        </Select>
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={form.control}
                            name="integrationId"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Integration</FormLabel>

                                    <FormControl>
                                        <ComboBox
                                            items={
                                                integrations?.length
                                                    ? integrations?.map((integration) => ({
                                                          label: <IntegrationLabel integration={integration} />,
                                                          value: integration.id,
                                                      }))
                                                    : []
                                            }
                                            onChange={(item) => {
                                                field.onChange(item?.value);

                                                form.handleSubmit(handleFilterConnectedUsers)();
                                            }}
                                            value={field.value}
                                        />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={form.control}
                            name="createDateRange"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Created Date</FormLabel>

                                    <FormControl>
                                        <DatePickerWithRange
                                            onSelect={(value) => {
                                                field.onChange(value);

                                                form.handleSubmit(handleFilterConnectedUsers)();
                                            }}
                                            value={field.value}
                                        />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                    </form>
                </Form>
            }
            leftSidebarHeader={<Header title="Connected Users" />}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[connectedUsersError]} loading={connectedUsersLoading}>
                {connectedUsers && connectedUsers?.length > 0 ? (
                    <ConnectedUserTable connectedUsers={connectedUsers} />
                ) : (
                    <EmptyList
                        icon={<UsersIcon className="size-24 text-gray-300" />}
                        message="Get started by connecting new integrations."
                        title="No Connected Users"
                    />
                )}
            </PageLoader>

            <ConnectedUserSheet />
        </LayoutContainer>
    );
};

export default ConnectedUsers;
