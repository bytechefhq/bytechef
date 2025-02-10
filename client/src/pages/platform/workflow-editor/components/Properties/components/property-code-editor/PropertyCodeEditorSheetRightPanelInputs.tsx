import {Card, CardContent, CardHeader, CardTitle} from '@/components/ui/card';

const PropertyCodeEditorSheetRightPanelInputs = ({input}: {input: {[key: string]: object}}) => {
    return (
        <Card className="border-none shadow-none">
            <CardContent className="px-4">
                <CardHeader className="px-0 py-4">
                    <CardTitle>Inputs</CardTitle>
                </CardHeader>

                <div className="space-y-1 text-sm">
                    {input &&
                        Object.entries(input).map(([key, value]) => {
                            let string = value.toString();

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
            </CardContent>
        </Card>
    );
};

export default PropertyCodeEditorSheetRightPanelInputs;
