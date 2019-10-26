package com.github.arcizon.triton

import java.util.Properties

import scala.sys.process.{Process, ProcessLogger}
import scala.util.Try

/**
 * '''MDataClient''' class provides methods to interact with instance metadata deployed on Joyent Triton cloud.
 *
 * @constructor creates a new MDataClient connector instance
 * @param processLogger A log handler to log stdout and stderr events of the processes
 * @param binPath A prefix path for the mdata executable binaries, if they are not present in current PATH scope
 * @see [[https://github.com/joyent/mdata-client mdata-client]] for information on Joyent Triton mdata-client
 */
class MDataClient(processLogger: ProcessLogger, binPath: String) {
  private val mdata_list_cmd: String = binPath.concat("mdata-list")
  private val mdata_get_cmd: String = binPath.concat("mdata-get")
  private val mdata_put_cmd: String = binPath.concat("mdata-put")
  private val mdata_delete_cmd: String = binPath.concat("mdata-delete")

  /**
   * `put` method calls '''mdata-put''' binary as child process and returns the output as a boolean
   *
   * {{{
   * scala> mdataClient.put("version", "1.0")
   *
   * res1: Boolean = true
   *
   * scala> mdataClient.put("version", "")
   * java.lang.IllegalArgumentException: requirement failed: value argument can't be empty!!
   *   at scala.Predef$.require(Predef.scala:224)
   *   at com.github.arcizon.triton.MDataClient.put(MDataClient.scala:46)
   *   ... 32 elided
   * }}}
   *
   * @param key   input metadata key
   * @param value input metadata value for the `key`
   * @return a boolean, true for success and false for failure
   * @see [[https://smartos.org/man/1M/mdata-put mdata-put]] for more information on mdata-put command
   */
  def put(key: String, value: String): Boolean = {
    require(key.nonEmpty, "key argument can't be empty!!")
    require(value.nonEmpty, "value argument can't be empty!!")
    if (Process(mdata_put_cmd, Seq(key, value)).run(processLogger).exitValue() == 0) true else false
  }

  /**
   * `delete` method calls '''mdata-delete''' binary as child process and returns the output as a boolean
   *
   * {{{
   * scala> mdataClient.delete("version")
   *
   * res2: Boolean = true
   *
   * scala> mdataClient.delete("version")
   * res3: Boolean = false
   * }}}
   *
   * @param key input metadata key
   * @return a boolean, true for success and false for failure
   * @see [[https://smartos.org/man/1M/mdata-delete mdata-delete]] for more information on mdata-delete command
   */
  def delete(key: String): Boolean = {
    require(key.nonEmpty, "key argument can't be empty!!")
    if (!listKeys().contains(key)) {
      false
    } else {
      if (Process(mdata_delete_cmd, Seq(key)).run(processLogger).exitValue() == 0) true else false
    }
  }

  /**
   * `loadToSystemProperties` method loads the instance metadata as ''JVM System Properties''
   *
   * {{{
   * scala> mdataClient.loadToSystemProperties()
   * scala> System.getProperty("lifecycle")
   *
   * res16: String = devl
   * }}}
   *
   * @return an Unit
   */
  def loadToSystemProperties(): Unit = System.setProperties(asProperties())

  /**
   * `asProperties` method returns the instance metadata in the form of java ''Properties'' instance
   *
   * {{{
   * scala> mdataClient.asProperties()
   *
   * res14: java.util.Properties = {lifecycle=devl, component=abc}
   * }}}
   *
   * @return a ''java.util.Properties'' instance
   */
  def asProperties(): Properties = {
    val props: Properties = new Properties()
    listKeys().foreach(key => {
      val value: Option[String] = get(key)
      if (value.isDefined) props.setProperty(key, value.get)
    })
    props
  }

  /**
   * `listKeys` method calls '''mdata-list''' binary as child process and returns the output as list of string
   *
   * {{{
   * scala> mdataClient.listKeys()
   *
   * res0: List[String] = List(root_authorized_keys, lifecycle, component)
   * }}}
   *
   * @return a list of instance metadata keys
   * @see [[https://smartos.org/man/1M/mdata-list mdata-list]] for more information on mdata-list command
   */
  def listKeys(): List[String] = Process(mdata_list_cmd).lineStream(processLogger).toList

  /**
   * `get` method calls '''mdata-get''' binary as child process and returns the output as an optional string
   *
   * {{{
   * scala> mdataClient.get("lifecycle")
   *
   * res1: Option[String] = Some(devl)
   *
   * scala> mdataClient.get("dummy")
   *
   * res1: Option[String] = None
   * }}}
   *
   * @param key input metadata key
   * @return an optional string value for the input metadata key
   * @see [[https://smartos.org/man/1M/mdata-get mdata-get]] for more information on mdata-get command
   */
  def get(key: String): Option[String] = {
    require(key.nonEmpty, "key argument can't be empty!!")
    Try(Process(mdata_get_cmd, Seq(key)).lineStream(processLogger).headOption).getOrElse(None)
  }
}

/**
 * Factory for `MDataClient` instance to perform metadata operations on instance
 */
object MDataClient {
  /**
   * Creates a `MDataClient` instance with the binPath is set to empty on default assuming the binaries are in PATH
   * and printing stdout and stderr to console
   *
   * @return a [[MDataClient]] instance
   *
   * {{{
   * scala> import com.github.arcizon.triton.MDataClient
   * scala> val mdataClient: MDataClient = MDataClient()
   *
   * mdataClient: com.github.arcizon.triton.MDataClient = com.github.arcizon.triton.MDataClient@269f4bad
   * }}}
   */
  def apply(binPath: String = ""): MDataClient = apply(o => println(o), e => println(e), binPath)

  /**
   * Creates a `MDataClient` instance with provided log handlers for stdout and stderr
   *
   * @param stdout  The standard output log handler
   * @param stderr  The standard error log handler
   * @param binPath A prefix path for the mdata executable binaries, if they are not present in current PATH scope
   * @return a [[MDataClient]] instance
   *
   * {{{
   * scala> import org.apache.logging.log4j.{LogManager, Logger}
   * scala> val log: Logger = LogManager.getLogger(this.getClass.getName)
   *
   * scala> import com.github.arcizon.triton.MDataClient
   * scala> import scala.sys.process.ProcessLogger
   * scala> val mdataClient: MDataClient = MDataClient(stdout => log.info(stdout), stderr => log.error(stderr), "")
   *
   * mdataClient: com.github.arcizon.triton.MDataClient = com.github.arcizon.triton.MDataClient@269f4bad
   * }}}
   */
  def apply(stdout: String => Unit, stderr: String => Unit, binPath: String): MDataClient = apply(
    ProcessLogger(stdout, stderr),
    binPath
  )

  /**
   * Creates a `MDataClient` instance with provided binPath
   *
   * @param processLogger A log handler to log stdout and stderr events of the processes
   * @param binPath A prefix path for the mdata-* executable binaries, if they are not present in current PATH scope
   *                default to empty string assuming it is in current PATH scope.
   * @return a [[MDataClient]] instance
   *
   * {{{
   * scala> import org.apache.logging.log4j.{LogManager, Logger}
   * scala> val log: Logger = LogManager.getLogger(this.getClass.getName)
   *
   * scala> import com.github.arcizon.triton.MDataClient
   * scala> val mdataClient: MDataClient = MDataClient(ProcessLogger(stdout => log.info(stdout),
   * stderr => log.error(stderr)), "/usr/sbin/")
   *
   * mdataClient: com.github.arcizon.triton.MDataClient = com.github.arcizon.triton.MDataClient@269f4bad
   * }}}
   */
  def apply(processLogger: ProcessLogger, binPath: String): MDataClient = new MDataClient(processLogger, binPath)
}