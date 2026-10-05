import {FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import {Suspense, lazy} from 'react';
import {Control, ControllerProps, FieldValues, Path} from 'react-hook-form';

const MonacoEditorWrapper = lazy(() => import('@/shared/components/MonacoEditorWrapper'));

const editorOptions = {
    automaticLayout: true,
    folding: true,
    fontSize: 12,
    lineNumbers: 'on' as const,
    minimap: {enabled: false},
    scrollBeyondLastLine: false,
    tabSize: 2,
    wordWrap: 'on' as const,
};

interface JsonSchemaFormFieldProps<TFieldValuesType extends FieldValues, TNameType extends Path<TFieldValuesType>> {
    control: Control<TFieldValuesType>;
    label: string;
    name: TNameType;
    rules?: ControllerProps<TFieldValuesType, TNameType>['rules'];
}

const JsonSchemaFormField = <TFieldValuesType extends FieldValues, TNameType extends Path<TFieldValuesType>>({
    control,
    label,
    name,
    rules,
}: JsonSchemaFormFieldProps<TFieldValuesType, TNameType>) => (
    <FormField
        control={control}
        name={name}
        render={({field}) => (
            <FormItem>
                <FormLabel>{label}</FormLabel>

                <FormControl>
                    <div className="h-48 overflow-hidden rounded-md border">
                        <Suspense fallback={<MonacoEditorLoader />}>
                            <MonacoEditorWrapper
                                defaultLanguage="json"
                                onChange={(value) => field.onChange(value || '')}
                                onMount={() => {}}
                                options={editorOptions}
                                value={field.value}
                            />
                        </Suspense>
                    </div>
                </FormControl>

                <FormMessage />
            </FormItem>
        )}
        rules={rules}
    />
);

export default JsonSchemaFormField;
