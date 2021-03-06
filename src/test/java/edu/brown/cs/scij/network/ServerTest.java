package edu.brown.cs.scij.network;

import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.*;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class ServerTest {
  @Test
  public void connectTest() throws NoSuchPlayerException {
    Server s = new DummyServer();
    Map<String, Object> id0 = s.connect("an.ip.address");
    Map<String, Object> id1 = s.connect("another.ip.address");
    Key k0 = Key.fromJSONString((String) id0.get("key"));
    Key k1 = Key.fromJSONString((String) id1.get("key"));
    s.disconnect(k0);
    s.disconnect(k1);

    assertEquals(id0, s.connect("an.ip.address"));
    assertEquals(id1, s.connect("another.ip.address"));
    assertFalse(id0 == id1);

    s.close();
  }

  @Test
  public void pingTest() throws NoSuchPlayerException {
    Server s = new DummyServer();
    Map<String, Object> map0 = s.connect("my.ip.address");
    Map<String, Object> map1 = s.connect("your.ip.address");
    Key id0 = Key.fromJSONString((String) map0.get("key"));
    Key id1 = Key.fromJSONString((String) map1.get("key"));

    s.ping(id0);
    s.ping(id1);
    assertEquals(2, s.pingCount(id0));
    assertEquals(2, s.pingCount(id1));

    s.ping(id0);
    s.ping(id0);
    assertEquals(4, s.pingCount(id0));
    s.ping(id1);
    assertEquals(3, s.pingCount(id1));

    Map<String, Object> map2 = s.connect("somebody.else.s.ip.address");
    Key id2 = Key.fromJSONString((String) map2.get("key"));

    s.ping(id2);
    assertEquals(2, s.pingCount(id2));
    assertEquals(3, s.pingCount(id1));
    assertEquals(4, s.pingCount(id0));

    s.close();
  }

  @Test
  public void connectPingTest() throws NoSuchPlayerException {
    Server s = new DummyServer();
    Map<String, Object> map0 = s.connect("my.ip.address");
    Key id0 = Key.fromJSONString((String) map0.get("key"));
    int player0 = (Integer) map0.get("player");

    assertTrue(s.ping(id0));
    assertEquals(ImmutableMap.of("connected", ImmutableMap.of(player0, true)),
      s.update(id0));

    Map<String, Object> map1 = s.connect("your.ip.address");
    Key id1 = Key.fromJSONString((String) map1.get("key"));
    int player1 = (Integer) map1.get("player");
    assertTrue(s.ping(id0));
    assertTrue(s.ping(id1));
    assertEquals(ImmutableMap.of("connected", ImmutableMap.of(player0, true,
      player1, true)),
        s.update(id0));

    assertEquals(ImmutableMap.of("connected", ImmutableMap.of(player0, true,
      player1, true)),
        s.update(id1));

    assertFalse(s.ping(id0));
    assertFalse(s.ping(id1));
  }

  @Test
  public void disconnectTest() throws NoSuchPlayerException {
    Server s = new DummyServer();
    Map<String, Object> map0 = s.connect("0.1.2.3");
    Map<String, Object> map1 = s.connect("1.2.3.4");
    Key id0 = Key.fromJSONString((String) map0.get("key"));
    Key id1 = Key.fromJSONString((String) map1.get("key"));
    int player0 = (Integer) map0.get("player");
    int player1 = (Integer) map1.get("player");

    s.update(id0);
    s.update(id1);

    try {
      Thread.sleep(1500);
    } catch (InterruptedException ex) {
      fail();
    }
    assertFalse(s.ping(id0));
    try {
      Thread.sleep(1500);
    } catch (InterruptedException ex) {
      fail();
    }
    assertTrue(s.ping(id0));
    assertEquals(ImmutableMap.of("connected", 
      ImmutableMap.of(player0, true, player1, false)),
        s.update(id0));
    assertFalse(s.ping(id0));

    s.close();
  }

  @Test
  public void updateTest() throws NoSuchPlayerException {
    Server s = new DummyServer();
    Map<String, Object> map0 = s.connect("0.1.2.3");
    Map<String, Object> map1 = s.connect("1.2.3.4");
    Key id0 = Key.fromJSONString((String) map0.get("key"));
    Key id1 = Key.fromJSONString((String) map1.get("key"));

    s.update(id0);
    s.update(id1);

    assertFalse(s.ping(id0));
    assertFalse(s.ping(id1));

    s.putField("someField", "someValue");

    assertTrue(s.ping(id0));
    assertTrue(s.ping(id1));

    assertEquals(s.update(id0), ImmutableMap.of("someField", "someValue"));
    assertFalse(s.ping(id0));
    assertTrue(s.ping(id1));

    assertEquals(s.update(id0), ImmutableMap.of());
    assertFalse(s.ping(id0));
    assertTrue(s.ping(id1));

    assertEquals(s.update(id1), ImmutableMap.of("someField", "someValue"));
    assertFalse(s.ping(id0));
    assertFalse(s.ping(id1));

    s.close();
  }

  @Test
  public void multipleUpdateTest() throws NoSuchPlayerException {
    Server s = new DummyServer();
    Map<String, Object> map0 = s.connect("0.1.2.3");
    Key id0 = Key.fromJSONString((String) map0.get("key"));
    s.update(id0);

    assertFalse(s.ping(id0));

    s.putField("someField", "someValue");
    assertTrue(s.ping(id0));
    s.putField("anotherField", "anotherValue");
    assertTrue(s.ping(id0));

    assertEquals(s.update(id0),
      ImmutableMap.of("someField", "someValue",
        "anotherField", "anotherValue"));
    assertFalse(s.ping(id0));

    s.putField("oneLastField", "oneLastValue");
    assertTrue(s.ping(id0));

    assertEquals(s.update(id0),
      ImmutableMap.of("oneLastField", "oneLastValue"));
    assertFalse(s.ping(id0));

    s.close();
  }

  @Test
  public void reUpdateTest() throws NoSuchPlayerException {
    Server s = new DummyServer();
    Map<String, Object> map0 = s.connect("my.ip");
    Key id0 = Key.fromJSONString((String) map0.get("key"));
    s.update(id0);

    assertFalse(s.ping(id0));
    s.putField("someField", "someValue");
    assertTrue(s.ping(id0));
    s.putField("someField", "someOtherValue");
    assertTrue(s.ping(id0));

    assertEquals(s.update(id0),
      ImmutableMap.of("someField", "someOtherValue"));
    assertFalse(s.ping(id0));

    assertEquals(s.update(id0), ImmutableMap.of());
    assertFalse(s.ping(id0));

    s.putField("someField", "someThirdValue");
    assertTrue(s.ping(id0));

    assertEquals(s.update(id0),
      ImmutableMap.of("someField", "someThirdValue"));
    assertFalse(s.ping(id0));

    s.close();
  }

  @Test
  public void outOfDateKeyTest() {
    Key.setActiveGeneration(-1);
    Key k = Key.generate();
    Server s = new DummyServer();

    assertEquals(-1, s.pingCount(k));
    try {
      s.ping(k);
      fail();
    } catch (NoSuchPlayerException ex) {
      assertTrue(true);
    }
    assertEquals(null, s.update(k));
  }

  @Test
  public void multipleConnectTest() {
    Server s = new DummyServer();

    Key k = Key.fromJSONString((String) s.connect("an.ip").get("key"));
    Map<String, Object> map = s.connect("an.ip");
    assertEquals("undefined", map.get("key"));
    assertEquals("undefined", map.get("player"));
    assertTrue(map.containsKey("alert"));

    s.disconnect(k);
    Key k2 = Key.fromJSONString((String) s.connect("an.ip").get("key"));
    assertEquals(k, k2);
  }

  @Test
  public void sealUnsealTest() throws NoSuchPlayerException {
    Server s = new DummyServer();
    Key k = Key.fromJSONString((String) s.connect("an.ip").get("key"));

    s.seal();
    Map<String, Object> map2 = s.connect("another.ip");
    assertEquals("undefined", map2.get("key"));
    assertEquals("undefined", map2.get("player"));
    assertTrue(map2.containsKey("alert"));
    assertTrue(s.ping(k));

    try {
      Thread.sleep(2500);
    } catch (InterruptedException ex) {
      fail();
    }

    assertTrue(s.ping(k));
    s.disconnect(k);
    assertEquals(k,
      Key.fromJSONString((String) s.connect("an.ip").get("key")));

    s.unseal();
    map2 = s.connect("another.ip");
    Key k2 = Key.fromJSONString((String) map2.get("key"));
    assertFalse("undefined".equals(map2.get("key")));

    assertTrue(s.ping(k));
    assertTrue(s.ping(k2));
  }

  @Test
  public void connectAndLoadTest() throws NoSuchPlayerException {
    Server s = new DummyServer();
    s.putField("someField", "someValue");
    Key k = Key.fromJSONString((String) s.connect("an.ip").get("key"));
    assertTrue(s.ping(k));
    Map<String, Object> updates = s.update(k);
    assertEquals(ImmutableSet.of("connected", "someField"), updates.keySet());
  }
}
