package com.leegern.xrocketmq5.core.json;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.JsonNodeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.ser.std.ArraySerializerBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import javax.sql.rowset.serial.SerialException;
import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

public class JacksonProvider {

    public static final String NAME = "jackson";

    private static final Logger LOGGER = LoggerFactory.getLogger(JacksonProvider.class);

    private static JacksonProvider instance;

    /**
     * the zoneId for LocalDateTime
     */
    private static ZoneId zoneId = ZoneId.systemDefault();

    private final ObjectMapper mapper = new ObjectMapper();

    private final SimpleModule module = new SimpleModule();

    /**
     * customize serializer for java.sql.Timestamp
     */
    private final JsonSerializer timestampSerializer = new TimestampSerializer();

    /**
     * customize deserializer for java.sql.Timestamp
     */
    private final JsonDeserializer timestampDeserializer = new TimestampDeserializer();

    /**
     * customize serializer of java.sql.Blob
     */
    private final JsonSerializer blobSerializer = new BlobSerializer();

    /**
     * customize deserializer of java.sql.Blob
     */
    private final JsonDeserializer blobDeserializer = new BlobDeserializer();

    /**
     * customize serializer of java.sql.Clob
     */
    private final JsonSerializer clobSerializer = new ClobSerializer();

    /**
     * customize deserializer of java.sql.Clob
     */
    private final JsonDeserializer clobDeserializer = new ClobDeserializer();

    /**
     * customize serializer of java.time.LocalDateTime
     */
    private final JsonSerializer localDateTimeSerializer = new LocalDateTimeSerializer();

    /**
     * customize deserializer of java.time.LocalDateTime
     */
    private final JsonDeserializer localDateTimeDeserializer = new LocalDateTimeDeserializer();


    public static JacksonProvider getInstance() {
        if (Objects.isNull(instance)) {
            synchronized (JacksonProvider.class) {
                if (Objects.isNull(instance)) {
                    instance = new JacksonProvider();
                }
            }
        }
        return instance;
    }

    /**
     * Get jackson instance
     * @return
     */
    public ObjectMapper getJson() {
        return mapper;
    }


    {
        module.addSerializer(Timestamp.class, timestampSerializer);
        module.addDeserializer(Timestamp.class, timestampDeserializer);
        module.addSerializer(SerialBlob.class, blobSerializer);
        module.addDeserializer(SerialBlob.class, blobDeserializer);
        module.addSerializer(SerialClob.class, clobSerializer);
        module.addDeserializer(SerialClob.class, clobDeserializer);
        module.addSerializer(LocalDateTime.class, localDateTimeSerializer);
        module.addDeserializer(LocalDateTime.class, localDateTimeDeserializer);
        mapper.registerModule(module);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        mapper.enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER);
    }



    /**
     * if necessary
     * extend {@link ArraySerializerBase}
     */
    private static class TimestampSerializer extends JsonSerializer<Timestamp> {

        @Override
        public void serializeWithType(Timestamp timestamp, JsonGenerator gen, SerializerProvider serializers,
                                      TypeSerializer typeSerializer) throws IOException {
            JsonToken valueShape = JsonToken.VALUE_NUMBER_INT;
            // if has microseconds, serialized as an array
            if (timestamp.getNanos() % 1000000 > 0) {
                valueShape = JsonToken.START_ARRAY;
            }

            WritableTypeId typeId = typeSerializer.writeTypePrefix(gen,
                    typeSerializer.typeId(timestamp, valueShape));
            serialize(timestamp, gen, serializers);
            gen.writeTypeSuffix(typeId);
        }

        @Override
        public void serialize(Timestamp timestamp, JsonGenerator gen, SerializerProvider serializers) {
            try {
                gen.writeNumber(timestamp.getTime());
                // if has microseconds, serialized as an array, write the nanos to the array
                if (timestamp.getNanos() % 1000000 > 0) {
                    gen.writeNumber(timestamp.getNanos());
                }
            } catch (IOException e) {
                LOGGER.error("serialize java.sql.Timestamp error : {}", e.getMessage(), e);
            }
        }
    }

    /**
     * if necessary
     * extend {@link JsonNodeDeserializer}
     */
    private static class TimestampDeserializer extends JsonDeserializer<Timestamp> {

        @Override
        public Timestamp deserialize(JsonParser p, DeserializationContext ctxt) {
            try {
                if (p.isExpectedStartArrayToken()) {
                    ArrayNode arrayNode = p.getCodec().readTree(p);
                    Timestamp timestamp = new Timestamp(arrayNode.get(0).asLong());
                    timestamp.setNanos(arrayNode.get(1).asInt());
                    return timestamp;
                } else {
                    long timestamp = p.getLongValue();
                    return new Timestamp(timestamp);
                }
            } catch (IOException e) {
                LOGGER.error("deserialize java.sql.Timestamp error : {}", e.getMessage(), e);
            }
            return null;
        }
    }

    /**
     * the class of serialize blob type
     */
    private static class BlobSerializer extends JsonSerializer<SerialBlob> {

        @Override
        public void serializeWithType(SerialBlob blob, JsonGenerator gen, SerializerProvider serializers,
                                      TypeSerializer typeSer) throws IOException {
            WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen,
                    typeSer.typeId(blob, JsonToken.VALUE_EMBEDDED_OBJECT));
            serialize(blob, gen, serializers);
            typeSer.writeTypeSuffix(gen, typeIdDef);
        }

        @Override
        public void serialize(SerialBlob blob, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            try {
                gen.writeBinary(blob.getBytes(1, (int)blob.length()));
            } catch (SerialException e) {
                LOGGER.error("serialize java.sql.Blob error : {}", e.getMessage(), e);
            }
        }
    }

    /**
     * the class of deserialize blob type
     */
    private static class BlobDeserializer extends JsonDeserializer<SerialBlob> {

        @Override
        public SerialBlob deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            try {
                return new SerialBlob(p.getBinaryValue());
            } catch (SQLException e) {
                LOGGER.error("deserialize java.sql.Blob error : {}", e.getMessage(), e);
            }
            return null;
        }
    }

    /**
     * the class of serialize clob type
     */
    private static class ClobSerializer extends JsonSerializer<SerialClob> {

        @Override
        public void serializeWithType(SerialClob clob, JsonGenerator gen, SerializerProvider serializers,
                                      TypeSerializer typeSer) throws IOException {
            WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen,
                    typeSer.typeId(clob, JsonToken.VALUE_EMBEDDED_OBJECT));
            serialize(clob, gen, serializers);
            typeSer.writeTypeSuffix(gen, typeIdDef);
        }

        @Override
        public void serialize(SerialClob clob, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            try (Reader r = clob.getCharacterStream()) {
                gen.writeString(r, (int)clob.length());
            } catch (SerialException e) {
                LOGGER.error("serialize java.sql.Blob error : {}", e.getMessage(), e);
            }
        }
    }

    private static class ClobDeserializer extends JsonDeserializer<SerialClob> {

        @Override
        public SerialClob deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            try {
                return new SerialClob(p.getValueAsString().toCharArray());
            } catch (SQLException e) {
                LOGGER.error("deserialize java.sql.Clob error : {}", e.getMessage(), e);
            }
            return null;
        }
    }

    /**
     * the class of serialize LocalDateTime type
     */
    private static class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {

        @Override
        public void serializeWithType(LocalDateTime localDateTime, JsonGenerator gen, SerializerProvider serializers,
                                      TypeSerializer typeSer) throws IOException {
            JsonToken valueShape = JsonToken.VALUE_NUMBER_INT;
            // if has microseconds, serialized as an array
            if (localDateTime.getNano() % 1000000 > 0) {
                valueShape = JsonToken.START_ARRAY;
            }

            WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen,
                    typeSer.typeId(localDateTime, valueShape));
            serialize(localDateTime, gen, serializers);
            typeSer.writeTypeSuffix(gen, typeIdDef);
        }

        @Override
        public void serialize(LocalDateTime localDateTime, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            try {
                Instant instant = localDateTime.atZone(zoneId).toInstant();
                gen.writeNumber(instant.toEpochMilli());
                // if has microseconds, serialized as an array, write the nano to the array
                if (instant.getNano() % 1000000 > 0) {
                    gen.writeNumber(instant.getNano());
                }
            } catch (IOException e) {
                LOGGER.error("serialize java.time.LocalDateTime error : {}", e.getMessage(), e);
            }
        }
    }

    /**
     * the class of deserialize LocalDateTime type
     */
    private static class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            try {
                Instant instant;
                if (p.isExpectedStartArrayToken()) {
                    ArrayNode arrayNode = p.getCodec().readTree(p);
                    long timestamp = arrayNode.get(0).asLong();
                    instant = Instant.ofEpochMilli(timestamp);
                    if (arrayNode.size() > 1) {
                        int nano = arrayNode.get(1).asInt();
                        instant = instant.plusNanos(nano % 1000000);
                    }
                } else {
                    long timestamp = p.getLongValue();
                    instant = Instant.ofEpochMilli(timestamp);
                }
                return LocalDateTime.ofInstant(instant, zoneId);
            } catch (Exception e) {
                LOGGER.error("deserialize java.time.LocalDateTime error : {}", e.getMessage(), e);
            }
            return null;
        }
    }
}
