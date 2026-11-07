import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface ResponseSchemaNoteStateI {
    responseSchemaNoteDismissed: boolean;
    setResponseSchemaNoteDismissed: (responseSchemaNoteDismissed: boolean) => void;
}

export const useResponseSchemaNoteStore = create<ResponseSchemaNoteStateI>()(
    devtools(
        persist(
            (set) => ({
                responseSchemaNoteDismissed: false,
                setResponseSchemaNoteDismissed: (responseSchemaNoteDismissed) => set({responseSchemaNoteDismissed}),
            }),
            {
                name: 'bytechef.response-schema-note',
            }
        )
    )
);
