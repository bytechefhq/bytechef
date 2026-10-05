import {Input} from '@/components/Input/Input';
import {FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {McpServer} from '@/shared/middleware/graphql';
import {Control, FieldValues, Path} from 'react-hook-form';

interface McpServerFormFieldProps<TFieldValuesType extends FieldValues> {
    control: Control<TFieldValuesType>;
    mcpServer?: McpServer;
    name: Path<TFieldValuesType>;
}

const McpServerFormField = <TFieldValuesType extends FieldValues>({
    control,
    mcpServer,
    name,
}: McpServerFormFieldProps<TFieldValuesType>) => (
    <FormField
        control={control}
        name={name}
        render={({field}) => (
            <FormItem>
                <FormLabel>MCP Server</FormLabel>

                <FormControl>
                    <Input
                        {...field}
                        disabled={!!mcpServer}
                        placeholder={mcpServer ? mcpServer.name : 'Select MCP Server'}
                        value={mcpServer ? mcpServer.name : field.value}
                    />
                </FormControl>

                <FormMessage />
            </FormItem>
        )}
    />
);

export default McpServerFormField;
