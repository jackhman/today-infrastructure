/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.http.codec.protobuf;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;

import org.reactivestreams.Publisher;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.codec.Decoder;
import cn.taketoday.core.codec.DecodingException;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferLimitException;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * A {@code Decoder} that reads {@link com.google.protobuf.Message}s using
 * <a href="https://developers.google.com/protocol-buffers/">Google Protocol Buffers</a>.
 *
 * <p>Flux deserialized via
 * {@link #decode(Publisher, ResolvableType, MimeType, Map)} are expected to use
 * <a href="https://developers.google.com/protocol-buffers/docs/techniques?hl=en#streaming">
 * delimited Protobuf messages</a> with the size of each message specified before
 * the message itself. Single values deserialized via
 * {@link #decodeToMono(Publisher, ResolvableType, MimeType, Map)} are expected
 * to use regular Protobuf message format (without the size prepended before
 * the message).
 *
 * <p>Notice that default instance of Protobuf message produces empty byte
 * array, so {@code Mono.just(Msg.getDefaultInstance())} sent over the network
 * will be deserialized as an empty {@link Mono}.
 *
 * <p>To generate {@code Message} Java classes, you need to install the
 * {@code protoc} binary.
 *
 * <p>This decoder requires Protobuf 3 or higher, and supports
 * {@code "application/x-protobuf"} and {@code "application/octet-stream"} with
 * the official {@code "com.google.protobuf:protobuf-java"} library.
 *
 * @author Sebastien Deleuze
 * @see ProtobufEncoder
 * @since 4.0
 */
public class ProtobufDecoder extends ProtobufCodecSupport implements Decoder<Message> {

  /** The default max size for aggregating messages. */
  protected static final int DEFAULT_MESSAGE_MAX_SIZE = 256 * 1024;

  private static final ConcurrentMap<Class<?>, Method> methodCache = new ConcurrentReferenceHashMap<>();

  private final ExtensionRegistry extensionRegistry;

  private int maxMessageSize = DEFAULT_MESSAGE_MAX_SIZE;

  /**
   * Construct a new {@code ProtobufDecoder}.
   */
  public ProtobufDecoder() {
    this(ExtensionRegistry.newInstance());
  }

  /**
   * Construct a new {@code ProtobufDecoder} with an initializer that allows the
   * registration of message extensions.
   *
   * @param extensionRegistry a message extension registry
   */
  public ProtobufDecoder(ExtensionRegistry extensionRegistry) {
    Assert.notNull(extensionRegistry, "ExtensionRegistry must not be null");
    this.extensionRegistry = extensionRegistry;
  }

  /**
   * The max size allowed per message.
   * <p>By default, this is set to 256K.
   *
   * @param maxMessageSize the max size per message, or -1 for unlimited
   */
  public void setMaxMessageSize(int maxMessageSize) {
    this.maxMessageSize = maxMessageSize;
  }

  /**
   * Return the {@link #setMaxMessageSize configured} message size limit.
   */
  public int getMaxMessageSize() {
    return this.maxMessageSize;
  }

  @Override
  public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
    return Message.class.isAssignableFrom(elementType.toClass()) && supportsMimeType(mimeType);
  }

  @Override
  public Flux<Message> decode(
          Publisher<DataBuffer> inputStream, ResolvableType elementType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    MessageDecoderFunction decoderFunction =
            new MessageDecoderFunction(elementType, this.maxMessageSize);

    return Flux.from(inputStream)
            .flatMapIterable(decoderFunction)
            .doOnTerminate(decoderFunction::discard);
  }

  @Override
  public Mono<Message> decodeToMono(Publisher<DataBuffer> inputStream, ResolvableType elementType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

    return DataBufferUtils.join(inputStream, this.maxMessageSize)
            .map(dataBuffer -> decode(dataBuffer, elementType, mimeType, hints));
  }

  @Override
  public Message decode(
          DataBuffer dataBuffer, ResolvableType targetType,
          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) throws DecodingException {

    try {
      Message.Builder builder = getMessageBuilder(targetType.toClass());
      ByteBuffer byteBuffer = ByteBuffer.allocate(dataBuffer.readableByteCount());
      dataBuffer.toByteBuffer(byteBuffer);
      builder.mergeFrom(CodedInputStream.newInstance(byteBuffer), this.extensionRegistry);
      return builder.build();
    }
    catch (IOException ex) {
      throw new DecodingException("I/O error while parsing input stream", ex);
    }
    catch (Exception ex) {
      throw new DecodingException("Could not read Protobuf message: " + ex.getMessage(), ex);
    }
    finally {
      DataBufferUtils.release(dataBuffer);
    }
  }

  /**
   * Create a new {@code Message.Builder} instance for the given class.
   * <p>This method uses a ConcurrentHashMap for caching method lookups.
   */
  private static Message.Builder getMessageBuilder(Class<?> clazz) throws Exception {
    Method method = methodCache.get(clazz);
    if (method == null) {
      method = clazz.getMethod("newBuilder");
      methodCache.put(clazz, method);
    }
    return (Message.Builder) method.invoke(clazz);
  }

  @Override
  public List<MimeType> getDecodableMimeTypes() {
    return getMimeTypes();
  }

  private class MessageDecoderFunction implements Function<DataBuffer, Iterable<? extends Message>> {

    private final ResolvableType elementType;

    private final int maxMessageSize;

    @Nullable
    private DataBuffer output;

    private int messageBytesToRead;

    private int offset;

    public MessageDecoderFunction(ResolvableType elementType, int maxMessageSize) {
      this.elementType = elementType;
      this.maxMessageSize = maxMessageSize;
    }

    @Override
    public Iterable<? extends Message> apply(DataBuffer input) {
      try {
        List<Message> messages = new ArrayList<>();
        int remainingBytesToRead;
        int chunkBytesToRead;

        do {
          if (this.output == null) {
            if (!readMessageSize(input)) {
              return messages;
            }
            if (this.maxMessageSize > 0 && this.messageBytesToRead > this.maxMessageSize) {
              throw new DataBufferLimitException(
                      "The number of bytes to read for message " +
                              "(" + this.messageBytesToRead + ") exceeds " +
                              "the configured limit (" + this.maxMessageSize + ")");
            }
            this.output = input.factory().allocateBuffer(this.messageBytesToRead);
          }

          chunkBytesToRead = Math.min(this.messageBytesToRead, input.readableByteCount());
          remainingBytesToRead = input.readableByteCount() - chunkBytesToRead;

          byte[] bytesToWrite = new byte[chunkBytesToRead];
          input.read(bytesToWrite, 0, chunkBytesToRead);
          this.output.write(bytesToWrite);
          this.messageBytesToRead -= chunkBytesToRead;

          if (this.messageBytesToRead == 0) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(this.output.readableByteCount());
            this.output.toByteBuffer(byteBuffer);
            CodedInputStream stream = CodedInputStream.newInstance(byteBuffer);
            DataBufferUtils.release(this.output);
            this.output = null;
            Message message = getMessageBuilder(this.elementType.toClass())
                    .mergeFrom(stream, extensionRegistry)
                    .build();
            messages.add(message);
          }
        }
        while (remainingBytesToRead > 0);
        return messages;
      }
      catch (DecodingException ex) {
        throw ex;
      }
      catch (IOException ex) {
        throw new DecodingException("I/O error while parsing input stream", ex);
      }
      catch (Exception ex) {
        throw new DecodingException("Could not read Protobuf message: " + ex.getMessage(), ex);
      }
      finally {
        DataBufferUtils.release(input);
      }
    }

    /**
     * Parse message size as a varint from the input stream, updating {@code messageBytesToRead} and
     * {@code offset} fields if needed to allow processing of upcoming chunks.
     * Inspired from {@link CodedInputStream#readRawVarint32(int, java.io.InputStream)}
     *
     * @return {code true} when the message size is parsed successfully, {code false} when the message size is
     * truncated
     * @see <a href="https://developers.google.com/protocol-buffers/docs/encoding#varints">Base 128 Varints</a>
     */
    private boolean readMessageSize(DataBuffer input) {
      if (this.offset == 0) {
        if (input.readableByteCount() == 0) {
          return false;
        }
        int firstByte = input.read();
        if ((firstByte & 0x80) == 0) {
          this.messageBytesToRead = firstByte;
          return true;
        }
        this.messageBytesToRead = firstByte & 0x7f;
        this.offset = 7;
      }

      if (this.offset < 32) {
        for (; this.offset < 32; this.offset += 7) {
          if (input.readableByteCount() == 0) {
            return false;
          }
          final int b = input.read();
          this.messageBytesToRead |= (b & 0x7f) << this.offset;
          if ((b & 0x80) == 0) {
            this.offset = 0;
            return true;
          }
        }
      }
      // Keep reading up to 64 bits.
      for (; this.offset < 64; this.offset += 7) {
        if (input.readableByteCount() == 0) {
          return false;
        }
        final int b = input.read();
        if ((b & 0x80) == 0) {
          this.offset = 0;
          return true;
        }
      }
      this.offset = 0;
      throw new DecodingException("Cannot parse message size: malformed varint");
    }

    public void discard() {
      if (this.output != null) {
        DataBufferUtils.release(this.output);
      }
    }
  }

}
