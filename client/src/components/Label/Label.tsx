import {Label as LabelPrimitive} from '@radix-ui/react-label';

interface LabelProps {
    htmlFor: string;
    value: string;
}

const Label = ({htmlFor, value}: LabelProps) => (
    <LabelPrimitive
        htmlFor={htmlFor}
        className="ml-3 select-none text-sm font-medium text-gray-900 dark:text-gray-100"
    >
        {value}
    </LabelPrimitive>
);

export default Label;
