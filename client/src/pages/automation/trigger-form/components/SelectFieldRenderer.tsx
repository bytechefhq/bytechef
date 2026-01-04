import {Checkbox} from '@/components/ui/checkbox';
import {FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {TriggerFormInput} from '@/shared/middleware/platform/configuration';
import {UseFormReturn} from 'react-hook-form';

interface SelectFieldRendererProps {
    form: UseFormReturn<Record<string, unknown>>;
    formInput: Partial<TriggerFormInput>;
    name: string;
}

export const SelectFieldRenderer = ({form, formInput, name}: SelectFieldRendererProps) => {
    const {
        fieldDescription,
        fieldLabel,
        fieldName,
        fieldOptions,
        maxSelection,
        minSelection,
        multipleChoice,
        placeholder,
        required,
    } = formInput;

    const options = fieldOptions || [];
    const label = fieldLabel || fieldName || name;

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
                                        checked={Array.isArray(field.value) ? field.value.includes(opt.value) : false}
                                        onCheckedChange={(checked) => {
                                            const current: string[] = Array.isArray(field.value) ? field.value : [];
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
                                <SelectItem key={opt.value} value={opt.value as string}>
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
};
