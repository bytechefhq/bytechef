import {Button} from '@/components/ui/button';
import {Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {RadioGroup, RadioGroupItem} from '@/components/ui/radio-group';
import {zodResolver} from '@hookform/resolvers/zod';
import React from 'react';
import {useForm} from 'react-hook-form';
import * as z from 'zod';

const appearanceFormSchema = z.object({
    theme: z.enum(['light', 'dark', 'system'], {
        required_error: 'Please select a theme.',
    }),
});

type AppearanceFormValues = z.infer<typeof appearanceFormSchema>;

const defaultValues: Partial<AppearanceFormValues> = {
    theme: localStorage.getItem('theme') as 'light' | 'dark' | 'system',
};

export default function Appearance() {
    const form = useForm<AppearanceFormValues>({
        defaultValues,
        resolver: zodResolver(appearanceFormSchema),
    });

    function onSubmit(data: AppearanceFormValues) {
        /* eslint-disable @typescript-eslint/no-explicit-any */
        (window as any).__setPreferredTheme(data.theme);
    }

    return (
        <Form {...form}>
            <form className="space-y-8" onSubmit={form.handleSubmit(onSubmit)}>
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
                                onValueChange={(e: 'light' | 'dark' | 'system') => field.onChange(e)}
                            >
                                <FormItem>
                                    <FormLabel className="[&:has([data-state=checked])>div]:border-primary">
                                        <FormControl>
                                            <RadioGroupItem className="sr-only" value="light" />
                                        </FormControl>

                                        <div className="items-center rounded-md border-2 border-muted p-1 hover:border-accent">
                                            <div className="space-y-2 rounded-sm bg-[#ecedef] p-2">
                                                <div className="space-y-2 rounded-md bg-white p-2 shadow-sm">
                                                    <div className="h-2 w-[80px] rounded-lg bg-[#ecedef]" />

                                                    <div className="h-2 w-[100px] rounded-lg bg-[#ecedef]" />
                                                </div>

                                                <div className="flex items-center space-x-2 rounded-md bg-white p-2 shadow-sm">
                                                    <div className="h-4 w-4 rounded-full bg-[#ecedef]" />

                                                    <div className="h-2 w-[100px] rounded-lg bg-[#ecedef]" />
                                                </div>

                                                <div className="flex items-center space-x-2 rounded-md bg-white p-2 shadow-sm">
                                                    <div className="h-4 w-4 rounded-full bg-[#ecedef]" />

                                                    <div className="h-2 w-[100px] rounded-lg bg-[#ecedef]" />
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

                                        <div className="items-center rounded-md border-2 border-muted bg-popover p-1 hover:bg-accent hover:text-accent-foreground">
                                            <div className="space-y-2 rounded-sm bg-slate-950 p-2">
                                                <div className="space-y-2 rounded-md bg-slate-800 p-2 shadow-sm">
                                                    <div className="h-2 w-[80px] rounded-lg bg-slate-400" />

                                                    <div className="h-2 w-[100px] rounded-lg bg-slate-400" />
                                                </div>

                                                <div className="flex items-center space-x-2 rounded-md bg-slate-800 p-2 shadow-sm">
                                                    <div className="h-4 w-4 rounded-full bg-slate-400" />

                                                    <div className="h-2 w-[100px] rounded-lg bg-slate-400" />
                                                </div>

                                                <div className="flex items-center space-x-2 rounded-md bg-slate-800 p-2 shadow-sm">
                                                    <div className="h-4 w-4 rounded-full bg-slate-400" />

                                                    <div className="h-2 w-[100px] rounded-lg bg-slate-400" />
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

                                        <div className="items-center rounded-md border-2 border-muted bg-popover p-1 hover:bg-accent hover:text-accent-foreground">
                                            <div className="space-y-2 rounded-sm bg-[#ecedef] p-2">
                                                <div className="space-y-2 rounded-md bg-slate-800 p-2 shadow-sm">
                                                    <div className="h-2 w-[80px] rounded-lg bg-slate-400" />

                                                    <div className="h-2 w-[100px] rounded-lg bg-slate-400" />
                                                </div>

                                                <div className="flex items-center space-x-2 rounded-md bg-slate-800 p-2 shadow-sm">
                                                    <div className="h-4 w-4 rounded-full bg-slate-400" />

                                                    <div className="h-2 w-[100px] rounded-lg bg-slate-400" />
                                                </div>

                                                <div className="flex items-center space-x-2 rounded-md bg-slate-800 p-2 shadow-sm">
                                                    <div className="h-4 w-4 rounded-full bg-slate-400" />

                                                    <div className="h-2 w-[100px] rounded-lg bg-slate-400" />
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

                <Button type="submit">Update preferences</Button>
            </form>
        </Form>
    );
}
