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

package com.bytechef.component.notion.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.notion.constant.NotionConstants.CAPTION;
import static com.bytechef.component.notion.constant.NotionConstants.CHECKED;
import static com.bytechef.component.notion.constant.NotionConstants.CHILDREN;
import static com.bytechef.component.notion.constant.NotionConstants.COLOR;
import static com.bytechef.component.notion.constant.NotionConstants.CONTENT;
import static com.bytechef.component.notion.constant.NotionConstants.EXPRESSION;
import static com.bytechef.component.notion.constant.NotionConstants.ID;
import static com.bytechef.component.notion.constant.NotionConstants.LANGUAGE;
import static com.bytechef.component.notion.constant.NotionConstants.RICH_TEXT;
import static com.bytechef.component.notion.constant.NotionConstants.TYPE;
import static com.bytechef.component.notion.constant.NotionConstants.URL;
import static com.bytechef.component.notion.util.NotionBlockType.BOOKMARK;
import static com.bytechef.component.notion.util.NotionBlockType.BREADCRUMB;
import static com.bytechef.component.notion.util.NotionBlockType.BULLETED_LIST_ITEM;
import static com.bytechef.component.notion.util.NotionBlockType.CALLOUT;
import static com.bytechef.component.notion.util.NotionBlockType.CODE;
import static com.bytechef.component.notion.util.NotionBlockType.EMBED;
import static com.bytechef.component.notion.util.NotionBlockType.FILE;
import static com.bytechef.component.notion.util.NotionBlockType.HEADING_1;
import static com.bytechef.component.notion.util.NotionBlockType.HEADING_2;
import static com.bytechef.component.notion.util.NotionBlockType.HEADING_3;
import static com.bytechef.component.notion.util.NotionBlockType.NUMBERED_LIST_ITEM;
import static com.bytechef.component.notion.util.NotionBlockType.PARAGRAPH;
import static com.bytechef.component.notion.util.NotionBlockType.PDF;
import static com.bytechef.component.notion.util.NotionBlockType.QUOTE;
import static com.bytechef.component.notion.util.NotionBlockType.TABLE_OF_CONTENTS;
import static com.bytechef.component.notion.util.NotionBlockType.TOGGLE;
import static com.bytechef.component.notion.util.NotionBlockType.TO_DO;
import static com.bytechef.component.notion.util.NotionBlockType.getPropertyTypeByName;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.notion.util.NotionBlockType;
import com.bytechef.component.notion.util.NotionRichTextType;
import com.bytechef.component.notion.util.NotionUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class NotionAddBlockToPageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("addBlockToPage")
        .title("Add Block to Page")
        .description("Adds a new content block to a page.")
        .properties(
            string(ID)
                .label("Parent Page ID")
                .options((OptionsFunction<String>) NotionUtils::getPageIdOptions)
                .required(true),
            array(CHILDREN)
                .label("Children")
                .maxItems(100)
                .description("Child content to append to a container block as an array of block objects.")
                .items(
                    object()
                        .properties(
                            string(TYPE)
                                .label("Block Type")
                                .description("The type of block to add.")
                                .options(
                                    option("Bookmark", BOOKMARK.getName()),
                                    option("Breadcrumb", BREADCRUMB.getName()),
                                    option("Bulleted List Item", BULLETED_LIST_ITEM.getName()),
                                    option("Callout", CALLOUT.getName()),
                                    option("Code", CODE.getName()),
                                    option("Embed", EMBED.getName(),
                                        "Embed block objects include information about another website " +
                                            "displayed within the Notion UI."),
                                    option("Equation", NotionBlockType.EQUATION.getName()),
                                    option("Heading 1", HEADING_1.getName()),
                                    option("Heading 2", HEADING_2.getName()),
                                    option("Heading 3", HEADING_3.getName()),
                                    option("Numbered List Item", NUMBERED_LIST_ITEM.getName()),
                                    option("Paragraph", PARAGRAPH.getName()),
                                    option("Quote", QUOTE.getName()),
                                    option("Table of Contents", TABLE_OF_CONTENTS.getName()),
                                    option("To Do", TO_DO.getName()),
                                    option("Toggle", TOGGLE.getName()))
                                .required(true),
                            array(CAPTION)
                                .label("Caption")
                                .description("The caption for the bookmark.")
                                .displayCondition(
                                    "%s == '%s'".formatted(CHILDREN + "[index]." + TYPE, BOOKMARK.getName()))
                                .items(
                                    object()
                                        .properties(
                                            string(TYPE)
                                                .options(
                                                    option("Text", NotionRichTextType.TEXT.getName()),
                                                    option("Equation", NotionRichTextType.EQUATION.getName()))
                                                .defaultValue(NotionRichTextType.TEXT.getName())
                                                .required(true),
                                            object(NotionRichTextType.TEXT.getName())
                                                .label("Text")
                                                .displayCondition("%s == '%s'".formatted(
                                                    CHILDREN + "[index]." + CAPTION + "[index]." + TYPE,
                                                    NotionRichTextType.TEXT.getName()))
                                                .properties(
                                                    string(CONTENT)
                                                        .label("Content")
                                                        .description("The actual text content of the text.")
                                                        .required(true))
                                                .required(false),
                                            object(NotionRichTextType.EQUATION.getName())
                                                .label("Equation")
                                                .displayCondition("%s == '%s'".formatted(
                                                    CHILDREN + "[index]." + CAPTION + "[index]." + TYPE,
                                                    NotionRichTextType.EQUATION.getName()))
                                                .properties(
                                                    string(EXPRESSION)
                                                        .label("Expression")
                                                        .description(
                                                            "The LaTeX string representing the inline equation.")
                                                        .required(true))
                                                .required(true)))
                                .required(false),
                            string(URL)
                                .label("Url")
                                .description("The link for the bookmark.")
                                .displayCondition(
                                    "%s == '%s'".formatted(CHILDREN + "[index]." + TYPE, BOOKMARK.getName()))
                                .required(true),
                            string(URL)
                                .label("Url")
                                .description("The link to the website that the embed block displays.")
                                .displayCondition(
                                    "%s == '%s'".formatted(CHILDREN + "[index]." + TYPE, EMBED.getName()))
                                .required(true),
                            string(EXPRESSION)
                                .label("Expression")
                                .description("A KaTeX compatible string.")
                                .displayCondition("%s == '%s'".formatted(CHILDREN + "[index]." + TYPE,
                                    NotionBlockType.EQUATION.getName()))
                                .required(true),
                            bool(CHECKED)
                                .label("Checked")
                                .description("Whether the To do is checked.")
                                .displayCondition(
                                    "%s == '%s'".formatted(CHILDREN + "[index]." + TYPE, TO_DO.getName()))
                                .required(false),
                            string(COLOR)
                                .label("Color")
                                .description("The color of the block.")
                                .displayCondition(
                                    "%s == '%s' || %s == '%s' || %s == '%s' || %s == '%s' || %s == '%s'".formatted(
                                        CHILDREN + "[index]." + TYPE, BULLETED_LIST_ITEM.getName(),
                                        CHILDREN + "[index]." + TYPE, CALLOUT.getName(),
                                        CHILDREN + "[index]." + TYPE, TO_DO.getName(),
                                        CHILDREN + "[index]." + TYPE, TABLE_OF_CONTENTS.getName(),
                                        CHILDREN + "[index]." + TYPE, TOGGLE.getName()))
                                .options(
                                    option("Blue", "blue"),
                                    option("Blue Background", "blue_background"),
                                    option("Brown", "brown"),
                                    option("Brown Background", "brown_background"),
                                    option("Default", "default"),
                                    option("Gray", "gray"),
                                    option("Gray Background", "gray_background"),
                                    option("Green", "green"),
                                    option("Green Background", "green_background"),
                                    option("Orange", "orange"),
                                    option("Orange Background", "orange_background"),
                                    option("Yellow", "yellow"),
                                    option("Green", "green"),
                                    option("Pink", "pink"),
                                    option("Pink Background", "pink_background"),
                                    option("Purple", "purple"),
                                    option("Purple Background", "purple_background"),
                                    option("Red", "red"),
                                    option("Red Background", "red_background"),
                                    option("Yellow Background", "yellow_background"))
                                .defaultValue("default")
                                .required(false),
                            array(RICH_TEXT)
                                .label("Rich Text")
                                .description("The rich text in the block.")
                                .maxItems(1)
                                .displayCondition(
                                    ("%s == '%s' || %s == '%s' || %s == '%s' || %s == '%s' || %s == '%s' || " +
                                        "%s == '%s' || %s == '%s' || %s == '%s' || %s == '%s' || %s == '%s' || " +
                                        "%s == '%s'")
                                            .formatted(
                                                CHILDREN + "[index]." + TYPE, BULLETED_LIST_ITEM.getName(),
                                                CHILDREN + "[index]." + TYPE, CALLOUT.getName(),
                                                CHILDREN + "[index]." + TYPE, CODE.getName(),
                                                CHILDREN + "[index]." + TYPE, HEADING_1.getName(),
                                                CHILDREN + "[index]." + TYPE, HEADING_2.getName(),
                                                CHILDREN + "[index]." + TYPE, HEADING_3.getName(),
                                                CHILDREN + "[index]." + TYPE, NUMBERED_LIST_ITEM.getName(),
                                                CHILDREN + "[index]." + TYPE, PARAGRAPH.getName(),
                                                CHILDREN + "[index]." + TYPE, QUOTE.getName(),
                                                CHILDREN + "[index]." + TYPE, TO_DO.getName(),
                                                CHILDREN + "[index]." + TYPE, TOGGLE.getName()))
                                .items(
                                    object()
                                        .properties(
                                            string(TYPE)
                                                .options(
                                                    option("Text", NotionRichTextType.TEXT.getName()),
                                                    option("Equation", NotionRichTextType.EQUATION.getName()))
                                                .defaultValue(NotionRichTextType.TEXT.getName())
                                                .required(true),
                                            object(NotionRichTextType.TEXT.getName())
                                                .label("Text")
                                                .displayCondition("%s == '%s'".formatted(
                                                    CHILDREN + "[index]." + RICH_TEXT + "[index]." + TYPE,
                                                    NotionRichTextType.TEXT.getName()))
                                                .properties(
                                                    string(CONTENT)
                                                        .label("Content")
                                                        .description("The actual text content of the text.")
                                                        .required(true))
                                                .required(false),
                                            object(NotionRichTextType.EQUATION.getName())
                                                .label("Equation")
                                                .displayCondition("%s == '%s'".formatted(
                                                    CHILDREN + "[index]." + RICH_TEXT + "[index]." + TYPE,
                                                    NotionRichTextType.EQUATION.getName()))
                                                .properties(
                                                    string(EXPRESSION)
                                                        .label("Expression")
                                                        .description(
                                                            "The LaTeX string representing the inline equation.")
                                                        .required(true))
                                                .required(true)))
                                .required(true),
                            string(LANGUAGE)
                                .label("Language")
                                .displayCondition(
                                    "%s == '%s'".formatted(CHILDREN + "[index]." + TYPE, CODE.getName()))
                                .options(
                                    option("abap", "abap"),
                                    option("arduino", "arduino"),
                                    option("bash", "bash"),
                                    option("basic", "basic"),
                                    option("c", "c"),
                                    option("clojure", "clojure"),
                                    option("coffeescript", "coffeescript"),
                                    option("c++", "c++"),
                                    option("c#", "c#"),
                                    option("css", "css"),
                                    option("dart", "dart"),
                                    option("diff", "diff"),
                                    option("docker", "docker"),
                                    option("elixir", "elixir"),
                                    option("elm", "elm"),
                                    option("erlang", "erlang"),
                                    option("flow", "flow"),
                                    option("fortran", "fortran"),
                                    option("f#", "f#"),
                                    option("gherkin", "gherkin"),
                                    option("glsl", "glsl"),
                                    option("go", "go"),
                                    option("graphql", "graphql"),
                                    option("groovy", "groovy"),
                                    option("haskell", "haskell"),
                                    option("html", "html"),
                                    option("java", "java"),
                                    option("javascript", "javascript"),
                                    option("json", "json"),
                                    option("julia", "julia"),
                                    option("kotlin", "kotlin"),
                                    option("latex", "latex"),
                                    option("less", "less"),
                                    option("lisp", "lisp"),
                                    option("livescript", "livescript"),
                                    option("lua", "lua"),
                                    option("makefile", "makefile"),
                                    option("markdown", "markdown"),
                                    option("markup", "markup"),
                                    option("matlab", "matlab"),
                                    option("mermaid", "mermaid"),
                                    option("nix", "nix"),
                                    option("objective-c", "objective-c"),
                                    option("ocaml", "ocaml"),
                                    option("pascal", "pascal"),
                                    option("perl", "perl"),
                                    option("php", "php"),
                                    option("plain text", "plain text"),
                                    option("powershell", "powershell"),
                                    option("prolog", "prolog"),
                                    option("protobuf", "protobuf"),
                                    option("python", "python"),
                                    option("r", "r"),
                                    option("reason", "reason"),
                                    option("ruby", "ruby"),
                                    option("rust", "rust"),
                                    option("sass", "sass"),
                                    option("scala", "scala"),
                                    option("scheme", "scheme"),
                                    option("scss", "scss"),
                                    option("shell", "shell"),
                                    option("sql", "sql"),
                                    option("swift", "swift"),
                                    option("typescript", "typescript"),
                                    option("vb.net", "vb.net"),
                                    option("verilog", "verilog"),
                                    option("vhdl", "vhdl"),
                                    option("visual basic", "visual basic"),
                                    option("webassembly", "webassembly"),
                                    option("xml", "xml"),
                                    option("yaml", "yaml"),
                                    option("java/c/c++/c#", "java/c/c++/c#"))
                                .required(true)))
                .required(true))
        .output()
        .perform(NotionAddBlockToPageAction::perform);

    private NotionAddBlockToPageAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        List<Map<String, ?>> childrenBlocks = new ArrayList<>();
        List<Map<String, ?>> children = inputParameters.getRequiredList(CHILDREN, new TypeReference<>() {});

        for (Map<String, ?> child : children) {
            String blockType = (String) child.get(TYPE);
            Map<String, Object> childrenMap = new HashMap<>();
            childrenMap.put("object", "block");
            childrenMap.put(TYPE, blockType);

            NotionBlockType blockTypeEnum = getPropertyTypeByName(blockType);
            switch (blockTypeEnum) {
                case BOOKMARK -> {
                    Map<String, Object> bookmarkMap = new HashMap<>();
                    bookmarkMap.put(URL, child.get(URL));

                    if (child.get(CAPTION) != null) {
                        bookmarkMap.put(CAPTION, child.get(CAPTION));
                    }
                    childrenMap.put(BOOKMARK.getName(), bookmarkMap);
                }
                case BREADCRUMB -> childrenMap.put(BREADCRUMB.getName(), Map.of());
                case BULLETED_LIST_ITEM -> {
                    Map<String, Object> bulletMap = new HashMap<>();

                    bulletMap.put(RICH_TEXT, child.get(RICH_TEXT));

                    if (child.get(COLOR) != null) {
                        bulletMap.put(COLOR, child.get(COLOR));
                    }

                    childrenMap.put(BULLETED_LIST_ITEM.getName(), bulletMap);
                }
                case CALLOUT -> {
                    Map<String, Object> calloutMap = new HashMap<>();

                    calloutMap.put(RICH_TEXT, child.get(RICH_TEXT));

                    if (child.get(COLOR) != null) {
                        calloutMap.put(COLOR, child.get(COLOR));
                    }

                    childrenMap.put(CALLOUT.getName(), calloutMap);
                }
                case CODE -> childrenMap.put(
                    CODE.getName(), Map.of(RICH_TEXT, child.get(RICH_TEXT), LANGUAGE, child.get(LANGUAGE)));
                case EMBED -> childrenMap.put(EMBED.getName(), Map.of(URL, child.get(URL)));
                case EQUATION -> childrenMap.put(
                    NotionBlockType.EQUATION.getName(), Map.of(EXPRESSION, child.get(EXPRESSION)));
                case FILE -> childrenMap.put(FILE.getName(), Map.of());
                case HEADING_1 -> childrenMap.put(HEADING_1.getName(), Map.of(RICH_TEXT, child.get(RICH_TEXT)));
                case HEADING_2 -> childrenMap.put(HEADING_2.getName(), Map.of(RICH_TEXT, child.get(RICH_TEXT)));
                case HEADING_3 -> childrenMap.put(HEADING_3.getName(), Map.of(RICH_TEXT, child.get(RICH_TEXT)));
                case NUMBERED_LIST_ITEM -> childrenMap.put(NUMBERED_LIST_ITEM.getName(),
                    Map.of(RICH_TEXT, child.get(RICH_TEXT)));
                case PARAGRAPH -> childrenMap.put(PARAGRAPH.getName(), Map.of(RICH_TEXT, child.get(RICH_TEXT)));
                case PDF -> childrenMap.put(PDF.getName(), Map.of());
                case QUOTE -> childrenMap.put(QUOTE.getName(), Map.of(RICH_TEXT, child.get(RICH_TEXT)));
                case TABLE_OF_CONTENTS -> childrenMap.put(TABLE_OF_CONTENTS.getName(), Map.of(COLOR, child.get(COLOR)));
                case TO_DO -> {
                    Map<String, Object> todoMap = new HashMap<>();

                    todoMap.put(RICH_TEXT, child.get(RICH_TEXT));

                    if (child.get(CHECKED) != null) {
                        todoMap.put(CHECKED, child.get(CHECKED));
                    }

                    if (child.get(COLOR) != null) {
                        todoMap.put(COLOR, child.get(COLOR));
                    }

                    childrenMap.put(TO_DO.getName(), todoMap);
                }
                case TOGGLE -> childrenMap.put(TOGGLE.getName(), Map.of(RICH_TEXT, child.get(RICH_TEXT)));
                default -> throw new IllegalArgumentException("Unknown block type: " + blockTypeEnum);
            }

            childrenBlocks.add(childrenMap);
        }

        return context.http(http -> http.patch("/blocks/%s/children".formatted(inputParameters.getRequiredString(ID))))
            .body(Http.Body.of(Map.of(CHILDREN, childrenBlocks)))
            .configuration(Http.responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
