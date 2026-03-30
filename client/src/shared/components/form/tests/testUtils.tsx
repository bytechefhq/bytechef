import {Form} from '@/components/ui/form';
import {ReactNode} from 'react';
import {UseFormReturn, useForm} from 'react-hook-form';

type FormRefType = {current: UseFormReturn<Record<string, unknown>> | null};

interface CreateFormWrapperOptionsProps {
    defaultValues?: Record<string, unknown>;
    formRef?: FormRefType;
}

export function createFormWrapper({defaultValues = {}, formRef}: CreateFormWrapperOptionsProps = {}) {
    return function FormWrapper({children}: {children: ReactNode}) {
        const form = useForm<Record<string, unknown>>({
            defaultValues,
            mode: 'onSubmit',
        });

        if (formRef) {
            formRef.current = form;
        }

        return <Form {...form}>{children}</Form>;
    };
}

export function createMockForm(defaultValues: Record<string, unknown> = {}) {
    const formRef: FormRefType = {current: null};

    const wrapper = createFormWrapper({defaultValues, formRef});

    return {
        form: new Proxy({} as UseFormReturn<Record<string, unknown>>, {
            get(_target, prop) {
                if (formRef.current) {
                    const value = (formRef.current as Record<string, unknown>)[prop as string];

                    if (typeof value === 'function') {
                        return value.bind(formRef.current);
                    }

                    return value;
                }

                return undefined;
            },
        }),
        formRef,
        wrapper,
    };
}
