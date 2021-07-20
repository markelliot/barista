package barista;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.net.MediaType;
import java.io.IOException;

public interface SerDe {
    <T> ByteRepr serialize(T any);

    <T> T deserialize(ByteRepr bytes, Class<T> objClass);

    String contentType();

    record ByteRepr(String raw) {}

    final class ObjectMapperSerDe implements SerDe {
        private final ObjectMapper mapper;

        public ObjectMapperSerDe() {
            this.mapper =
                    new ObjectMapper()
                            .registerModule(new GuavaModule())
                            .registerModule(new Jdk8Module())
                            .registerModule(new JavaTimeModule())
                            .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
                            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }

        @Override
        public <T> ByteRepr serialize(T any) {
            try {
                return new ByteRepr(mapper.writeValueAsString(any));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error while serializing object to bytes", e);
            }
        }

        @Override
        public <T> T deserialize(ByteRepr bytes, Class<T> objClass) {
            try {
                return mapper.readValue(bytes.raw(), objClass);
            } catch (IOException e) {
                throw new RuntimeException("Error while deserializing object from bytes", e);
            }
        }

        @Override
        public String contentType() {
            return MediaType.JSON_UTF_8.toString();
        }
    }
}
