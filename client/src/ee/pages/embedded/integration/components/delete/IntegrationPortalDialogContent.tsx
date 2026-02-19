import {Button} from '@/components/ui/button';
import {Form, FormControl, FormDescription, FormField, FormItem, FormLabel} from '@/components/ui/form';
import {Switch} from '@/components/ui/switch';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {zodResolver} from '@hookform/resolvers/zod';
import * as React from 'react';
import {useForm} from 'react-hook-form';
import * as z from 'zod';

const notificationsFormSchema = z.object({
    communication_emails: z.boolean().default(false).optional(),
    marketing_emails: z.boolean().default(false).optional(),
    mobile: z.boolean().default(false).optional(),
    security_emails: z.boolean(),
    social_emails: z.boolean().default(false).optional(),
    type: z.enum(['all', 'mentions', 'none'], {
        message: 'You need to select a notification type.',
    }),
});

type NotificationsFormValuesType = z.infer<typeof notificationsFormSchema>;

// This can come from your database or API.
const defaultValues: Partial<NotificationsFormValuesType> = {
    communication_emails: false,
    marketing_emails: false,
    security_emails: true,
    social_emails: true,
};

const IntegrationPortalDialogContent = () => {
    const form = useForm<NotificationsFormValuesType>({
        defaultValues,
        resolver: zodResolver(notificationsFormSchema),
    });

    return (
        <>
            <Tabs defaultValue="overview">
                <TabsList className="grid w-full grid-cols-2">
                    <TabsTrigger value="overview">Overview</TabsTrigger>

                    <TabsTrigger value="configuration">Configuration</TabsTrigger>
                </TabsList>

                <TabsContent value="overview">
                    <div className="p-4 text-sm">
                        <p>
                            Lorem ipsum dolor sit amet, consectetur adipiscing elit. Mauris elementum, felis sit amet
                            facilisis interdum, mi nisl lobortis risus, vel commodo metus justo id mauris. Quisque eu
                            sapien turpis. Phasellus eleifend urna non elit ultrices, ut vulputate tellus efficitur.
                        </p>

                        <ul className="list-disc p-4">
                            <li>Fusce fermentum massa id ligula hendrerit bibendum.</li>

                            <li>Suspendisse porttitor ante ligula, in euismod neque malesuada nec.</li>

                            <li>Sed fringilla arcu sed turpis mollis, id gravida ante facilisis.</li>

                            <li>Aliquam laoreet urna vel tincidunt sodales.</li>

                            <li>
                                Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae.
                            </li>
                        </ul>
                    </div>
                </TabsContent>

                <TabsContent value="configuration">
                    <div className="mt-4">
                        <Form {...form}>
                            <form className="space-y-8">
                                <div className="space-y-4">
                                    <FormField
                                        control={form.control}
                                        name="communication_emails"
                                        render={({field}) => (
                                            <FormItem className="flex flex-row items-center justify-between rounded-lg border p-4">
                                                <div className="space-y-0.5">
                                                    <FormLabel className="text-base">Communication emails</FormLabel>

                                                    <FormDescription>
                                                        Receive emails about your account activity.
                                                    </FormDescription>
                                                </div>

                                                <FormControl>
                                                    <Switch checked={field.value} onCheckedChange={field.onChange} />
                                                </FormControl>
                                            </FormItem>
                                        )}
                                    />

                                    <FormField
                                        control={form.control}
                                        name="marketing_emails"
                                        render={({field}) => (
                                            <FormItem className="flex flex-row items-center justify-between rounded-lg border p-4">
                                                <div className="space-y-0.5">
                                                    <FormLabel className="text-base">Marketing emails</FormLabel>

                                                    <FormDescription>
                                                        Receive emails about new products, features, and more.
                                                    </FormDescription>
                                                </div>

                                                <FormControl>
                                                    <Switch checked={field.value} onCheckedChange={field.onChange} />
                                                </FormControl>
                                            </FormItem>
                                        )}
                                    />

                                    <FormField
                                        control={form.control}
                                        name="social_emails"
                                        render={({field}) => (
                                            <FormItem className="flex flex-row items-center justify-between rounded-lg border p-4">
                                                <div className="space-y-0.5">
                                                    <FormLabel className="text-base">Social emails</FormLabel>

                                                    <FormDescription>
                                                        Receive emails for friend requests, follows, and more.
                                                    </FormDescription>
                                                </div>

                                                <FormControl>
                                                    <Switch checked={field.value} onCheckedChange={field.onChange} />
                                                </FormControl>
                                            </FormItem>
                                        )}
                                    />

                                    <FormField
                                        control={form.control}
                                        name="security_emails"
                                        render={({field}) => (
                                            <FormItem className="flex flex-row items-center justify-between rounded-lg border p-4">
                                                <div className="space-y-0.5">
                                                    <FormLabel className="text-base">Security emails</FormLabel>

                                                    <FormDescription>
                                                        Receive emails about your account activity and security.
                                                    </FormDescription>
                                                </div>

                                                <FormControl>
                                                    <Switch
                                                        aria-readonly
                                                        checked={field.value}
                                                        disabled
                                                        onCheckedChange={field.onChange}
                                                    />
                                                </FormControl>
                                            </FormItem>
                                        )}
                                    />

                                    <FormField
                                        control={form.control}
                                        name="security_emails"
                                        render={() => (
                                            <FormItem className="flex flex-row items-center justify-between rounded-lg border p-4">
                                                <div className="space-y-0.5">
                                                    <FormLabel className="text-base">Disconnect Integration</FormLabel>
                                                </div>

                                                <FormControl>
                                                    <Button variant="destructive">Disconnect</Button>
                                                </FormControl>
                                            </FormItem>
                                        )}
                                    />
                                </div>
                            </form>
                        </Form>
                    </div>
                </TabsContent>
            </Tabs>
        </>
    );
};

export default IntegrationPortalDialogContent;
