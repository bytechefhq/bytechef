/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.task.handler.spreadsheet.file;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONParser;

public class SpreadsheetFileTaskDescriptorTest {

    private final ObjectMapper objectMapper = new ObjectMapper() {
        {
            setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
    };

    @Test
    public void testSpreadsheetFileTaskDescription() throws JsonProcessingException {
        JSONAssert.assertEquals(
            """
            {
                "description":"Reads and writes data from a spreadsheet file",
                "displayName":"Spreadsheet File",
                "name":"spreadsheetFile",
                "properties":[
                    {
                        "defaultValue":"read",
                        "description":"The operation to perform.",
                        "displayName":"Operation",
                        "name":"operation",
                        "required":true,
                        "type":"SELECT",
                        "options":[
                            {
                                "name":"Read to file",
                                "value":"READ"
                            },
                            {
                                "name":"Write from file",
                                "value":"WRITE"
                            }
                        ]
                    },
                    {
                        "defaultValue":"data",
                        "description":"Name of the binary property from which to read the binary data of the spreadsheet file.",
                        "displayName":"Binary Property",
                        "displayOption":{
                            "show":{
                                "operation":["READ"]
                            }
                        },
                        "name":"binaryPropertyName",
                        "type":"STRING"
                    },
                    {
                        "defaultValue":"CSV",
                        "description":"The format of the file to save the data.",
                        "displayName":"FileFormat",
                        "displayOption":{
                            "show":{
                                "operation":["WRITE"]
                            }
                        },
                        "name":"fileFormat",
                        "type":"SELECT",
                        "options":[
                            {
                                "name":"CSV",
                                "value":"CSV",
                                "description":"Comma-separated value"
                            },
                            {
                                "name":"HTML",
                                "value":"HTML",
                                "description":"HTML Table"
                            },
                            {
                                "name":"ODS",
                                "value":"ODS",
                                "description":"OpenDocument Spreadsheet"
                            },
                            {
                                "name":"RTF",
                                "value":"RTF",
                                "description":"Rich Text Format"
                            },
                            {
                                "name":"XLS",
                                "value":"XLS",
                                "description":"Microsoft Excel"
                            },
                            {
                                "name":"XLSX",
                                "value":"XLSX",
                                "description":"Microsoft Excel"
                            }
                        ]
                    },
                    {
                        "defaultValue":"data",
                        "description":"Name of the binary property in which to save the binary data of the spreadsheet file.",
                        "displayName":"Binary Property",
                        "displayOption":{
                            "show":{
                                "operation":["WRITE"]
                            }
                        },
                        "name":"binaryPropertyName",
                        "type":"STRING"
                    },
                    {
                        "displayName":"Options",
                        "name":"options",
                        "type":"COLLECTION",
                        "options":[
                            {
                                "defaultValue":false,
                                "description":"Weather compression will be applied or not.",
                                "displayName":"Compression",
                                "displayOption":{
                                    "show":{
                                        "operation":["WRITE"],
                                        "fileFormat":["XLSX","ODS"]
                                    }
                                },
                                "name":"compression",
                                "type":"BOOLEAN"
                            },
                            {
                                "defaultValue":"",
                                "description":"File name to set in binary data. By default will \\"spreadsheet.<fileFormat>\\" be used.",
                                "displayName":"File Name",
                                "displayOption":{
                                    "show":{
                                        "operation":["WRITE"]
                                    }
                                },
                                "name":"fileName",
                                "type":"STRING"
                            },
                            {
                                "defaultValue":true,
                                "description":"The first row of the file contains the header names.",
                                "displayName":"Header Row",
                                "displayOption":{
                                    "show":{
                                        "operation":["READ"]
                                    }
                                },
                                "name":"headerRow",
                                "type":"BOOLEAN"
                            },
                            {
                                "defaultValue":false,
                                "description":"When reading from file the empty cells will be filled with an empty string.",
                                "displayName":"Include Empty Cells",
                                "displayOption":{
                                    "show":{
                                        "operation":["READ"]
                                    }
                                },
                                "name":"includeEmptyCells",
                                "type":"BOOLEAN"
                            },
                            {
                                "defaultValue":false,
                                "description":"If the data should be returned RAW instead of parsed.",
                                "displayName":"RAW Data",
                                "displayOption":{
                                    "show":{
                                        "operation":["READ"]
                                    }
                                },
                                "name":"rawData",
                                "type":"BOOLEAN"
                            },
                            {
                                "defaultValue":false,
                                "description":"In some cases and file formats, it is necessary to read data specifically as string, otherwise some special characters are interpreted the wrong way.",
                                "displayName":"Read As String",
                                "displayOption":{
                                    "show":{
                                        "operation":["READ"]
                                    }
                                },
                                "name":"readAsString",
                                "type":"BOOLEAN"
                            },
                            {
                                "description":"The range to read from the table. If set to a number it will be the starting row.",
                                "displayName":"Range",
                                "displayOption":{
                                    "show":{
                                        "operation":["READ"]
                                    }
                                },
                                "name":"range",
                                "type":"COLLECTION",
                                "fields":[
                                    {
                                        "displayName":"Start Row index",
                                        "name":"startRow",
                                        "type":"NUMBER"
                                    },
                                    {
                                        "displayName":"End Row index",
                                        "name":"endRow","type":"NUMBER"
                                    }
                                ]
                            },
                            {
                                "defaultValue":"Sheet",
                                "description":"The name of the sheet to read from in the spreadsheet (if supported). If not set, the first one gets chosen.",
                                "displayName":"Sheet Name",
                                "displayOption":{
                                    "show":{
                                        "operation":["READ"]
                                    }
                                },
                                "name":"sheetName","type":"STRING"
                            },
                            {
                                "defaultValue":"Sheet",
                                "description":"The name of the sheet to create in the spreadsheet.",
                                "displayName":"Sheet Name",
                                "displayOption":{
                                    "show":{
                                        "operation":["WRITE"],
                                        "fileFormat":["ODS","XLS","XLSX"]
                                    }
                                },
                                "name":"sheetName",
                                "type":"STRING"
                            }
                        ],
                        "placeholder":"Add Option"
                    }
                ],
                "version":1.0
            }
            """,
            (JSONObject) JSONParser.parseJSON(
                objectMapper.writeValueAsString(SpreadsheetFileTaskDescriptor.TASK_DESCRIPTION)
            ),
            true
        );
    }
}
