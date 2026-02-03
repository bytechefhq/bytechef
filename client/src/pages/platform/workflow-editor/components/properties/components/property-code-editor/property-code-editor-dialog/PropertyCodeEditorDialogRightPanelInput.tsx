import {Card, CardContent, CardHeader, CardTitle} from '@/components/ui/card';

type InputValueType = string | number | boolean | null | undefined | InputValueType[] | {[key: string]: InputValueType};

interface InputEntriesProps {
    entries: [string, InputValueType][];
    indent?: number;
}

const formatValue = (value: InputValueType): string => {
    if (value === null) {
        return 'null';
    }

    if (value === undefined) {
        return 'undefined';
    }

    if (typeof value === 'string') {
        let result = value;

        if (result.length > 23) {
            result = result.slice(0, 23) + '...';
        }

        return result;
    }

    if (typeof value === 'number' || typeof value === 'boolean') {
        return String(value);
    }

    if (Array.isArray(value)) {
        return `[${value.length} items]`;
    }

    return '';
};

const isNestedObject = (value: InputValueType): value is {[key: string]: InputValueType} => {
    return typeof value === 'object' && value !== null && !Array.isArray(value);
};

const InputEntries = ({entries, indent = 0}: InputEntriesProps) => {
    return (
        <div className="space-y-1">
            {entries.map(([key, value]) => {
                const hasNestedObject = isNestedObject(value);

                return (
                    <div key={key}>
                        <div className="flex" style={{paddingLeft: `${indent * 12}px`}}>
                            <div className={hasNestedObject ? 'font-medium' : 'w-1/2'}>{key}</div>

                            {!hasNestedObject && <div className="w-1/2 text-foreground/60">{formatValue(value)}</div>}
                        </div>

                        {hasNestedObject && <InputEntries entries={Object.entries(value)} indent={indent + 1} />}
                    </div>
                );
            })}
        </div>
    );
};

const PropertyCodeEditorDialogRightPanelInput = ({input}: {input: {[key: string]: InputValueType}}) => {
    const entries = Object.entries(input) as [string, InputValueType][];

    return (
        <Card className="border-none shadow-none">
            <CardContent className="px-4">
                <CardHeader className="px-0 py-4">
                    <CardTitle>Input</CardTitle>
                </CardHeader>

                {entries.length > 0 ? (
                    <div className="text-sm">
                        <InputEntries entries={entries} />
                    </div>
                ) : (
                    <div>
                        <span className="text-sm text-muted-foreground">No defined entries</span>
                    </div>
                )}
            </CardContent>
        </Card>
    );
};

export default PropertyCodeEditorDialogRightPanelInput;
