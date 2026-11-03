import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import {FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {PlusIcon, Trash2Icon} from 'lucide-react';
import {Control, useFieldArray} from 'react-hook-form';

import {MASKED_VALUE, MAX_WEBHOOK_HEADERS, NotificationFormValuesType} from '../hooks/useNotifications';

interface WebhookHeadersFieldProps {
    control: Control<NotificationFormValuesType, unknown, NotificationFormValuesType>;
}

interface WebhookHeadersFormValuesI {
    settings: {webhookHeaders: Array<{name: string; value: string}>};
}

const WebhookHeadersField = ({control}: WebhookHeadersFieldProps) => {
    const {append, fields, remove} = useFieldArray({
        control: control as unknown as Control<WebhookHeadersFormValuesI>,
        name: 'settings.webhookHeaders',
    });

    return (
        <fieldset className="space-y-2 border-0 p-0">
            <legend className="text-sm font-medium">Headers</legend>

            {fields.map((item, index) => (
                <div className="flex items-start gap-2" key={item.id}>
                    <FormField
                        control={control}
                        name={`settings.webhookHeaders.${index}.name`}
                        render={({field}) => (
                            <FormItem className="flex-1">
                                <FormLabel className="sr-only">Header name</FormLabel>

                                <FormControl>
                                    <Input {...field} aria-label="Header name" placeholder="Header name" />
                                </FormControl>

                                <FormMessage />
                            </FormItem>
                        )}
                    />

                    <FormField
                        control={control}
                        name={`settings.webhookHeaders.${index}.value`}
                        render={({field}) => (
                            <FormItem className="flex-1">
                                <FormLabel className="sr-only">Header value</FormLabel>

                                <FormControl>
                                    <Input
                                        aria-label="Header value"
                                        autoComplete="off"
                                        onChange={(event) => field.onChange(event.target.value)}
                                        placeholder={
                                            field.value === MASKED_VALUE
                                                ? 'Saved, leave unchanged to keep'
                                                : 'Header value'
                                        }
                                        type="password"
                                        value={field.value === MASKED_VALUE ? '' : String(field.value || '')}
                                    />
                                </FormControl>

                                <FormMessage />
                            </FormItem>
                        )}
                    />

                    <Button
                        aria-label="Remove header"
                        icon={<Trash2Icon />}
                        onClick={() => remove(index)}
                        type="button"
                        variant="ghost"
                    />
                </div>
            ))}

            <Button
                disabled={fields.length >= MAX_WEBHOOK_HEADERS}
                icon={<PlusIcon />}
                label="Add header"
                onClick={() => append({name: '', value: ''})}
                type="button"
                variant="outline"
            />
        </fieldset>
    );
};

export default WebhookHeadersField;
