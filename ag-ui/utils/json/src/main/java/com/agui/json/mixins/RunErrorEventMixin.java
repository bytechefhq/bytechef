package com.agui.json.mixins;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Jackson mixin for {@link com.agui.core.event.RunErrorEvent} that aliases the Java field name
 * {@code error} to the on-the-wire name {@code message} required by the AG-UI protocol.
 *
 * <p>
 * The TypeScript reference implementation's {@code RunErrorEventSchema} ({@code @ag-ui/core})
 * declares this field as {@code message: z.string()} (required). Without this rename the Java
 * server emits {@code {"type":"RUN_ERROR","error":"…"}}, the browser-side Zod validator rejects
 * the event with a ZodError ({@code path: ["message"], message: "Required"}), and any real
 * agent failure surfaces in the client as a meta-failure to deserialize the failure event —
 * with the original error text dropped on the floor.
 * </p>
 *
 * <p>
 * Wired up in {@link com.agui.json.ObjectMapperFactory#createModule()} alongside the other
 * mixins. Kept in this module (not the {@code packages/core} module) so {@code core} stays
 * free of any Jackson annotation dependency.
 * </p>
 *
 * <p>
 * The {@code @JsonAlias("error")} keeps any existing producer or archived payload that wrote
 * the legacy {@code error} key parseable, so this is a non-breaking serialization fix on the
 * read path — the rename only changes what the Java server writes.
 * </p>
 */
public interface RunErrorEventMixin {

    @JsonProperty("message")
    @JsonAlias("error")
    String getError();
}
