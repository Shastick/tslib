package io.sqooba.oss.timeseries

import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel

import fi.iki.yak.ts.compression.gorilla.{GorillaDecompressor, LongArrayInput}

import scala.util.Try

/** Provides abstraction and tools for compressing/archiving timeseries data. The
  * compression used is Gorilla TSC encoding implemented by a Java library
  * [[fi.iki.yak.ts.compression.gorilla]].
  *
  * There are three main types:
  *   - [[io.sqooba.oss.timeseries.archive.GorillaArray]]:
  *     a simple byte array that represents a compressed/encoded map of
  *     (timestamp, value) tuples.
  *
  *   - [[io.sqooba.oss.timeseries.archive.GorillaBlock]]:
  *     the representation of a compressed/encoded TimeSeries as defined in this library.
  *     A GorillaBlock consists of two GorillaArrays one for the values, one for the
  *     validities.
  *
  *   - [[io.sqooba.oss.timeseries.archive.GorillaSuperBlock]]:
  *     Groups many GorillaBlocks of the same TimeSeries into a larger binary format/file.
  *     This allows compression of a timeseries that spans a very long range of time.
  *
  * There are additional helpers for constructing those representations.
  *
  * @note The only supported type for the values of the TSEntries at the moment is
  * Double. This can lead to precision problems if you have very high long  values
  * that you convert to double and pass to the compression.
  */
// TODO make compression generic for all numeric types. This should be possible because
//   all numeric types can be represented by the 64-bits of a Long.
//   Tracked by T547.
package object archive {

  /** Represents a gorilla encoded series of (timestamp, value) tuples, without validities.
    * It is just an array of bytes.
    */
  type GorillaArray = Array[Byte]

  /** Read an integer from the channel (big-endian byte order).
    * @param channel a seekable byte channel
    * @param offsetToEnd the number of bytes between the end of the integer bytes
    *                    and the end of the channel
    */
  private[archive] def readIntFromEnd(channel: SeekableByteChannel, offsetToEnd: Long): Int =
    ByteBuffer
      .wrap(
        readBytesFromEnd(channel, offsetToEnd, Integer.BYTES)
      )
      .getInt

  /** Read a block of bytes from the end of the stream.
    * @param offsetToEnd the number of bytes between the end of the block bytes
    *                    and the end of the channel
    */
  private[archive] def readBytesFromEnd(channel: SeekableByteChannel, offsetToEnd: Long, length: Int): Array[Byte] =
    readBytes(channel, channel.size() - offsetToEnd - length, length)

  /** Read a block of bytes from the channel. */
  private[archive] def readBytes(channel: SeekableByteChannel, offset: Long, length: Int): Array[Byte] = {
    channel.position(offset)
    val buffer = ByteBuffer.allocate(length)

    require(channel.read(buffer) == length, "There is not enough data on the channel.")

    buffer.flip()
    buffer.array()
  }

  /** Helper function
    * @param longs in an array
    * @return the same array but as an array of bytes
    */
  private[archive] def longArray2byteArray(longs: Array[Long]): Array[Byte] =
    longs
      .foldLeft(ByteBuffer.allocate(java.lang.Long.BYTES * longs.length))(
        (buffer, long) => buffer.putLong(long)
      )
      .array()

  /** Helper function
    * @param bytes in an array
    * @return the same array but as an array of longs
    */
  private[archive] def byteArray2longArray(bytes: Array[Byte]): Array[Long] = {
    val buffer = ByteBuffer.wrap(bytes)

    Array.fill(bytes.length / java.lang.Long.BYTES) { buffer.getLong }
  }

  private[archive] def int2byteArray(int: Int): Array[Byte] =
    ByteBuffer.allocate(java.lang.Integer.BYTES).putInt(int).array()

  private[archive] def byteArray2Int(array: Array[Byte]): Int =
    ByteBuffer.wrap(array).getInt

  private[archive] def wrapTryDecompressor(bytes: GorillaArray): Try[GorillaDecompressor] =
    Try(new GorillaDecompressor(new LongArrayInput(byteArray2longArray(bytes))))

}
