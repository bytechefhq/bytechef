import {FormDescription} from '@/components/ui/form';
import sanitize from 'sanitize-html';

import {FormInputType} from '../util/triggerForm-utils';

interface CustomHTMLFieldRendererProps {
    formInput: Partial<FormInputType>;
}

export const CustomHTMLFieldRenderer = ({formInput}: CustomHTMLFieldRendererProps) => {
    const {defaultValue, fieldDescription} = formInput;

    return (
        <div className="space-y-2">
            {fieldDescription && <FormDescription>{fieldDescription}</FormDescription>}

            <div
                className="prose max-w-none"
                dangerouslySetInnerHTML={{
                    __html: sanitize(defaultValue ?? '', {
                        allowedAttributes: {
                            br: ['class'],
                            div: ['class'],
                            em: ['class'],
                            h1: ['class'],
                            h2: ['class'],
                            h3: ['class'],
                            h4: ['class'],
                            h5: ['class'],
                            h6: ['class'],
                            li: ['class'],
                            ol: ['class'],
                            p: ['class'],
                            span: ['class'],
                            strong: ['class'],
                            ul: ['class'],
                        },
                        allowedTags: [
                            'br',
                            'div',
                            'em',
                            'h1',
                            'h2',
                            'h3',
                            'h4',
                            'h5',
                            'h6',
                            'li',
                            'ol',
                            'p',
                            'span',
                            'strong',
                            'ul',
                        ],
                    }),
                }}
            />
        </div>
    );
};
