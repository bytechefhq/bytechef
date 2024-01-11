import ComboBox from '@/components/ComboBox';
import {Button} from '@/components/ui/button';
import {Calendar} from '@/components/ui/calendar';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import LayoutContainer from '@/layouts/LayoutContainer';
import PageHeader from '@/layouts/PageHeader';
import {cn} from '@/lib/utils';
import {zodResolver} from '@hookform/resolvers/zod';
import {addDays, format} from 'date-fns';
import {CalendarIcon} from 'lucide-react';
import {HTMLAttributes, useState} from 'react';
import {DateRange} from 'react-day-picker';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

const formSchema = z.object({
    from: z.date(),
    integrationId: z.number(),
    name: z.string().min(2, {
        message: 'Name must be at least 2 characters.',
    }),
    status: z.string(),
    to: z.date(),
});

export function DatePickerWithRange({className}: HTMLAttributes<HTMLDivElement>) {
    const [date, setDate] = useState<DateRange | undefined>({
        from: addDays(new Date(), -20),
        to: new Date(),
    });

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
                        onSelect={setDate}
                        selected={date}
                    />
                </PopoverContent>
            </Popover>
        </div>
    );
}

const ConnectedUsers = () => {
    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            from: new Date(),
            name: '',
            to: addDays(new Date(), 20),
        },
        resolver: zodResolver(formSchema),
    });

    function onSubmit(values: z.infer<typeof formSchema>) {
        console.log(values);
    }

    return (
        <LayoutContainer
            header={<PageHeader position="main" title="All Users" />}
            leftSidebarBody={
                <Form {...form}>
                    <form className="space-y-4 px-4" onSubmit={form.handleSubmit(onSubmit)}>
                        <FormField
                            control={form.control}
                            name="name"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Search</FormLabel>

                                    <FormControl>
                                        <Input placeholder="Name or User Id" {...field} />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={form.control}
                            name="status"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Connection Status</FormLabel>

                                    <FormControl>
                                        <Select {...field}>
                                            <SelectTrigger>
                                                <SelectValue placeholder="Choose Status..." />
                                            </SelectTrigger>

                                            <SelectContent>
                                                <SelectItem value="valid">Valid</SelectItem>

                                                <SelectItem value="invalid">Invalid</SelectItem>
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
                                        <ComboBox items={[]} {...field} />
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
                                    <FormLabel>Created Date</FormLabel>

                                    <FormControl>
                                        <DatePickerWithRange {...field} />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                    </form>
                </Form>
            }
            leftSidebarHeader={<PageHeader title="Connected Users" />}
            leftSidebarWidth="72"
        >
            <div className="p-4">Table TODO</div>
        </LayoutContainer>
    );
};

export default ConnectedUsers;
