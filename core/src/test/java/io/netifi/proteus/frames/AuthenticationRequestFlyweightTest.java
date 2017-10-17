package io.netifi.proteus.frames;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

/** */
public class AuthenticationRequestFlyweightTest {
  @Test
  public void testEncode() {
    int length = AuthenticationRequestFlyweight.computeLength();
    byte[] at = new byte[20];
    Random rnd = new Random();
    rnd.nextBytes(at);
    long accessKey = 1;
    ByteBuf byteBuf = Unpooled.buffer(length);

    int encodedLength =
        AuthenticationRequestFlyweight.encode(byteBuf, Unpooled.wrappedBuffer(at), accessKey, 0);
    Assert.assertEquals(length, encodedLength);

    Assert.assertEquals(accessKey, AuthenticationRequestFlyweight.accessKey(byteBuf));
    byte[] at1 = new byte[20];
    AuthenticationRequestFlyweight.accessToken(byteBuf).getBytes(0, at1);
    Assert.assertArrayEquals(at, at1);
  }
}
