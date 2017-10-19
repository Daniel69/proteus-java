package io.netifi.proteus.frames;

import io.netty.buffer.ByteBuf;

public class ProteusMetadataFlyweight {
  private static final int VERSION = 2;
  private static final int NAMESPACE_ID_SIZE = 4;
  private static final int SERVICE_ID_SIZE = 4;
  private static final int METHOD_ID_SIZE = 4;

  public static int computeLength() {
    return VERSION + NAMESPACE_ID_SIZE + SERVICE_ID_SIZE + METHOD_ID_SIZE;
  }
  
  public static int encode(ByteBuf byteBuf, int namespaceId, int serviceId, int methodId) {
      int offset = 0;
      
      byteBuf.setShort(offset, 1);
      offset += VERSION;
      
      byteBuf.setInt(offset, namespaceId);
      offset += NAMESPACE_ID_SIZE;
      
      byteBuf.setInt(offset, serviceId);
      offset += SERVICE_ID_SIZE;
      
      byteBuf.setInt(offset, methodId);
      offset += METHOD_ID_SIZE;
      
      return offset;
  }

  public static int verion(ByteBuf byteBuf) {
    return toUnsignedInt(byteBuf.getShort(0));
  }

  public static int namespaceId(ByteBuf byteBuf) {
    int offset = VERSION;
    return byteBuf.getInt(offset);
  }

  public static int serviceId(ByteBuf byteBuf) {
    int offset = VERSION + NAMESPACE_ID_SIZE;
    return byteBuf.getInt(offset);
  }

  public static int methodId(ByteBuf byteBuf) {
    int offset = VERSION + NAMESPACE_ID_SIZE + SERVICE_ID_SIZE;
    return byteBuf.getInt(offset);
  }

  private static int toUnsignedInt(short x) {
    return x & 0x7FFF;
  }
}
