import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface ResponseSchemaNoteStateI {
    showResponseSchemaNote: boolean;
    setShowResponseSchemaNote: (showResponseSchemaNote: boolean) => void;
}

export const useResponseSchemaNoteStore = create<ResponseSchemaNoteStateI>()(
    devtools(
        persist(
            (set) => ({
                setShowResponseSchemaNote: (showResponseSchemaNote) =>
                    set(() => ({
                        showResponseSchemaNote,
                    })),
                showResponseSchemaNote: true,
            }),
            {
                name: 'bytechef.response-schema-note',
            }
        ),
        {
            name: 'response-schema-note',
        }
    )
);
