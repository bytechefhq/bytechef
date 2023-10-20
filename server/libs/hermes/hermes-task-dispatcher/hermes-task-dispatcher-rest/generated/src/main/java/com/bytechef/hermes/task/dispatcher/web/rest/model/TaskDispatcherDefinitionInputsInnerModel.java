package com.bytechef.hermes.task.dispatcher.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.task.dispatcher.web.rest.model.AnyPropertyModel;
import com.bytechef.hermes.task.dispatcher.web.rest.model.ArrayPropertyModel;
import com.bytechef.hermes.task.dispatcher.web.rest.model.BooleanPropertyModel;
import com.bytechef.hermes.task.dispatcher.web.rest.model.DateTimePropertyModel;
import com.bytechef.hermes.task.dispatcher.web.rest.model.DisplayOptionModel;
import com.bytechef.hermes.task.dispatcher.web.rest.model.IntegerPropertyModel;
import com.bytechef.hermes.task.dispatcher.web.rest.model.NullPropertyModel;
import com.bytechef.hermes.task.dispatcher.web.rest.model.NumberPropertyModel;
import com.bytechef.hermes.task.dispatcher.web.rest.model.ObjectPropertyModel;
import com.bytechef.hermes.task.dispatcher.web.rest.model.OptionPropertyModel;
import com.bytechef.hermes.task.dispatcher.web.rest.model.PropertyTypeModel;
import com.bytechef.hermes.task.dispatcher.web.rest.model.StringPropertyModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;


@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-10-27T16:28:51.543539+02:00[Europe/Zagreb]")
public interface TaskDispatcherDefinitionInputsInnerModel {
}
