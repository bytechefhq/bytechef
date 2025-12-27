import DatePicker from '@/components/DatePicker/DatePicker';
import DateTimePicker from '@/components/DateTimePicker/DateTimePicker';
import {Button} from '@/components/ui/button';
import {Checkbox} from '@/components/ui/checkbox';
import {Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {RadioGroup, RadioGroupItem} from '@/components/ui/radio-group';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Textarea} from '@/components/ui/textarea';
import React, {useEffect, useMemo, useState} from 'react';
import {Controller, useForm} from 'react-hook-form';
import {useParams} from 'react-router-dom';
import sanitize from 'sanitize-html';

import {FieldType, FormInputType, TriggerFormType, fetchTriggerFormDefinition} from './util/triggerForm-utils';

export default function TriggerForm() {
    const {environment, workflowExecutionId} = useParams<{environment: string; workflowExecutionId: string}>();

    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [submitted, setSubmitted] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [definition, setDefinition] = useState<TriggerFormType | null>(null);

    const form = useForm<Record<string, unknown>>({
        defaultValues: {},
        mode: 'onSubmit',
    });

    useEffect(() => {
        if (!workflowExecutionId) {
            return;
        }

        const controller = new AbortController();

        const fetchDefinition = async () => {
            setError(null);

            try {
                const triggerFormDefinition = await fetchTriggerFormDefinition(workflowExecutionId, controller.signal);

                if (controller.signal.aborted) {
                    return;
                }

                setDefinition(triggerFormDefinition);

                if (triggerFormDefinition.inputs) {
                    const defaultValues: Record<string, unknown> = {};

                    triggerFormDefinition.inputs.forEach((input) => {
                        if (input.fieldType === FieldType.CUSTOM_HTML) {
                            return;
                        }

                        if (input.defaultValue !== undefined) {
                            if (input.fieldType === FieldType.CHECKBOX) {
                                defaultValues[input.fieldName] = !!input.defaultValue;
                            } else {
                                defaultValues[input.fieldName] = input.defaultValue;
                            }
                        }
                    });

                    form.reset(defaultValues);
                }
            } catch (e: unknown) {
                if (e instanceof Error && e.name === 'AbortError') {
                    return;
                }

                setError(e instanceof Error ? e.message : 'Failed to load trigger definition');
            } finally {
                if (!controller.signal.aborted) {
                    setLoading(false);
                }
            }
        };

        fetchDefinition();

        return () => {
            controller.abort();
        };
    }, [form, workflowExecutionId]);

    const ui = useMemo(() => {
        if (!definition) {
            return null;
        }

        const {appendAttribution, buttonLabel, customFormStyling, formDescription, formTitle, inputs} = definition;

        return {
            appendAttribution: appendAttribution ?? true,
            buttonLabel: buttonLabel || 'Submit',
            customFormStyling: customFormStyling || '',
            inputs: inputs || [],
            subtitle: formDescription || '',
            title: formTitle || 'Form',
        };
    }, [definition]);

    const handleSubmit = async (values: Record<string, unknown>) => {
        if (!workflowExecutionId) return;

        setSubmitting(true);
        setError(null);

        try {
            const hasFiles = ui?.inputs.some(
                (input) => input.fieldType === FieldType.FILE_INPUT && values[input.fieldName]
            );

            let body: BodyInit;
            const headers: Record<string, string> = {};

            if (hasFiles) {
                const formData = new FormData();

                formData.append('submittedAt', Date.now().toString());

                for (const key in values) {
                    const value = values[key];

                    if (Array.isArray(value)) {
                        value.forEach((v) => {
                            if (v instanceof File) {
                                formData.append(`body.${key}`, v);
                            } else if (v !== undefined && v !== null) {
                                formData.append(`body.${key}`, typeof v === 'object' ? JSON.stringify(v) : String(v));
                            }
                        });
                    } else if (value instanceof File) {
                        formData.append(`body.${key}`, value);
                    } else if (value !== undefined && value !== null) {
                        formData.append(
                            `body.${key}`,
                            typeof value === 'object' ? JSON.stringify(value) : String(value)
                        );
                    }
                }

                body = formData;
            } else {
                body = JSON.stringify({
                    body: values,
                    submittedAt: Date.now(),
                });

                headers['Content-Type'] = 'application/json';
            }

            const res = await fetch(`/webhooks/${workflowExecutionId}`, {
                body,
                headers,
                method: 'POST',
            });

            if (!res.ok) {
                throw new Error(`Submission failed: ${res.statusText}`);
            }

            setSubmitted(true);
        } catch (e: unknown) {
            setError(e instanceof Error ? e.message : 'Failed to submit form');
        } finally {
            setSubmitting(false);
        }
    };

    if (loading) {
        return (
            <div className="h-full overflow-auto">
                <div className="mx-auto w-full max-w-2xl p-6 text-center text-sm text-muted-foreground">
                    Loading form…
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="h-full overflow-auto">
                <div className="p-6">
                    <div className="text-sm text-destructive">{error}</div>
                </div>
            </div>
        );
    }

    if (submitted) {
        return (
            <div className="h-full overflow-auto">
                <div className="mx-auto w-full max-w-2xl p-6 text-center">
                    <h1 className="text-2xl font-semibold tracking-tight">Thank you!</h1>

                    <p className="mt-2 text-sm text-muted-foreground">Your response has been submitted.</p>
                </div>
            </div>
        );
    }

    if (!ui || !definition) {
        return (
            <div className="h-full overflow-auto">
                <div className="mx-auto w-full max-w-2xl p-6 text-center text-sm text-muted-foreground">
                    No definition found.
                </div>
            </div>
        );
    }

    const renderField = (name: string, formInput: Partial<FormInputType>) => {
        const {
            defaultValue,
            fieldDescription,
            fieldLabel,
            fieldName,
            fieldOptions,
            fieldType,
            formLabel,
            maxSelection,
            minSelection,
            multipleChoice,
            placeholder,
            required,
        } = formInput;

        if (fieldType === FieldType.HIDDEN_FIELD) {
            return <input name={name} readOnly type="hidden" value={defaultValue?.toString() ?? ''} />;
        }

        const label = formLabel || fieldLabel || fieldName || name;

        switch (fieldType) {
            case FieldType.INPUT:
            case FieldType.EMAIL_INPUT:
            case FieldType.NUMBER_INPUT:
            case FieldType.PASSWORD_INPUT: {
                const type =
                    fieldType === FieldType.EMAIL_INPUT
                        ? 'email'
                        : fieldType === FieldType.NUMBER_INPUT
                          ? 'number'
                          : fieldType === FieldType.PASSWORD_INPUT
                            ? 'password'
                            : 'text';
                return (
                    <FormField
                        control={form.control}
                        name={name}
                        render={({field}) => (
                            <FormItem className="space-y-2">
                                <FormLabel>{label}</FormLabel>

                                {fieldDescription && <FormDescription>{fieldDescription}</FormDescription>}

                                <FormControl>
                                    <Input
                                        placeholder={placeholder}
                                        type={type}
                                        {...field}
                                        value={
                                            typeof field.value === 'string' || typeof field.value === 'number'
                                                ? field.value
                                                : ''
                                        }
                                    />
                                </FormControl>

                                <FormMessage />
                            </FormItem>
                        )}
                        rules={{required}}
                    />
                );
            }
            case FieldType.TEXTAREA: {
                return (
                    <FormField
                        control={form.control}
                        name={name}
                        render={({field}) => (
                            <FormItem className="space-y-2">
                                <FormLabel>{label}</FormLabel>

                                {fieldDescription && <FormDescription>{fieldDescription}</FormDescription>}

                                <FormControl>
                                    <Textarea
                                        placeholder={placeholder}
                                        {...field}
                                        value={typeof field.value === 'string' ? field.value : ''}
                                    />
                                </FormControl>

                                <FormMessage />
                            </FormItem>
                        )}
                        rules={{required}}
                    />
                );
            }
            case FieldType.SELECT: {
                const options = fieldOptions || [];

                if (multipleChoice) {
                    return (
                        <FormField
                            control={form.control}
                            name={name}
                            render={({field}) => (
                                <FormItem className="space-y-2">
                                    <FormLabel>{label}</FormLabel>

                                    {fieldDescription && <FormDescription>{fieldDescription}</FormDescription>}

                                    <div className="flex flex-col gap-2">
                                        {options.map((opt) => (
                                            <label className="flex items-center gap-2 text-sm" key={opt.value}>
                                                <Checkbox
                                                    checked={
                                                        Array.isArray(field.value)
                                                            ? field.value.includes(opt.value)
                                                            : false
                                                    }
                                                    onCheckedChange={(checked) => {
                                                        const current: string[] = Array.isArray(field.value)
                                                            ? field.value
                                                            : [];
                                                        if (checked) {
                                                            if (
                                                                maxSelection &&
                                                                maxSelection > 0 &&
                                                                current.length >= maxSelection
                                                            ) {
                                                                return;
                                                            }
                                                            field.onChange([...current, opt.value]);
                                                        } else {
                                                            field.onChange(current.filter((v) => v !== opt.value));
                                                        }
                                                    }}
                                                />

                                                <span>{opt.label}</span>
                                            </label>
                                        ))}
                                    </div>

                                    <FormMessage />
                                </FormItem>
                            )}
                            rules={{
                                required: required ? 'Required' : false,
                                validate: (value: unknown) => {
                                    if (!Array.isArray(value)) {
                                        return true;
                                    }

                                    if (minSelection && minSelection > 0 && value.length < minSelection) {
                                        return `Select at least ${minSelection} option${minSelection > 1 ? 's' : ''}`;
                                    }

                                    if (maxSelection && maxSelection > 0 && value.length > maxSelection) {
                                        return `Select at most ${maxSelection} option${maxSelection > 1 ? 's' : ''}`;
                                    }

                                    return true;
                                },
                            }}
                        />
                    );
                }

                return (
                    <FormField
                        control={form.control}
                        name={name}
                        render={({field}) => (
                            <FormItem className="space-y-2">
                                <FormLabel>{label}</FormLabel>

                                {fieldDescription && <FormDescription>{fieldDescription}</FormDescription>}

                                <Select
                                    onValueChange={field.onChange}
                                    value={typeof field.value === 'string' ? field.value : undefined}
                                >
                                    <FormControl>
                                        <SelectTrigger>
                                            <SelectValue placeholder={placeholder || 'Select...'} />
                                        </SelectTrigger>
                                    </FormControl>

                                    <SelectContent>
                                        {options.map((opt) => (
                                            <SelectItem key={opt.value} value={opt.value}>
                                                {opt.label}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>

                                <FormMessage />
                            </FormItem>
                        )}
                        rules={{required}}
                    />
                );
            }
            case FieldType.RADIO: {
                const options = fieldOptions || [];

                return (
                    <FormField
                        control={form.control}
                        name={name}
                        render={({field}) => (
                            <FormItem className="space-y-2">
                                <FormLabel>{label}</FormLabel>

                                {fieldDescription && <FormDescription>{fieldDescription}</FormDescription>}

                                <div className="flex flex-col gap-2">
                                    <RadioGroup
                                        onValueChange={field.onChange}
                                        value={typeof field.value === 'string' ? field.value : undefined}
                                    >
                                        <div className="flex items-center space-x-2">
                                            {options.map((opt) => (
                                                <div className="flex items-center space-x-2" key={opt.value}>
                                                    <RadioGroupItem id={`${name}-${opt.value}`} value={opt.value} />

                                                    <Label htmlFor={`${name}-${opt.value}`}>{opt.label}</Label>
                                                </div>
                                            ))}
                                        </div>
                                    </RadioGroup>
                                </div>

                                <FormMessage />
                            </FormItem>
                        )}
                        rules={{required}}
                    />
                );
            }
            case FieldType.CHECKBOX: {
                return (
                    <FormField
                        control={form.control}
                        name={name}
                        render={({field}) => (
                            <FormItem className="space-y-2">
                                {fieldDescription && <FormDescription>{fieldDescription}</FormDescription>}

                                <div className="flex items-center space-x-2">
                                    <FormControl>
                                        <Checkbox checked={!!field.value} onCheckedChange={field.onChange} />
                                    </FormControl>

                                    <FormLabel className="font-normal">{placeholder || label}</FormLabel>
                                </div>

                                <FormMessage />
                            </FormItem>
                        )}
                        rules={{required}}
                    />
                );
            }
            case FieldType.DATE_PICKER:
            case FieldType.DATETIME_PICKER: {
                return (
                    <FormField
                        control={form.control}
                        name={name}
                        render={({field}) => (
                            <FormItem className="space-y-2">
                                <FormLabel>{label}</FormLabel>

                                {fieldDescription && <FormDescription>{fieldDescription}</FormDescription>}

                                <FormControl>
                                    {fieldType === FieldType.DATE_PICKER ? (
                                        <DatePicker
                                            onChange={(date) => field.onChange(date?.toISOString())}
                                            value={field.value ? new Date(field.value as string) : undefined}
                                        />
                                    ) : (
                                        <DateTimePicker
                                            onChange={(date) => field.onChange(date?.toISOString())}
                                            value={field.value ? new Date(field.value as string) : undefined}
                                        />
                                    )}
                                </FormControl>

                                <FormMessage />
                            </FormItem>
                        )}
                        rules={{required}}
                    />
                );
            }
            case FieldType.FILE_INPUT: {
                return (
                    <Controller
                        control={form.control}
                        name={name}
                        render={({field}) => (
                            <FormItem className="space-y-2">
                                <FormLabel>{label}</FormLabel>

                                {fieldDescription && <FormDescription>{fieldDescription}</FormDescription>}

                                <FormControl>
                                    <div>
                                        <input
                                            onChange={(e) => field.onChange(e.target.files?.[0] || null)}
                                            type="file"
                                        />
                                    </div>
                                </FormControl>

                                <FormMessage />
                            </FormItem>
                        )}
                        rules={{required}}
                    />
                );
            }
            case FieldType.CUSTOM_HTML: {
                return (
                    <div className="space-y-2">
                        {fieldDescription && <FormDescription>{fieldDescription}</FormDescription>}

                        <div
                            className="prose max-w-none"
                            dangerouslySetInnerHTML={{
                                __html: sanitize(defaultValue ?? '', {
                                    allowedAttributes: {
                                        div: ['class'],
                                        table: ['class'],
                                        td: ['class'],
                                        tr: ['class'],
                                    },
                                }),
                            }}
                        />
                    </div>
                );
            }
            default:
                return (
                    <FormField
                        control={form.control}
                        name={name}
                        render={({field}) => (
                            <FormItem className="space-y-2">
                                <FormLabel>{label}</FormLabel>

                                {fieldDescription && <FormDescription>{fieldDescription}</FormDescription>}

                                <FormControl>
                                    <Input
                                        placeholder={placeholder}
                                        {...field}
                                        value={
                                            typeof field.value === 'string' || typeof field.value === 'number'
                                                ? field.value
                                                : ''
                                        }
                                    />
                                </FormControl>

                                <FormMessage />
                            </FormItem>
                        )}
                    />
                );
        }
    };

    return (
        <div className="h-full overflow-auto">
            <div className="mx-auto w-full max-w-2xl p-6">
                {environment !== 'production' && (
                    <div className="mb-4 space-x-1 uppercase">
                        <span>Environment:</span>

                        <span className="font-semibold">{environment}</span>
                    </div>
                )}

                {ui.customFormStyling && <style>{ui.customFormStyling}</style>}

                <div className="mb-6">
                    <h1 className="text-2xl font-semibold tracking-tight">{ui.title}</h1>

                    {ui.subtitle && (
                        <p className="mt-2 whitespace-pre-line text-sm text-muted-foreground">{ui.subtitle}</p>
                    )}
                </div>

                <Form {...form}>
                    <form className="space-y-6" onSubmit={form.handleSubmit(handleSubmit)}>
                        {ui.inputs?.length ? (
                            ui.inputs.map((formInput, idx) => {
                                const name = formInput.fieldName || `field_${idx}`;

                                return <div key={name}>{renderField(name, formInput)}</div>;
                            })
                        ) : (
                            <div className="text-sm text-muted-foreground">No inputs defined.</div>
                        )}

                        <div className="pt-2">
                            <Button disabled={submitting} type="submit">
                                {submitting ? 'Submitting...' : ui.buttonLabel}
                            </Button>
                        </div>
                    </form>
                </Form>

                {ui.appendAttribution && (
                    <div className="mt-8 space-x-1 border-t pt-4 text-center text-xs text-muted-foreground">
                        <span>Powered by</span>

                        <a href="https://www.bytechef.io" rel="noopener noreferrer" target="_blank">
                            ByteChef
                        </a>
                    </div>
                )}
            </div>
        </div>
    );
}
