package io.netifi.proteus.frames;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

/** */
public class RoutingFlyweightTest {

  @Test
  public void testComputeLengthWithApiNoMetadataNoToken() {
    int expected = 49;
    int length = RoutingFlyweight.computeLength(true, false, false, "dest", Unpooled.buffer(12));
    Assert.assertEquals(expected, length);
  }

  @Test
  public void testComputeLengthWithApiMetadataNoToken() {
    int expected = 53;
    int length = RoutingFlyweight.computeLength(true, true, false, "dest", Unpooled.buffer(12));
    Assert.assertEquals(expected, length);
  }

  @Test
  public void testComputeLengthWithApiMetadataToken() {
    int expected = 57;
    int length = RoutingFlyweight.computeLength(true, true, true, "dest", Unpooled.buffer(12));
    Assert.assertEquals(expected, length);
  }

  @Test
  public void testComputeLengthWithNoApiMetadataNoToken() {
    int expected = 41;
    int length = RoutingFlyweight.computeLength(false, true, false, "dest", Unpooled.buffer(12));
    Assert.assertEquals(expected, length);
  }

  @Test
  public void testComputeLengthWithNoApiMetadataToken() {
    int expected = 45;
    int length = RoutingFlyweight.computeLength(false, true, true, "dest", Unpooled.buffer(12));
    Assert.assertEquals(expected, length);
  }

  @Test
  public void testComputeLengthWithNoApiNoMetadataToken() {
    int expected = 41;
    int length = RoutingFlyweight.computeLength(false, false, true, "dest", Unpooled.buffer(12));
    Assert.assertEquals(expected, length);
  }

  @Test
  public void testEncodeWithApiCall() {
    String fromDestinationId = "dest";
    String group = "group";
    String destination = "toDest";
    int length1 =
        RouteDestinationFlyweight.computeLength(RouteType.STREAM_ID_ROUTE, destination, group);
    ByteBuf routeDestinationBuf = Unpooled.buffer(length1);
    RouteDestinationFlyweight.encodeRouteByDestination(
        routeDestinationBuf, RouteType.STREAM_ID_ROUTE, 1, destination, group);

    int length =
        RoutingFlyweight.computeLength(true, true, false, fromDestinationId, routeDestinationBuf);

    System.out.println(length1);

    ByteBuf byteBuf = Unpooled.buffer(length);

    boolean apiCall = true;
    boolean hasToken = false;
    boolean hasMetadata = true;
    int token = 0;
    long fromAccessKey = 1;

    int userMetadataLength = 3;
    int namespaceId = 4;
    int serviceId = 5;
    int methodId = 6;

    int encodeLength =
        RoutingFlyweight.encode(
            byteBuf,
            apiCall,
            hasToken,
            hasMetadata,
            token,
            fromAccessKey,
            fromDestinationId,
            userMetadataLength,
            namespaceId,
            serviceId,
            methodId,
            0,
            routeDestinationBuf);

    Assert.assertEquals(length, encodeLength);

    System.out.println(encodeLength);

    Assert.assertEquals(RoutingFlyweight.accessKey(byteBuf), fromAccessKey);
    Assert.assertEquals(RoutingFlyweight.destination(byteBuf), fromDestinationId);
    Assert.assertEquals(RoutingFlyweight.userMetadataLength(byteBuf), userMetadataLength);
    Assert.assertEquals(RoutingFlyweight.namespaceId(byteBuf), namespaceId);
    Assert.assertEquals(RoutingFlyweight.serviceId(byteBuf), serviceId);
    Assert.assertEquals(RoutingFlyweight.methodId(byteBuf), methodId);
  }

  @Test
  public void testEncodeWithoutApiCall() {
    String fromDestinationId = "dest";
    String group = "group";
    String destination = "toDest";
    int length1 =
        RouteDestinationFlyweight.computeLength(RouteType.STREAM_ID_ROUTE, destination, group);
    ByteBuf routeDestinationBuf = Unpooled.buffer(length1);
    RouteDestinationFlyweight.encodeRouteByDestination(
        routeDestinationBuf, RouteType.STREAM_ID_ROUTE, 1, destination, group);

    int length =
        RoutingFlyweight.computeLength(false, true, false, fromDestinationId, routeDestinationBuf);

    System.out.println(length1);

    ByteBuf byteBuf = Unpooled.buffer(length);

    boolean apiCall = false;
    boolean hasToken = false;
    boolean hasMetadata = true;
    int token = 0;
    long fromAccessKey = 1;

    int userMetadataLength = 3;
    int namespaceId = 4;
    int serviceId = 5;
    int methodId = 6;

    int encodeLength =
        RoutingFlyweight.encode(
            byteBuf,
            apiCall,
            hasToken,
            hasMetadata,
            token,
            fromAccessKey,
            fromDestinationId,
            userMetadataLength,
            namespaceId,
            serviceId,
            methodId,
            0,
            routeDestinationBuf);

    Assert.assertEquals(length, encodeLength);

    System.out.println(encodeLength);

    Assert.assertEquals(RoutingFlyweight.accessKey(byteBuf), fromAccessKey);
    Assert.assertEquals(RoutingFlyweight.destination(byteBuf), fromDestinationId);
    Assert.assertEquals(RoutingFlyweight.userMetadataLength(byteBuf), userMetadataLength);
  }
}
