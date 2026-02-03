import {Card, CardContent, CardHeader, CardTitle} from '@/components/ui/card';

const PropertyCodeEditorDialogRightPanelInputs = ({input}: {input: {[key: string]: object}}) => {
    const entries = Object.entries(input);

    return (
        <Card className="border-none shadow-none">
            <CardContent className="px-4">
                <CardHeader className="px-0 py-4">
                    <CardTitle>Inputs</CardTitle>
                </CardHeader>

                {entries.length > 0 ? (
                    <div className="space-y-1 text-sm">
                        {entries.map(([key, value]) => {
                            let string = String(value);

                            if (string.length > 23) {
                                string = string.slice(0, 23) + '...';
                            }

                            return (
                                <div className="flex" key={key}>
                                    <div className="w-1/2">{key}</div>

                                    <div className="w-1/2 text-foreground/60">{string}</div>
                                </div>
                            );
                        })}
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

export default PropertyCodeEditorDialogRightPanelInputs;
