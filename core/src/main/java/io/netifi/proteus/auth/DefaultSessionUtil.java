package io.netifi.proteus.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;

/** */
class DefaultSessionUtil extends SessionUtil {
  private static final String ALGORITHM = "HmacSHA1";
  private static final ThreadLocal<ByteBuffer> LONG_BUFFER =
      new ThreadLocal<ByteBuffer>() {
        @Override
        protected ByteBuffer initialValue() {
          return ByteBuffer.allocate(8);
        }
      };
  private final Clock clock;

  DefaultSessionUtil(Clock clock) {
    this.clock = clock;
  }

  DefaultSessionUtil() {
    this(new DefaultSystemClock());
  }

  @Override
  public byte[] generateSessionToken(byte[] key, ByteBuffer data, long count) {
    try {
      byte[] steps = getStepsAsByteArray(count);
      byte[] oneTimeKey = new byte[key.length + 8];

      System.arraycopy(key, 0, oneTimeKey, 0, key.length);
      System.arraycopy(steps, 0, oneTimeKey, key.length, steps.length);

      SecretKeySpec keySpec = new SecretKeySpec(oneTimeKey, ALGORITHM);
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(keySpec);
      mac.update(data);
      return mac.doFinal();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override
  public int generateRequestToken(byte[] sessionToken, ByteBuffer message, long count) {
    byte[] bytes = generateSessionToken(sessionToken, message, count);
    ByteBuffer byteBuffer = LONG_BUFFER.get();
    byteBuffer.clear();
    byteBuffer.put(bytes, 0, 4);
    return ByteBuffer.wrap(bytes).getInt();
  }

  @Override
  public boolean validateMessage(byte[] sessionToken, ByteBuffer message, int requestToken, long count) {
    int generatedToken = generateRequestToken(sessionToken, message, count);
    return requestToken == generatedToken;
  }

  byte[] getStepsAsByteArray(long count) {
    ByteBuffer byteBuffer = LONG_BUFFER.get();
    byteBuffer.clear();
    byteBuffer.putLong(count);
    return byteBuffer.array();
  }

  @Override
  public long getThirtySecondsStepsFromEpoch() {
    return clock.getEpochTime() / 30000;
  }
}
