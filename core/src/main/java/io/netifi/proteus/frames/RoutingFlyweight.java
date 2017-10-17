package io.netifi.proteus.frames;

import io.netifi.proteus.util.BitUtil;
import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

/** */
public class RoutingFlyweight {
  private static final int NAMESPACE_ID_SIZE = BitUtil.SIZE_OF_INT;
  private static final int SERVICE_ID_SIZE = BitUtil.SIZE_OF_INT;
  private static final int METHOD_ID_SIZE = BitUtil.SIZE_OF_INT;
  private static final int USER_METADATA_LENGTH_SIZE = BitUtil.SIZE_OF_INT;
  private static final int ACCESS_KEY_SIZE = BitUtil.SIZE_OF_LONG;
  private static final int DESTINATION_LENGTH_SIZE = BitUtil.SIZE_OF_BYTE;
  private static final int TOKEN_SIZE = BitUtil.SIZE_OF_INT;

  private RoutingFlyweight() {}

  public static int computeLength(
      boolean apiCall, boolean metadata, boolean token, CharSequence destination, ByteBuf route) {
    int length = FrameHeaderFlyweight.computeFrameHeaderLength();
    length += route.capacity();

    if (apiCall) {
      length += NAMESPACE_ID_SIZE + SERVICE_ID_SIZE + METHOD_ID_SIZE;
    }

    if (metadata) {
      length += USER_METADATA_LENGTH_SIZE;
    }

    if (token) {
      length += TOKEN_SIZE;
    }

    length += ACCESS_KEY_SIZE + DESTINATION_LENGTH_SIZE + destination.length();

    return length;
  }

  public static int encode(
      ByteBuf byteBuf,
      boolean apiCall,
      boolean hasToken,
      boolean hasMetadata,
      int token,
      long fromAccessKey,
      CharSequence fromDestination,
      int userMetadataLength,
      int namespaceId,
      int serviceId,
      int methodId,
      long seqId,
      ByteBuf route) {

    int destinationLength = fromDestination.length();
    if (destinationLength > 255) {
      throw new IllegalArgumentException("destination is longer then 255 characters");
    }

    if (route == null) {
      throw new NullPointerException("routes must not be null");
    }

    int flags = FrameHeaderFlyweight.encodeFlags(true, hasMetadata, false, apiCall, hasToken);

    int offset = FrameHeaderFlyweight.encodeFrameHeader(byteBuf, FrameType.ROUTE, flags, seqId);

    if (hasToken) {
      byteBuf.setInt(offset, token);
      offset += TOKEN_SIZE;
    }

    byteBuf.setLong(offset, fromAccessKey);
    offset += ACCESS_KEY_SIZE;

    byteBuf.setByte(offset, destinationLength);
    offset += DESTINATION_LENGTH_SIZE;

    byteBuf.setCharSequence(offset, fromDestination, CharsetUtil.US_ASCII);
    offset += destinationLength;

    if (hasMetadata) {
      byteBuf.setInt(offset, userMetadataLength);
      offset += USER_METADATA_LENGTH_SIZE;
    }

    if (apiCall) {
      byteBuf.setInt(offset, namespaceId);
      offset += NAMESPACE_ID_SIZE;

      byteBuf.setInt(offset, serviceId);
      offset += SERVICE_ID_SIZE;

      byteBuf.setInt(offset, methodId);
      offset += METHOD_ID_SIZE;
    }

    byte[] bytes = new byte[route.capacity()];
    route.getBytes(0, bytes);
    byteBuf.setBytes(offset, bytes);
    offset += route.capacity();

    return offset;
  }

  public static int token(ByteBuf byteBuf) {
    if (FrameHeaderFlyweight.token(byteBuf)) {
      int offset = FrameHeaderFlyweight.computeFrameHeaderLength();
      return byteBuf.getInt(offset);
    } else {
      throw new IllegalStateException("no token flag set");
    }
  }

  public static long accessKey(ByteBuf byteBuf) {
    int offset =
        FrameHeaderFlyweight.computeFrameHeaderLength()
            + (FrameHeaderFlyweight.token(byteBuf) ? TOKEN_SIZE : 0);

    return byteBuf.getLong(offset);
  }

  private static int calculateDestinationOffset(ByteBuf byteBuf) {
    int offset =
        FrameHeaderFlyweight.computeFrameHeaderLength()
            + (FrameHeaderFlyweight.token(byteBuf) ? TOKEN_SIZE : 0)
            + ACCESS_KEY_SIZE;
    int length = BitUtil.toUnsignedInt(byteBuf.getByte(offset));
    return length + DESTINATION_LENGTH_SIZE;
  }

  public static String destination(ByteBuf byteBuf) {
    int offset =
        FrameHeaderFlyweight.computeFrameHeaderLength()
            + (FrameHeaderFlyweight.token(byteBuf) ? TOKEN_SIZE : 0)
            + ACCESS_KEY_SIZE;
    int length = BitUtil.toUnsignedInt(byteBuf.getByte(offset));
    offset += DESTINATION_LENGTH_SIZE;
    return (String) byteBuf.getCharSequence(offset, length, CharsetUtil.US_ASCII);
  }

  public static int userMetadataLength(ByteBuf byteBuf) {
    int offset =
        FrameHeaderFlyweight.computeFrameHeaderLength()
            + (FrameHeaderFlyweight.token(byteBuf) ? TOKEN_SIZE : 0)
            + ACCESS_KEY_SIZE
            + calculateDestinationOffset(byteBuf);

    return byteBuf.getInt(offset);
  }

  public static int namespaceId(ByteBuf byteBuf) {
    int offset =
        FrameHeaderFlyweight.computeFrameHeaderLength()
            + (FrameHeaderFlyweight.token(byteBuf) ? TOKEN_SIZE : 0)
            + ACCESS_KEY_SIZE
            + calculateDestinationOffset(byteBuf)
            + +(FrameHeaderFlyweight.hasMetadata(byteBuf) ? USER_METADATA_LENGTH_SIZE : 0);

    return byteBuf.getInt(offset);
  }

  public static int serviceId(ByteBuf byteBuf) {
    int offset =
        FrameHeaderFlyweight.computeFrameHeaderLength()
            + (FrameHeaderFlyweight.token(byteBuf) ? TOKEN_SIZE : 0)
            + ACCESS_KEY_SIZE
            + calculateDestinationOffset(byteBuf)
            + (FrameHeaderFlyweight.hasMetadata(byteBuf) ? USER_METADATA_LENGTH_SIZE : 0)
            + NAMESPACE_ID_SIZE;

    return byteBuf.getInt(offset);
  }

  public static int methodId(ByteBuf byteBuf) {
    int offset =
        FrameHeaderFlyweight.computeFrameHeaderLength()
            + (FrameHeaderFlyweight.token(byteBuf) ? TOKEN_SIZE : 0)
            + ACCESS_KEY_SIZE
            + calculateDestinationOffset(byteBuf)
            + (FrameHeaderFlyweight.hasMetadata(byteBuf) ? USER_METADATA_LENGTH_SIZE : 0)
            + NAMESPACE_ID_SIZE
            + SERVICE_ID_SIZE;

    return byteBuf.getInt(offset);
  }

  public static ByteBuf route(ByteBuf byteBuf) {

    int offset =
        FrameHeaderFlyweight.computeFrameHeaderLength()
            + (FrameHeaderFlyweight.token(byteBuf) ? TOKEN_SIZE : 0)
            + ACCESS_KEY_SIZE
            + calculateDestinationOffset(byteBuf)
            + (FrameHeaderFlyweight.hasMetadata(byteBuf) ? USER_METADATA_LENGTH_SIZE : 0);

    if (FrameHeaderFlyweight.apiCall(byteBuf)) {
      offset += NAMESPACE_ID_SIZE + SERVICE_ID_SIZE + METHOD_ID_SIZE;
    }

    return byteBuf.slice(offset, byteBuf.capacity() - offset);
  }
}
