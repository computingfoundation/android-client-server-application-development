package com.organization.commons.base;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;

import java.io.IOException;

/**
 * Allows lowercase to be used when deserializing enums
 */
public class LowercaseEnumDeserializerModifier extends BeanDeserializerModifier {

    @Override
    public JsonDeserializer<Enum> modifyEnumDeserializer(DeserializationConfig config, final JavaType type,
                                                         BeanDescription beanDesc,
                                                         final JsonDeserializer<?> deserializer) {
        return new JsonDeserializer<Enum>() {
            @Override
            public Enum deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
                    Class<? extends Enum> rawClass = (Class<Enum<?>>) type.getRawClass();
                    return Enum.valueOf(rawClass, jp.getValueAsString().toUpperCase());
                }
            };
    }

}