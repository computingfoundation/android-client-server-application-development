package com.fencedin.backend.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fencedin.commons.base.LowercaseEnumDeserializerModifier;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

/**
 * Custom Jackson object mapper for JAX-RS
 */
@Provider
public class JacksonConfig implements ContextResolver<ObjectMapper> {
    private ObjectMapper mObjectMapper;

    public JacksonConfig() throws Exception {
        mObjectMapper = new ObjectMapper();
        // Module that allows deserializing all enum values using case-insensitive characters
        SimpleModule module = new SimpleModule().setDeserializerModifier(new LowercaseEnumDeserializerModifier());
        mObjectMapper.registerModule(module);
    }

    public ObjectMapper getContext(Class<?> objectType) {
        return mObjectMapper;
    }

}
