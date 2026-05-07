/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.canva.action;

import static com.bytechef.component.canva.constant.CanvaConstants.AS_SINGLE_IMAGE;
import static com.bytechef.component.canva.constant.CanvaConstants.DESIGN_ID;
import static com.bytechef.component.canva.constant.CanvaConstants.EXPORT_QUALITY;
import static com.bytechef.component.canva.constant.CanvaConstants.FORMAT;
import static com.bytechef.component.canva.constant.CanvaConstants.HEIGHT;
import static com.bytechef.component.canva.constant.CanvaConstants.LOSSLESS;
import static com.bytechef.component.canva.constant.CanvaConstants.PAGES;
import static com.bytechef.component.canva.constant.CanvaConstants.QUALITY;
import static com.bytechef.component.canva.constant.CanvaConstants.SIZE;
import static com.bytechef.component.canva.constant.CanvaConstants.TRANSPARENT_BACKGROUND;
import static com.bytechef.component.canva.constant.CanvaConstants.VIDEO_QUALITY;
import static com.bytechef.component.canva.constant.CanvaConstants.WIDTH;
import static com.bytechef.component.canva.util.CanvaUtils.pollJob;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ivona Pavela
 */
public class CanvaExportDesignAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("exportDesign")
        .title("Export Design")
        .description("Get the status and results of an export job, including link(s) to the downloadable file(s).")
        .help("", "https://docs.bytechef.io/reference/components/canva_v1#export-design")
        .properties(
            string(DESIGN_ID)
                .label("Design ID")
                .description("The design ID.")
                .required(true),
            object(FORMAT)
                .label("Format")
                .description("Details about the desired export format.")
                .properties(
                    string("type")
                        .label("Type")
                        .options(
                            option("pdf", "pdf", "Export the design as a PDF."),
                            option("jpg", "jpg", "Export the design as a JPEG."),
                            option("png", "png", "Export the design as a PNG."),
                            option("pptx", "pptx", "Export the design as a PPTX."),
                            option("gif", "gif", "Export the design as a GIF."),
                            option("mp4", "mp4", "Export the design as an MP4."),
                            option("html_bundle", "html_bundle", "Export the email design as an HTML bundle."),
                            option("html_standalone", "html_standalone",
                                "Export the email design as a standalone HTML file with hosted assets."))
                        .required(true),
                    integer(QUALITY)
                        .label("Quality")
                        .description(
                            "The quality of the exported JPEG that determines how compressed the exported file should be.")
                        .minValue(1)
                        .maxValue(100)
                        .displayCondition("%s == '%s'".formatted("format.type", "jpg"))
                        .required(true),
                    string(VIDEO_QUALITY)
                        .label("Video quality")
                        .description("The orientation and resolution of the exported video.")
                        .options(
                            option("horizontal_480p", "horizontal_480p"),
                            option("horizontal_720p", "horizontal_720p"),
                            option("horizontal_1080p", "horizontal_1080p"),
                            option("horizontal_4k", "horizontal_4k"),
                            option("vertical_480p", "vertical_480p"),
                            option("vertical_720p", "vertical_720p"),
                            option("vertical_1080p", "vertical_1080p"),
                            option("vertical_4k", "vertical_4k"))
                        .displayCondition("%s == '%s'".formatted("format.type", "mp4"))
                        .required(true),
                    string(EXPORT_QUALITY)
                        .label("Export quality")
                        .description("Specifies the export quality of the design.")
                        .options(
                            option("Regular", "regular"),
                            option("Pro", "pro"))
                        .displayCondition("%s != '%s' || %s != '%s' || %s != '%s'".formatted("format.type", "pptx",
                            "format.type", "html_bundle", "format.type", "html_standalone"))
                        .required(false),
                    integer(WIDTH)
                        .label("Width")
                        .description("Specify the width in pixels of the exported image.")
                        .minValue(40)
                        .maxValue(25000)
                        .displayCondition("%s == '%s' || %s == '%s' || %s == '%s' ".formatted("format.type", "jpg",
                            "format.type", "png", "format.type", "gif"))
                        .required(false),
                    integer(HEIGHT)
                        .label("Height")
                        .description("Specify the height in pixels of the exported image.")
                        .minValue(40)
                        .maxValue(25000)
                        .displayCondition("%s == '%s' || %s == '%s' || %s == '%s' ".formatted("format.type", "jpg",
                            "format.type", "png", "format.type", "gif"))
                        .required(false),
                    string(SIZE)
                        .label("Size")
                        .description("The paper size of the export PDF file.")
                        .options(
                            option("a4", "a4"),
                            option("a3", "a3"),
                            option("letter", "letter"),
                            option("legal", "legal"))
                        .displayCondition("%s == '%s'".formatted("format.type", "pdf"))
                        .required(false),
                    bool(LOSSLESS)
                        .label("Lossless")
                        .description("If set to true (default), the PNG is exported without compression.")
                        .displayCondition("%s == '%s'".formatted("format.type", "png"))
                        .required(false),
                    bool(TRANSPARENT_BACKGROUND)
                        .label("Transparent background")
                        .description("If set to true, the PNG is exported with a transparent background.")
                        .displayCondition("%s == '%s'".formatted("format.type", "png"))
                        .required(false),
                    bool(AS_SINGLE_IMAGE)
                        .label("As single image")
                        .description("When true, multi-page designs are merged into a single image.")
                        .displayCondition("%s == '%s'".formatted("format.type", "png"))
                        .required(false),
                    array(PAGES)
                        .label("Pages")
                        .description("To specify which pages to export in a multi-page design.")
                        .items(integer())
                        .required(false))
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("id")
                            .description("The export job ID."),
                        string("status")
                            .description("The export status of the job."),
                        array("URLs")
                            .description("Download URL(s) for the completed export job. ")
                            .items(string()))))
        .perform(CanvaExportDesignAction::perform);

    public static Map<String, Object>
        perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        Object jobObj = getJob(inputParameters, context);

        if (!(jobObj instanceof Map<?, ?> jobMap)) {
            throw new IllegalStateException("Invalid response: 'job' is missing or not an object");
        }

        String id = jobMap.get("id")
            .toString();

        return pollJob(context, "/exports/" + id, "status", 10, 500);

    }

    private static Object getJob(Parameters inputParameters, Context context) {

        Object formatObj = inputParameters.get(FORMAT);

        Map<String, Object> format = new HashMap<>();
        ((Map<?, ?>) formatObj).forEach((k, v) -> format.put(String.valueOf(k), v));

        if (format.containsKey(VIDEO_QUALITY)) {
            format.put(QUALITY, format.remove(VIDEO_QUALITY));
        }

        Map<String, Object> response = context
            .http(http -> http.post("/exports"))
            .body(Http.Body.of(Map.of(
                DESIGN_ID, inputParameters.getRequiredString(DESIGN_ID),
                FORMAT, format)))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return response.get("job");
    }
}
