package com.github.arcizon.triton

import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

class MDataClientTest extends FunSuite with BeforeAndAfterAll with Matchers {
  var mdataClient: MDataClient = _

  override def beforeAll(): Unit = {
    super.beforeAll()

    mdataClient = MDataClient(this.getClass.getResource("/mockscripts").getPath.concat("/"))
  }

  test("mdata-list should return a list of metadata keys") {
    assert(mdataClient.listKeys() === List("root_authorized_keys", "lifecycle", "component"))
  }

  test("mdata-get requires a non-empty metadata key argument") {
    assertThrows[IllegalArgumentException](mdataClient.get(""))
  }

  test("mdata-get for metadata key lifecycle should return optional string value as test") {
    assert(mdataClient.get("lifecycle") === Some("test"))
  }

  test("mdata-get for unavailable metadata key dummy should return optional value None") {
    assert(mdataClient.get("dummy") === None)
  }

  test("mdata-put requires non-empty arguments") {
    assertThrows[IllegalArgumentException](mdataClient.put("username", ""))
  }

  test("mdata-put for metadata key username with arcizon should return boolean true on success") {
    assert(mdataClient.put("username", "arcizon"))
  }

  test("mdata-put on replacing available metadata key's value returns true") {
    assert(mdataClient.put("component", "scalatest"))
  }

  test("mdata-delete requires a non-empty metadata key argument") {
    assertThrows[IllegalArgumentException](mdataClient.delete(""))
  }

  test("mdata-delete on existing metadata key returns true") {
    assert(mdataClient.delete("component"))
  }

  test("mdata-delete on non existing metadata key returns false") {
    assert(!mdataClient.delete("dummy"))
  }
}
