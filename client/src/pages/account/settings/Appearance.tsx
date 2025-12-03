import {Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {RadioGroup, RadioGroupItem} from '@/components/ui/radio-group';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useTheme} from '@/shared/providers/theme-provider';
import {zodResolver} from '@hookform/resolvers/zod';
import {useForm} from 'react-hook-form';
import * as z from 'zod';

const appearanceFormSchema = z.object({
    theme: z.enum(['light', 'dark', 'system'], {
        message: 'Please select a theme.',
    }),
});

type AppearanceFormValuesType = z.infer<typeof appearanceFormSchema>;

export default function Appearance() {
    const {setTheme, theme} = useTheme();

    const form = useForm<AppearanceFormValuesType>({
        defaultValues: {
            theme,
        },
        resolver: zodResolver(appearanceFormSchema),
    });

    return (
        <LayoutContainer
            header={<Header centerTitle={true} position="main" title="Appearance" />}
            leftSidebarOpen={false}
        >
            <Form {...form}>
                <form className="space-y-8 p-4 3xl:mx-auto 3xl:w-4/5">
                    <FormField
                        control={form.control}
                        name="theme"
                        render={({field}) => (
                            <FormItem className="space-y-1">
                                <FormLabel>Theme</FormLabel>

                                <FormDescription>Select the theme for the dashboard.</FormDescription>

                                <FormMessage />

                                <RadioGroup
                                    className="grid max-w-xl grid-cols-3 gap-8 pt-2"
                                    defaultValue={field.value}
                                    onValueChange={(e: 'light' | 'dark' | 'system') => {
                                        field.onChange(e);

                                        setTheme(e);
                                    }}
                                >
                                    <FormItem>
                                        <FormLabel className="[&:has([data-state=checked])>div]:border-primary">
                                            <FormControl>
                                                <RadioGroupItem className="sr-only" value="light" />
                                            </FormControl>

                                            <div className="items-center rounded-md border-2 border-border/50 p-1 hover:border-accent">
                                                <div className="space-y-2 rounded-sm bg-skeleton p-2">
                                                    <div className="space-y-2 rounded-md bg-white p-2 shadow-sm">
                                                        <div className="h-2 w-appearance-theme-choice-skeleton-small-width rounded-lg bg-skeleton" />

                                                        <div className="h-2 w-appearance-theme-choice-skeleton-large-width rounded-lg bg-skeleton" />
                                                    </div>

                                                    <div className="flex items-center space-x-2 rounded-md bg-white p-2 shadow-sm">
                                                        <div className="size-4 rounded-full bg-skeleton" />

                                                        <div className="h-2 w-appearance-theme-choice-skeleton-large-width rounded-lg bg-skeleton" />
                                                    </div>

                                                    <div className="flex items-center space-x-2 rounded-md bg-white p-2 shadow-sm">
                                                        <div className="size-4 rounded-full bg-skeleton" />

                                                        <div className="h-2 w-appearance-theme-choice-skeleton-large-width rounded-lg bg-skeleton" />
                                                    </div>
                                                </div>
                                            </div>

                                            <span className="block w-full p-2 text-center font-normal">Light</span>
                                        </FormLabel>
                                    </FormItem>

                                    <FormItem>
                                        <FormLabel className="[&:has([data-state=checked])>div]:border-primary">
                                            <FormControl>
                                                <RadioGroupItem className="sr-only" value="dark" />
                                            </FormControl>

                                            <div className="items-center rounded-md border-2 border-border/50 bg-popover p-1 hover:bg-accent hover:text-accent-foreground">
                                                <div className="space-y-2 rounded-sm bg-slate-950 p-2">
                                                    <div className="space-y-2 rounded-md bg-slate-800 p-2 shadow-sm">
                                                        <div className="h-2 w-appearance-theme-choice-skeleton-small-width rounded-lg bg-slate-400" />

                                                        <div className="h-2 w-appearance-theme-choice-skeleton-large-width rounded-lg bg-slate-400" />
                                                    </div>

                                                    <div className="flex items-center space-x-2 rounded-md bg-slate-800 p-2 shadow-sm">
                                                        <div className="size-4 rounded-full bg-slate-400" />

                                                        <div className="h-2 w-appearance-theme-choice-skeleton-large-width rounded-lg bg-slate-400" />
                                                    </div>

                                                    <div className="flex items-center space-x-2 rounded-md bg-slate-800 p-2 shadow-sm">
                                                        <div className="size-4 rounded-full bg-slate-400" />

                                                        <div className="h-2 w-appearance-theme-choice-skeleton-large-width rounded-lg bg-slate-400" />
                                                    </div>
                                                </div>
                                            </div>

                                            <span className="block w-full p-2 text-center font-normal">Dark</span>
                                        </FormLabel>
                                    </FormItem>

                                    <FormItem>
                                        <FormLabel className="[&:has([data-state=checked])>div]:border-primary">
                                            <FormControl>
                                                <RadioGroupItem className="sr-only" value="system" />
                                            </FormControl>

                                            <div className="items-center rounded-md border-2 border-border/50 bg-popover p-1 hover:bg-accent hover:text-accent-foreground">
                                                <div className="space-y-2 rounded-sm bg-skeleton p-2">
                                                    <div className="space-y-2 rounded-md bg-slate-800 p-2 shadow-sm">
                                                        <div className="h-2 w-appearance-theme-choice-skeleton-small-width rounded-lg bg-slate-400" />

                                                        <div className="h-2 w-appearance-theme-choice-skeleton-large-width rounded-lg bg-slate-400" />
                                                    </div>

                                                    <div className="flex items-center space-x-2 rounded-md bg-slate-800 p-2 shadow-sm">
                                                        <div className="size-4 rounded-full bg-slate-400" />

                                                        <div className="h-2 w-appearance-theme-choice-skeleton-large-width rounded-lg bg-slate-400" />
                                                    </div>

                                                    <div className="flex items-center space-x-2 rounded-md bg-slate-800 p-2 shadow-sm">
                                                        <div className="size-4 rounded-full bg-slate-400" />

                                                        <div className="h-2 w-appearance-theme-choice-skeleton-large-width rounded-lg bg-slate-400" />
                                                    </div>
                                                </div>
                                            </div>

                                            <span className="block w-full p-2 text-center font-normal">System</span>
                                        </FormLabel>
                                    </FormItem>
                                </RadioGroup>
                            </FormItem>
                        )}
                    />
                </form>
            </Form>
        </LayoutContainer>
    );
}
