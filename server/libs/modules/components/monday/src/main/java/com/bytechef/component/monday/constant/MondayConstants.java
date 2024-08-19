/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.monday.constant;

import java.util.List;

/**
 * @author Monika Kušter
 */
public class MondayConstants {

    public static final String BOARD_ID = "board_id";
    public static final String BOARDS = "boards";
    public static final String COLUMN_TYPE = "column_type";
    public static final String CREATE_COLUMN = "createColumn";
    public static final String CREATE_GROUP = "createGroup";
    public static final String CREATE_ITEM = "createItem";
    public static final String DATA = "data";
    public static final String END_DATE = "endDate";
    public static final String FROM = "from";
    public static final String GROUP_ID = "group_id";
    public static final String GROUP_NAME = "group_name";
    public static final String ID = "id";
    public static final String ITEM_NAME = "item_name";
    public static final String LABELS = "labels";
    public static final String MONDAY = "monday";
    public static final String NAME = "name";
    public static final String START_DATE = "startDate";
    public static final String TEXT = "text";
    public static final String TITLE = "title";
    public static final String TO = "to";
    public static final String TYPE = "type";
    public static final String VALUE = "value";
    public static final String WORKSPACE_ID = "workspace_id";

    public static final List<MondayColumnType> NOT_WRITABLE_COLUMN_TYPES = List.of(
        MondayColumnType.UNSUPPORTED,
        MondayColumnType.AUTO_NUMBER,
        MondayColumnType.NAME,
        MondayColumnType.COLOR_PICKER,
        MondayColumnType.BUTTON,
        MondayColumnType.MIRROR,
        MondayColumnType.SUBTASKS,
        MondayColumnType.ITEM_ID,
        MondayColumnType.CREATION_LOG,
        MondayColumnType.FILE,
        MondayColumnType.FORMULA,
        MondayColumnType.DOC,
        MondayColumnType.LAST_UPDATE,
        MondayColumnType.PROGRESS,
        MondayColumnType.TAGS,
        MondayColumnType.TIME_TRACKING,
        MondayColumnType.VOTE);

    private MondayConstants() {
    }
}
