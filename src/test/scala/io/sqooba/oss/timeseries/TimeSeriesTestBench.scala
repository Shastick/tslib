package io.sqooba.oss.timeseries

import java.util.concurrent.TimeUnit
import io.sqooba.oss.timeseries.immutable.{ContiguousTimeDomain, EmptyTimeSeries, TSEntry}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

// scalastyle:off magic.number
// scalastyle:off file.size.limit

trait TimeSeriesTestBench extends should.Matchers { this: AnyFlatSpec =>

  /**
   * *Main test bench* for a timeseries implementation. This tests all the functions
   * defined by the TimeSeries trait for a non empty and non singleton (TSEntry)
   * timeseries implementation. All the tests use double valued series.
   *
   * @note The mapping functions are only tested without compression. Use
   *       'nonEmptyNonSingletonDoubleTimeSeriesWithCompression' to test that.
   *
   * @param newTs constructor method for the timeseries implementation to test
   */
  def nonEmptyNonSingletonDoubleTimeSeries(newTs: Seq[TSEntry[Double]] => TimeSeries[Double]): Unit = {

    // Two contiguous entries
    val contig2 = newTs(Seq(TSEntry(1, 111d, 10), TSEntry(11, 222d, 10)))

    // Two entries with a gap in between
    val discon2 = newTs(Seq(TSEntry(1, 111d, 10), TSEntry(12, 222d, 10)))

    // Three entries, gap between first and second
    val three = newTs(Seq(TSEntry(1, 111d, 10), TSEntry(12, 222d, 10), TSEntry(22, 333d, 10)))

    val anotherThree = newTs(Seq(TSEntry(1, 111d, 9), TSEntry(10, 222d, 10), TSEntry(20, 444d, 10)))

    val tri = newTs(Seq(TSEntry(1, 111d, 10), TSEntry(12, 222d, 10), TSEntry(22, 333d, 10)))

    it should "give correct values for at()" in {

      // Check two contiguous values
      assert(2 === contig2.size)
      assert(contig2.nonEmpty)
      assert(contig2.at(0).isEmpty)
      assert(contig2.at(1).contains(111d))
      assert(contig2.at(10).contains(111d))
      assert(contig2.at(11).contains(222d))
      assert(contig2.at(20).contains(222d))
      assert(contig2.at(21).isEmpty)

      // Check two non contiguous values
      assert(discon2.size === 2)
      assert(discon2.at(0).isEmpty)
      assert(discon2.at(1).contains(111d))
      assert(discon2.at(10).contains(111d))
      assert(discon2.at(11).isEmpty)
      assert(discon2.at(12).contains(222d))
      assert(discon2.at(21).contains(222d))
      assert(discon2.at(22).isEmpty)
    }

    it should "be correctly defined" in {
      // Check two contiguous values
      assert(!contig2.defined(0))
      assert(contig2.defined(1))
      assert(contig2.defined(10))
      assert(contig2.defined(11))
      assert(contig2.defined(20))
      assert(!contig2.defined(21))

      // Check two non contiguous values
      assert(!discon2.defined(0))
      assert(discon2.defined(1))
      assert(discon2.defined(10))
      assert(!discon2.defined(11))
      assert(discon2.defined(12))
      assert(discon2.defined(21))
      assert(!discon2.defined(22))
    }

    it should "correctly trim on the left for contiguous entries" in {
      // Two contiguous entries
      // Left of the domain
      assert(contig2.entries === contig2.trimLeft(0).entries)
      assert(contig2.entries === contig2.trimLeft(1).entries)

      // Trimming on the first entry
      assert(Seq(TSEntry(2, 111d, 9), TSEntry(11, 222d, 10)) === contig2.trimLeft(2).entries)
      assert(Seq(TSEntry(10, 111d, 1), TSEntry(11, 222d, 10)) === contig2.trimLeft(10).entries)

      // Trimming at the boundary between entries:
      assert(Seq(TSEntry(11, 222d, 10)) === contig2.trimLeft(11).entries)

      // ... and on the second entry:
      assert(Seq(TSEntry(12, 222d, 9)) === contig2.trimLeft(12).entries)
      assert(Seq(TSEntry(20, 222d, 1)) === contig2.trimLeft(20).entries)

      // ... and after the second entry:
      assert(contig2.trimLeft(21).isEmpty)
    }

    it should "correctly trim on the left for not contiguous entries" in {
      // Two non-contiguous entries
      // Trimming left of the first entry
      assert(discon2.entries === discon2.trimLeft(0).entries)
      assert(discon2.entries === discon2.trimLeft(1).entries)

      // Trimming on the first entry
      assert(Seq(TSEntry(2, 111d, 9), TSEntry(12, 222d, 10)) === discon2.trimLeft(2).entries)
      assert(Seq(TSEntry(10, 111d, 1), TSEntry(12, 222d, 10)) === discon2.trimLeft(10).entries)

      // Trimming between entries:
      assert(Seq(TSEntry(12, 222d, 10)) === discon2.trimLeft(11).entries)
      assert(Seq(TSEntry(12, 222d, 10)) === discon2.trimLeft(12).entries)

      // ... and on the second
      assert(Seq(TSEntry(13, 222d, 9)) === discon2.trimLeft(13).entries)
      assert(Seq(TSEntry(21, 222d, 1)) === discon2.trimLeft(21).entries)

      // ... and after the second entry:
      assert(discon2.trimLeft(22).isEmpty)

      // Trim on a three element time series with a discontinuity
      // Left of the first entry
      assert(three.entries === three.trimLeft(0).entries)
      assert(three.entries === three.trimLeft(1).entries)

      // Trimming on the first entry
      assert(
        Seq(TSEntry(2, 111d, 9), TSEntry(12, 222d, 10), TSEntry(22, 333d, 10)) ===
            three.trimLeft(2).entries
      )
      assert(
        Seq(TSEntry(10, 111d, 1), TSEntry(12, 222d, 10), TSEntry(22, 333d, 10)) ===
            three.trimLeft(10).entries
      )

      // Trimming between entries:
      assert(Seq(TSEntry(12, 222d, 10), TSEntry(22, 333d, 10)) === three.trimLeft(11).entries)
      assert(Seq(TSEntry(12, 222d, 10), TSEntry(22, 333d, 10)) === three.trimLeft(12).entries)

      // ... and on the second
      assert(Seq(TSEntry(13, 222d, 9), TSEntry(22, 333d, 10)) === three.trimLeft(13).entries)
      assert(Seq(TSEntry(21, 222d, 1), TSEntry(22, 333d, 10)) === three.trimLeft(21).entries)

      // ... on the border between second and third
      assert(Seq(TSEntry(22, 333d, 10)) === three.trimLeft(22).entries)
      // on the third
      assert(Seq(TSEntry(23, 333d, 9)) === three.trimLeft(23).entries)
      assert(Seq(TSEntry(31, 333d, 1)) === three.trimLeft(31).entries)

      // ... and after every entry.
      assert(three.trimLeft(32).isEmpty)
    }

    it should "correctly trim on the left for discrete entries" in {
      // Two contiguous entries
      // Left of the domain
      assert(contig2.entries === contig2.trimLeftDiscrete(0, true).entries)
      assert(contig2.entries === contig2.trimLeftDiscrete(0, false).entries)
      assert(contig2.entries === contig2.trimLeftDiscrete(1, true).entries)
      assert(contig2.entries === contig2.trimLeftDiscrete(1, false).entries)

      // Trimming on the first entry
      assert(contig2.entries === contig2.trimLeftDiscrete(2, true).entries)
      assert(Seq(TSEntry(11, 222d, 10)) === contig2.trimLeftDiscrete(2, false).entries)
      assert(contig2.entries === contig2.trimLeftDiscrete(10, true).entries)
      assert(Seq(TSEntry(11, 222d, 10)) === contig2.trimLeftDiscrete(2, false).entries)

      // Trimming at the boundary between entries:
      assert(Seq(TSEntry(11, 222d, 10)) === contig2.trimLeftDiscrete(11, true).entries)
      assert(Seq(TSEntry(11, 222d, 10)) === contig2.trimLeftDiscrete(11, false).entries)

      // ... and on the second entry:
      assert(Seq(TSEntry(11, 222d, 10)) === contig2.trimLeftDiscrete(12, true).entries)
      assert(Seq() === contig2.trimLeftDiscrete(12, false).entries)
      assert(Seq(TSEntry(11, 222d, 10)) === contig2.trimLeftDiscrete(20, true).entries)
      assert(Seq() === contig2.trimLeftDiscrete(20, false).entries)

      // ... and after the second entry:
      assert(contig2.trimLeftDiscrete(21, true).isEmpty)
      assert(contig2.trimLeftDiscrete(21, false).isEmpty)
    }

    it should "correctly trim on the right for contiguous entries" in {

      // Two contiguous entries:
      // Right of the domain:
      assert(contig2.entries === contig2.trimRight(22).entries)
      assert(contig2.entries === contig2.trimRight(21).entries)

      // On the second entry
      contig2.trimRight(20).entries should contain theSameElementsInOrderAs
        Seq(TSEntry(1, 111d, 10), TSEntry(11, 222d, 9))

      contig2.trimRight(12).entries should contain theSameElementsInOrderAs
        Seq(TSEntry(1, 111d, 10), TSEntry(11, 222d, 1))

      // On the boundary
      contig2.trimRight(11).entries should contain theSameElementsInOrderAs
        Seq(TSEntry(1, 111d, 10))

      // On the first entry
      assert(TSEntry(1, 111d, 9) === contig2.trimRight(10))
      assert(TSEntry(1, 111d, 1) === contig2.trimRight(2))

      // Before the first entry
      assert(contig2.trimRight(1).isEmpty)
      assert(contig2.trimRight(0).isEmpty)

    }

    it should "correctly trim on the right for not contiguous entries" in {
      // Two non-contiguous entries
      // Trimming right of the second entry
      assert(discon2.entries === discon2.trimRight(23).entries)
      assert(discon2.entries === discon2.trimRight(22).entries)

      // Trimming on the last entry
      discon2.trimRight(21).entries should contain theSameElementsInOrderAs
        Seq(TSEntry(1, 111d, 10), TSEntry(12, 222d, 9))
      discon2.trimRight(13).entries should contain theSameElementsInOrderAs
        Seq(TSEntry(1, 111d, 10), TSEntry(12, 222d, 1))

      // Trimming between entries:
      discon2.trimRight(12).entries should contain theSameElementsInOrderAs
        Seq(TSEntry(1, 111d, 10))
      discon2.trimRight(11).entries should contain theSameElementsInOrderAs
        Seq(TSEntry(1, 111d, 10))

      // ... and on the first
      discon2.trimRight(10).entries should contain theSameElementsInOrderAs
        Seq(TSEntry(1, 111d, 9))
      discon2.trimRight(2).entries should contain theSameElementsInOrderAs
        Seq(TSEntry(1, 111d, 1))

      // ... and before the first entry:
      assert(discon2.trimRight(1).isEmpty)
      assert(discon2.trimRight(0).isEmpty)

      // Trim on a three element time series with a discontinuity
      // Right of the last entry
      assert(three.entries === three.trimRight(33).entries)
      assert(three.entries === three.trimRight(32).entries)

      // Trimming on the last entry
      assert(
        Seq(TSEntry(1, 111d, 10), TSEntry(12, 222d, 10), TSEntry(22, 333d, 9)) ===
            three.trimRight(31).entries
      )
      assert(
        Seq(TSEntry(1, 111d, 10), TSEntry(12, 222d, 10), TSEntry(22, 333d, 1)) ===
            three.trimRight(23).entries
      )

      // Trimming between 2nd and 3rd entries:
      assert(Seq(TSEntry(1, 111d, 10), TSEntry(12, 222d, 10)) === three.trimRight(22).entries)

      // ... and on the second
      assert(Seq(TSEntry(1, 111d, 10), TSEntry(12, 222d, 9)) === three.trimRight(21).entries)
      assert(Seq(TSEntry(1, 111d, 10), TSEntry(12, 222d, 1)) === three.trimRight(13).entries)

      // ... on the border between 1st and 2nd
      assert(Seq(TSEntry(1, 111d, 10)) === three.trimRight(12).entries)
      assert(Seq(TSEntry(1, 111d, 10)) === three.trimRight(11).entries)

      // ... on the first
      assert(Seq(TSEntry(1, 111d, 9)) === three.trimRight(10).entries)
      assert(Seq(TSEntry(1, 111d, 1)) === three.trimRight(2).entries)

      // ... and after every entry.
      assert(three.trimRight(1).isEmpty)
      assert(three.trimRight(0).isEmpty)
    }

    it should "correctly trim on the right for discrete entries" in {
      // Two contiguous entries:
      // Right of the domain:
      assert(contig2.entries === contig2.trimRightDiscrete(22, true).entries)
      assert(contig2.entries === contig2.trimRightDiscrete(22, false).entries)
      assert(contig2.entries === contig2.trimRightDiscrete(21, true).entries)
      assert(contig2.entries === contig2.trimRightDiscrete(21, false).entries)

      // On the second entry
      assert(Seq(TSEntry(1, 111d, 10), TSEntry(11, 222d, 10)) === contig2.trimRightDiscrete(20, true).entries)
      assert(Seq(TSEntry(1, 111d, 10)) === contig2.trimRightDiscrete(20, false).entries)
      assert(Seq(TSEntry(1, 111d, 10), TSEntry(11, 222d, 10)) === contig2.trimRightDiscrete(12, true).entries)
      assert(Seq(TSEntry(1, 111d, 10)) === contig2.trimRightDiscrete(12, false).entries)

      // On the boundary
      assert(Seq(TSEntry(1, 111d, 10)) === contig2.trimRightDiscrete(11, true).entries)
      assert(Seq(TSEntry(1, 111d, 10)) === contig2.trimRightDiscrete(11, false).entries)

      // On the first entry
      assert(TSEntry(1, 111d, 10) === contig2.trimRightDiscrete(10, true))
      assert(contig2.trimRightDiscrete(2, false).isEmpty)

      // Before the first entry
      assert(contig2.trimRightDiscrete(1, true).isEmpty)
      assert(contig2.trimRightDiscrete(1, false).isEmpty)
      assert(contig2.trimRightDiscrete(0, true).isEmpty)
      assert(contig2.trimRightDiscrete(0, false).isEmpty)
    }

    it should "correctly split a timeseries of three entries" in {
      val (l1, r1) = anotherThree.split(-1)
      l1 shouldBe EmptyTimeSeries
      r1.entries shouldBe anotherThree.entries

      val (l2, r2) = anotherThree.split(1)
      l2 shouldBe EmptyTimeSeries
      r2.entries shouldBe anotherThree.entries

      val (l3, r3) = anotherThree.split(1)
      l3.entries shouldBe anotherThree.trimRight(1).entries
      r3.entries shouldBe anotherThree.trimLeft(1).entries

      val (l4, r4) = anotherThree.split(9)
      l4.entries shouldBe anotherThree.trimRight(9).entries
      r4.entries shouldBe anotherThree.trimLeft(9).entries

      val (l5, r5) = anotherThree.split(10)
      l5.entries shouldBe anotherThree.trimRight(10).entries
      r5.entries shouldBe anotherThree.trimLeft(10).entries

      val (l6, r6) = anotherThree.split(11)
      l6.entries shouldBe anotherThree.trimRight(11).entries
      r6.entries shouldBe anotherThree.trimLeft(11).entries

      val (l7, r7) = anotherThree.split(19)
      l7.entries shouldBe anotherThree.trimRight(19).entries
      r7.entries shouldBe anotherThree.trimLeft(19).entries

      val (l8, r8) = anotherThree.split(20)
      l8.entries shouldBe anotherThree.trimRight(20).entries
      r8.entries shouldBe anotherThree.trimLeft(20).entries

      val (l9, r9) = anotherThree.split(21)
      l9.entries shouldBe anotherThree.trimRight(21).entries
      r9.entries shouldBe anotherThree.trimLeft(21).entries

      val (l10, r10) = anotherThree.split(29)
      l10.entries shouldBe anotherThree.trimRight(29).entries
      r10.entries shouldBe anotherThree.trimLeft(29).entries

      val (l11, r11) = anotherThree.split(30)
      l11.entries shouldBe anotherThree.entries
      r11 shouldBe EmptyTimeSeries

      val (l12, r12) = anotherThree.split(31)
      l12.entries shouldBe anotherThree.entries
      r12 shouldBe EmptyTimeSeries
    }

    it should "correctly map a timeseries of three entries" in {
      val up = anotherThree.map(_.toString + "asdf")
      assert(up.size === 3)
      assert(up.at(1).contains("111.0asdf"))
      assert(up.at(10).contains("222.0asdf"))
      assert(up.at(20).contains("444.0asdf"))
    }

    it should "correctly map a timeseries of three entries without compression" in {
      val up = anotherThree.map(s => 42, compress = false)
      assert(up.entries === Seq(TSEntry(1, 42, 9), TSEntry(10, 42, 10), TSEntry(20, 42, 10)))
    }

    it should "correctly map with time a timeseries of three entries" in {
      val up = anotherThree.mapEntries(e => e.value.toString + "_" + e.timestamp)
      assert(3 === up.size)
      assert(up.at(1).contains("111.0_1"))
      assert(up.at(10).contains("222.0_10"))
      assert(up.at(20).contains("444.0_20"))
    }

    it should "correctly map with time a timeseries of three entries without compression" in {
      val up = anotherThree.mapEntries(_ => 42, compress = false)
      assert(up.entries === Seq(TSEntry(1, 42, 9), TSEntry(10, 42, 10), TSEntry(20, 42, 10)))
    }

    it should "correctly filter a timeseries of three entries" in {
      val ts = newTs(Seq(TSEntry(1, 111d, 9), TSEntry(15, 222d, 15), TSEntry(30, 444d, 20)))
      assert(
        ts.filterEntries(_.timestamp < 15) === TSEntry(1, 111d, 9)
      )
      assert(
        ts.filterEntries(_.validity > 10).entries === Seq(TSEntry(15, 222d, 15), TSEntry(30, 444d, 20))
      )
      assert(
        ts.filterEntries(_.value > 10).entries === ts.entries
      )
      assert(
        ts.filterEntries(_.value < 0) === EmptyTimeSeries
      )
    }

    it should "correctly filter the values of a timeseries of three entries" in {
      val ts = newTs(Seq(TSEntry(1, 111d, 9), TSEntry(15, 222d, 15), TSEntry(30, 444d, 20)))

      assert(
        ts.filter(_ > 10).entries === ts.entries
      )
      assert(
        ts.filter(_ < 0) === EmptyTimeSeries
      )
    }

    it should "filter & map the values of a timeseries of three entries" in {
      val ts = newTs(Seq(TSEntry(1, 111d, 9), TSEntry(15, 222d, 15), TSEntry(30, 444d, 20)))
      ts.filterMap(
          v => if (v > 300) Some(2 * v) else None,
          compress = false
        )
        .entries shouldBe Seq(TSEntry(30, 888d, 20))
    }

    it should "filter & map the entries of a timeseries of three entries" in {
      val ts = newTs(Seq(TSEntry(1, 111d, 9), TSEntry(15, 222d, 15), TSEntry(30, 444d, 20)))

      ts.filterMapEntries(
          entry => if ((entry.value - entry.validity) % 2 == 0) Some(entry.timestamp) else None,
          compress = false
        )
        .entries shouldBe Seq(TSEntry(1, 1L, 9), TSEntry(30, 30L, 20))
    }

    it should "not fill a contiguous timeseries of three entries" in {
      val tri = anotherThree
      assert(tri.fill(333d).entries === tri.entries)
    }

    it should "fill a timeseries of three entries" in {
      val tri = newTs(Seq(TSEntry(1, 111d, 9), TSEntry(20, 222d, 10), TSEntry(40, 444d, 10)))

      assert(
        tri.fill(333d).entries ===
            Seq(
              TSEntry(1, 111d, 9),
              TSEntry(10, 333d, 10),
              TSEntry(20, 222d, 10),
              TSEntry(30, 333d, 10),
              TSEntry(40, 444d, 10)
            )
      )

      assert(
        tri.fill(111d).entries ===
            Seq(
              TSEntry(1, 111d, 19),
              TSEntry(20, 222d, 10),
              TSEntry(30, 111d, 10),
              TSEntry(40, 444d, 10)
            )
      )

      assert(
        tri.fill(222d).entries ===
            Seq(
              TSEntry(1, 111d, 9),
              TSEntry(10, 222d, 30),
              TSEntry(40, 444d, 10)
            )
      )

      assert(
        tri.fill(444d).entries ===
            Seq(
              TSEntry(1, 111d, 9),
              TSEntry(10, 444d, 10),
              TSEntry(20, 222d, 10),
              TSEntry(30, 444d, 20)
            )
      )
    }

    it should "return the correct values" in {
      assert(tri.values == tri.entries.map(_.value))
    }

    it should "return the correct head" in {
      assert(tri.head === TSEntry(1, 111d, 10))
    }

    it should "return the correct head option" in {
      assert(tri.headOption.contains(TSEntry(1, 111d, 10)))
    }

    it should "return the correct head value" in {
      assert(tri.headValue === 111d)
    }

    it should "return the correct head value option" in {
      assert(tri.headValueOption.contains(111d))
    }

    it should "return the correct last" in {
      assert(tri.last === TSEntry(22, 333d, 10))
    }

    it should "return the correct last option" in {
      assert(tri.lastOption.contains(TSEntry(22, 333d, 10)))
    }

    it should "return the correct last value" in {
      assert(tri.lastValue === 333d)
    }

    it should "return the correct last value option" in {
      assert(tri.lastValueOption.contains(333d))
    }

    it should "append entries correctly" in {
      val tri =
        newTs(Seq(TSEntry(1, 111d, 10), TSEntry(11, 222d, 10), TSEntry(21, 444d, 10)))

      // Appending after...
      tri.append(TSEntry(32, "Hy", 10), compress = false).entries shouldBe
        Seq(TSEntry(1, 111d, 10), TSEntry(11, 222d, 10), TSEntry(21, 444d, 10), TSEntry(32, "Hy", 10))

      tri.append(TSEntry(31, "Hy", 10), compress = false).entries shouldBe
        Seq(TSEntry(1, 111d, 10), TSEntry(11, 222d, 10), TSEntry(21, 444d, 10), TSEntry(31, "Hy", 10))

      // Appending on last entry
      tri.append(TSEntry(30, "Hy", 10), compress = false).entries shouldBe
        Seq(TSEntry(1, 111d, 10), TSEntry(11, 222d, 10), TSEntry(21, 444d, 9), TSEntry(30, "Hy", 10))

      tri.append(TSEntry(22, "Hy", 10), compress = false).entries shouldBe
        Seq(TSEntry(1, 111d, 10), TSEntry(11, 222d, 10), TSEntry(21, 444d, 1), TSEntry(22, "Hy", 10))

      // ... just after and on second entry
      tri.append(TSEntry(21, "Hy", 10), compress = false).entries shouldBe
        Seq(TSEntry(1, 111d, 10), TSEntry(11, 222d, 10), TSEntry(21, "Hy", 10))

      tri.append(TSEntry(20, "Hy", 10), compress = false).entries shouldBe
        Seq(TSEntry(1, 111d, 10), TSEntry(11, 222d, 9), TSEntry(20, "Hy", 10))

      tri.append(TSEntry(12, "Hy", 10), compress = false).entries shouldBe
        Seq(TSEntry(1, 111d, 10), TSEntry(11, 222d, 1), TSEntry(12, "Hy", 10))

      // ... just after and on first
      tri.append(TSEntry(11, "Hy", 10), compress = false).entries shouldBe
        Seq(TSEntry(1, 111d, 10), TSEntry(11, "Hy", 10))

      tri.append(TSEntry(10, "Hy", 10), compress = false).entries shouldBe
        Seq(TSEntry(1, 111d, 9), TSEntry(10, "Hy", 10))

      tri.append(TSEntry(2, "Hy", 10), compress = false).entries shouldBe
        Seq(TSEntry(1, 111d, 1), TSEntry(2, "Hy", 10))

      // And complete override
      tri.append(TSEntry(1, "Hy", 10), compress = false).entries shouldBe
        Seq(TSEntry(1, "Hy", 10))
    }

    it should "prepend entries correctly" in {
      val tri =
        newTs(Seq(TSEntry(5, 111d, 6), TSEntry(11, 222d, 10), TSEntry(21, 444d, 10)))

      // Prepending before...
      tri.prepend(TSEntry(1, "Hy", 3), compress = false).entries shouldBe
        Seq(TSEntry(1, "Hy", 3), TSEntry(5, 111d, 6), TSEntry(11, 222d, 10), TSEntry(21, 444d, 10))

      tri.prepend(TSEntry(2, "Hy", 3), compress = false).entries shouldBe
        Seq(TSEntry(2, "Hy", 3), TSEntry(5, 111d, 6), TSEntry(11, 222d, 10), TSEntry(21, 444d, 10))

      // Overlaps with first entry
      tri.prepend(TSEntry(1, "Hy", 5), compress = false).entries shouldBe
        Seq(TSEntry(1, "Hy", 5), TSEntry(6, 111d, 5), TSEntry(11, 222d, 10), TSEntry(21, 444d, 10))

      tri.prepend(TSEntry(5, "Hy", 5), compress = false).entries shouldBe
        Seq(TSEntry(5, "Hy", 5), TSEntry(10, 111d, 1), TSEntry(11, 222d, 10), TSEntry(21, 444d, 10))

      tri.prepend(TSEntry(1, "Hy", 10), compress = false).entries shouldBe
        Seq(TSEntry(1, "Hy", 10), TSEntry(11, 222d, 10), TSEntry(21, 444d, 10))

      // ... second entry
      tri.prepend(TSEntry(2, "Hy", 10), compress = false).entries shouldBe
        Seq(TSEntry(2, "Hy", 10), TSEntry(12, 222d, 9), TSEntry(21, 444d, 10))

      tri.prepend(TSEntry(10, "Hy", 10), compress = false).entries shouldBe
        Seq(TSEntry(10, "Hy", 10), TSEntry(20, 222d, 1), TSEntry(21, 444d, 10))

      tri.prepend(TSEntry(11, "Hy", 10), compress = false).entries shouldBe
        Seq(TSEntry(11, "Hy", 10), TSEntry(21, 444d, 10))

      // ... third entry
      tri.prepend(TSEntry(12, "Hy", 10), compress = false).entries shouldBe
        Seq(TSEntry(12, "Hy", 10), TSEntry(22, 444d, 9))

      tri.prepend(TSEntry(20, "Hy", 10), compress = false).entries shouldBe
        Seq(TSEntry(20, "Hy", 10), TSEntry(30, 444d, 1))

      // Complete override
      tri.prepend(TSEntry(21, "Hy", 10), compress = false).entries shouldBe
        Seq(TSEntry(21, "Hy", 10))

      tri.prepend(TSEntry(22, "Hy", 10), compress = false).entries shouldBe
        Seq(TSEntry(22, "Hy", 10))
    }

    def testTs(startsAt: Long): TimeSeries[Double] =
      newTs(
        Seq(
          TSEntry(startsAt, 123d, 10),
          TSEntry(startsAt + 10, 234d, 10),
          TSEntry(startsAt + 20, 345d, 10)
        )
      )

    it should "append a multi-entry TS at various times on the entry" in {
      val tri =
        newTs(Seq(TSEntry(1, 111d, 10), TSEntry(11, 222d, 10), TSEntry(21, 444d, 10)))

      // Append after all entries

      tri.append(testTs(31), compress = false).entries shouldBe
        tri.entries ++ testTs(31).entries

      tri.append(testTs(32), compress = false).entries shouldBe
        tri.entries ++ testTs(32).entries

      // On last
      tri.append(testTs(30), compress = false).entries shouldBe
        Seq(TSEntry(1, 111d, 10), TSEntry(11, 222d, 10), TSEntry(21, 444d, 9)) ++ testTs(30).entries

      tri.append(testTs(22), compress = false).entries shouldBe
        Seq(TSEntry(1, 111d, 10), TSEntry(11, 222d, 10), TSEntry(21, 444d, 1)) ++ testTs(22).entries

      tri.append(testTs(21), compress = false).entries shouldBe
        Seq(TSEntry(1, 111d, 10), TSEntry(11, 222d, 10)) ++ testTs(21).entries

      // On second
      tri.append(testTs(20), compress = false).entries shouldBe
        Seq(TSEntry(1, 111d, 10), TSEntry(11, 222d, 9)) ++ testTs(20).entries

      tri.append(testTs(12), compress = false).entries shouldBe
        Seq(TSEntry(1, 111d, 10), TSEntry(11, 222d, 1)) ++ testTs(12).entries

      tri.append(testTs(11), compress = false).entries shouldBe
        Seq(TSEntry(1, 111d, 10)) ++ testTs(11).entries

      // On first
      tri.append(testTs(10), compress = false).entries shouldBe
        Seq(TSEntry(1, 111d, 9)) ++ testTs(10).entries

      tri.append(testTs(2), compress = false).entries shouldBe
        Seq(TSEntry(1, 111d, 1)) ++ testTs(2).entries

      tri.append(testTs(1), compress = false).entries shouldBe
        testTs(1).entries
    }

    it should "prepend a multi-entry TS at various times on the entry" in {
      val tri =
        newTs(Seq(TSEntry(101, 111d, 10), TSEntry(111, 222d, 10), TSEntry(121, 444d, 10)))

      // Before all entries
      tri.prepend(testTs(70), compress = false).entries shouldBe
        testTs(70).entries ++ tri.entries

      tri.prepend(testTs(71), compress = false).entries shouldBe
        testTs(71).entries ++ tri.entries

      // On first
      tri.prepend(testTs(72), compress = false).entries shouldBe
        testTs(72).entries ++ Seq(TSEntry(102, 111d, 9), TSEntry(111, 222d, 10), TSEntry(121, 444d, 10))

      tri.prepend(testTs(80), compress = false).entries shouldBe
        testTs(80).entries ++ Seq(TSEntry(110, 111d, 1), TSEntry(111, 222d, 10), TSEntry(121, 444d, 10))

      tri.prepend(testTs(81), compress = false).entries shouldBe
        testTs(81).entries ++ Seq(TSEntry(111, 222d, 10), TSEntry(121, 444d, 10))

      // On second
      tri.prepend(testTs(82), compress = false).entries shouldBe
        testTs(82).entries ++ Seq(TSEntry(112, 222d, 9), TSEntry(121, 444d, 10))

      tri.prepend(testTs(90), compress = false).entries shouldBe
        testTs(90).entries ++ Seq(TSEntry(120, 222d, 1), TSEntry(121, 444d, 10))

      tri.prepend(testTs(91), compress = false).entries shouldBe
        testTs(91).entries ++ Seq(TSEntry(121, 444d, 10))

      // On third
      tri.prepend(testTs(92), compress = false).entries shouldBe
        testTs(92).entries ++ Seq(TSEntry(122, 444d, 9))

      tri.prepend(testTs(100), compress = false).entries shouldBe
        testTs(100).entries ++ Seq(TSEntry(130, 444d, 1))

      tri.prepend(testTs(101), compress = false).entries shouldBe
        testTs(101).entries

      tri.prepend(testTs(102), compress = false).entries shouldBe
        testTs(102).entries
    }

    it should "do a step integral" in {
      val tri = newTs(Seq(TSEntry(100, 1, 10), TSEntry(110, 2, 10), TSEntry(120, 3, 10)))

      assert(
        tri.stepIntegral(10, TimeUnit.SECONDS).entries ===
            Seq(TSEntry(100, 10.0, 10), TSEntry(110, 30.0, 10), TSEntry(120, 60.0, 10))
      )

      val withSampling = TSEntry(100, 1, 30)

      assert(
        withSampling.stepIntegral(10, TimeUnit.SECONDS).entries ===
            Seq(TSEntry(100, 10.0, 10), TSEntry(110, 20.0, 10), TSEntry(120, 30.0, 10))
      )
    }

    it should "split up the entries of a timeseries" in {
      val withSlicing = TSEntry(100, 1, 30)

      assert(
        withSlicing.splitEntriesLongerThan(10).entries ===
            Seq(TSEntry(100, 1, 10), TSEntry(110, 1, 10), TSEntry(120, 1, 10))
      )

      assert(
        withSlicing.splitEntriesLongerThan(20).entries ===
            Seq(TSEntry(100, 1, 20), TSEntry(120, 1, 10))
      )
    }

    it should "split a timeseries into buckets" in {
      val buckets = Stream.from(0, 10).map(_.toLong)
      val tri =
        newTs(Seq(TSEntry(10, 1, 10), TSEntry(20, 2, 5), TSEntry(25, 3, 5)))
      val result = tri.bucket(buckets)

      val expected = Stream(
        (0, EmptyTimeSeries),
        (10, TSEntry(10, 1, 10)),
        (20, newTs(Seq(TSEntry(20, 2, 5), TSEntry(25, 3, 5)))),
        (30, EmptyTimeSeries)
      )

      (expected, result).zipped.foreach {
        case ((eTs, eSeries), (rTs, rSeries)) =>
          rTs shouldBe eTs
          rSeries.entries shouldBe eSeries.entries
      }
    }

    it should "do a sliding integral of a timeseries" in {
      val triA = TimeSeries(
        Seq(
          TSEntry(10, 1, 10),
          TSEntry(20, 2, 2),
          TSEntry(22, 3, 10)
        )
      )

      triA.slidingIntegral(2, 2, TimeUnit.SECONDS).entries shouldBe Seq(
        TSEntry(10, 2, 2),
        TSEntry(12, 4, 8),
        TSEntry(20, 6, 2),
        TSEntry(22, 10, 2),
        TSEntry(24, 12, 8)
      )

      triA.slidingIntegral(4, 2, TimeUnit.SECONDS).entries shouldBe Seq(
        TSEntry(10, 2, 2),
        TSEntry(12, 4, 2),
        TSEntry(14, 6, 6),
        TSEntry(20, 8, 2),
        TSEntry(22, 12, 2),
        TSEntry(24, 16, 2),
        TSEntry(26, 18, 6)
      )

      triA.slidingIntegral(12, 8, TimeUnit.SECONDS).entries shouldBe Seq(
        TSEntry(10, 8.0, 8),
        TSEntry(18, 24.0, 8),
        TSEntry(26, 48.0, 4),
        TSEntry(30, 40.0, 4)
      )
    }

    it should "return an empty timeseries if one filters all values" in {
      val ts = newTs(
        Seq(
          TSEntry(1, 1, 1),
          TSEntry(2, 2, 2),
          TSEntry(3, 3, 3)
        )
      )

      assert(ts.filterEntries(_ => false) === EmptyTimeSeries)
    }

    it should "return a correct loose domain" in {
      tri.looseDomain shouldBe ContiguousTimeDomain(tri.head.timestamp, tri.last.definedUntil)
    }

    it should "calculate the support ratio" in {
      val threeFourths = newTs(Seq(TSEntry(1, 1234d, 2), TSEntry(4, 5678d, 1)))
      threeFourths.supportRatio shouldBe 0.75
    }
  }

  /**
   * Tests the functions defined by the trait TimeSeries for a given implementation
   * that is capable of taking generic values. (It is tested by Strings here.)
   *
   * @note The mapping functions are only tested without compression. Use
   *       'nonEmptyNonSingletonDoubleTimeSeriesWithCompression' to test that.
   *
   * @param newTsString constructor method for the timeseries implementation that
   *                     shall be tested
   */
  def nonEmptyNonSingletonGenericTimeSeries(
    newTsString: Seq[TSEntry[String]] => TimeSeries[String]
  ): Unit = {
    val threeStrings = newTsString(Seq(TSEntry(0, "Hi", 10), TSEntry(10, "Ho", 10), TSEntry(20, "Hu", 10)))

    it should "correctly map a timeseries of three strings" in {
      val up = threeStrings.map(s => s.toUpperCase())
      assert(up.size === 3)
      assert(up.at(0).contains("HI"))
      assert(up.at(10).contains("HO"))
      assert(up.at(20).contains("HU"))
    }

    it should "correctly map the entries of a timeseries of three strings" in {
      val up = threeStrings.mapEntries(e => e.value.toUpperCase() + e.timestamp)
      assert(3 === up.size)
      assert(up.at(0).contains("HI0"))
      assert(up.at(10).contains("HO10"))
      assert(up.at(20).contains("HU20"))
    }

    it should "correctly filter a timeseries of three strings" in {
      val ts = newTsString(Seq(TSEntry(0, "Hi", 10), TSEntry(15, "Ho", 15), TSEntry(30, "Hu", 20)))

      assert(
        ts.filterEntries(_.value.startsWith("H")).entries === ts.entries
      )
      assert(
        ts.filterEntries(_.value.endsWith("H")) === EmptyTimeSeries
      )
    }

    it should "fill a timeseries of three strings" in {
      val tri = newTsString(Seq(TSEntry(0, "Hi", 10), TSEntry(20, "Ho", 10), TSEntry(40, "Hu", 10)))

      assert(
        tri.fill("Ha").entries ===
            Seq(
              TSEntry(0, "Hi", 10),
              TSEntry(10, "Ha", 10),
              TSEntry(20, "Ho", 10),
              TSEntry(30, "Ha", 10),
              TSEntry(40, "Hu", 10)
            )
      )

      assert(
        tri.fill("Hi").entries ===
            Seq(
              TSEntry(0, "Hi", 20),
              TSEntry(20, "Ho", 10),
              TSEntry(30, "Hi", 10),
              TSEntry(40, "Hu", 10)
            )
      )

      assert(
        tri.fill("Ho").entries ===
            Seq(
              TSEntry(0, "Hi", 10),
              TSEntry(10, "Ho", 30),
              TSEntry(40, "Hu", 10)
            )
      )

      assert(
        tri.fill("Hu").entries ===
            Seq(
              TSEntry(0, "Hi", 10),
              TSEntry(10, "Hu", 10),
              TSEntry(20, "Ho", 10),
              TSEntry(30, "Hu", 20)
            )
      )
    }
  }

  /**
   * *Main test bench* for a timeseries implementation. This tests all the functions
   * defined by the TimeSeries trait for a non empty and non singleton (TSEntry)
   * timeseries implementation. All the tests use double valued series.
   *
   * @param newTs constructor method for the timeseries implementation to test
   */
  def nonEmptyNonSingletonDoubleTimeSeriesWithCompression(
    newTs: Seq[TSEntry[Double]] => TimeSeries[Double]
  ): Unit = {

    val anotherThree = newTs(Seq(TSEntry(1, 111d, 9), TSEntry(10, 222d, 10), TSEntry(20, 444d, 10)))

    it should "correctly map a timeseries of three entries with compression" in {
      val up = anotherThree.map(s => 42, compress = true)
      up.entries shouldBe Seq(TSEntry(1, 42, 29))
    }

    it should "filter & map the entries of a timeseries of three entries correctly" in {
      val ts = newTs(Seq(TSEntry(1, 111d, 15), TSEntry(15, 222d, 15), TSEntry(30, 444d, 20)))

      ts.filterMapEntries(
          entry => if (entry.timestamp < 25) Some(123.456) else None,
          compress = true
        )
        .entries shouldBe Seq(TSEntry(1, 123.456, 29))
    }

    it should "correctly map with time a timeseries of three entries with compression" in {
      val ts = anotherThree

      val up = ts.mapEntries(_ => 42, compress = true)
      assert(up.entries === Seq(TSEntry(1, 42, 29)))
    }

    it should "append entries correctly with compression" in {
      val tri =
        newTs(Seq(TSEntry(1, 111d, 10), TSEntry(11, 222d, 10), TSEntry(21, 444d, 10)))

      // Appending after...
      assert(
        Seq(TSEntry(1, 111d, 10), TSEntry(11, 222d, 10), TSEntry(21, 444d, 10), TSEntry(32, "Hy", 10))
          === tri.append(TSEntry(32, "Hy", 10)).entries
      )

      assert(
        Seq(TSEntry(1, 111d, 10), TSEntry(11, 222d, 10), TSEntry(21, 444d, 10), TSEntry(31, "Hy", 10))
          === tri.append(TSEntry(31, "Hy", 10)).entries
      )

      // Appending on last entry
      assert(
        Seq(TSEntry(1, 111d, 10), TSEntry(11, 222d, 10), TSEntry(21, 444d, 9), TSEntry(30, "Hy", 10))
          === tri.append(TSEntry(30, "Hy", 10)).entries
      )

      assert(
        Seq(TSEntry(1, 111d, 10), TSEntry(11, 222d, 10), TSEntry(21, 444d, 1), TSEntry(22, "Hy", 10))
          === tri.append(TSEntry(22, "Hy", 10)).entries
      )

      // ... just after and on second entry
      assert(
        Seq(TSEntry(1, 111d, 10), TSEntry(11, 222d, 10), TSEntry(21, "Hy", 10))
          === tri.append(TSEntry(21, "Hy", 10)).entries
      )

      assert(
        Seq(TSEntry(1, 111d, 10), TSEntry(11, 222d, 9), TSEntry(20, "Hy", 10))
          === tri.append(TSEntry(20, "Hy", 10)).entries
      )

      assert(
        Seq(TSEntry(1, 111d, 10), TSEntry(11, 222d, 1), TSEntry(12, "Hy", 10))
          === tri.append(TSEntry(12, "Hy", 10)).entries
      )

      // ... just after and on first
      assert(
        Seq(TSEntry(1, 111d, 10), TSEntry(11, "Hy", 10))
          === tri.append(TSEntry(11, "Hy", 10)).entries
      )

      assert(
        Seq(TSEntry(1, 111d, 9), TSEntry(10, "Hy", 10))
          === tri.append(TSEntry(10, "Hy", 10)).entries
      )

      assert(
        Seq(TSEntry(1, 111d, 1), TSEntry(2, "Hy", 10))
          === tri.append(TSEntry(2, "Hy", 10)).entries
      )

      // And complete override
      assert(
        Seq(TSEntry(1, "Hy", 10))
          === tri.append(TSEntry(1, "Hy", 10)).entries
      )

    }

    it should "prepend entries correctly with compression" in {
      val tri =
        newTs(Seq(TSEntry(1, 111d, 10), TSEntry(11, 222d, 10), TSEntry(21, 444d, 10)))

      // Prepending before...
      assert(
        Seq(TSEntry(-10, "Hy", 10), TSEntry(1, 111d, 10), TSEntry(11, 222d, 10), TSEntry(21, 444d, 10))
          === tri.prepend(TSEntry(-10, "Hy", 10)).entries
      )

      assert(
        Seq(TSEntry(-9, "Hy", 10), TSEntry(1, 111d, 10), TSEntry(11, 222d, 10), TSEntry(21, 444d, 10))
          === tri.prepend(TSEntry(-9, "Hy", 10)).entries
      )

      // Overlaps with first entry
      assert(
        Seq(TSEntry(-8, "Hy", 10), TSEntry(2, 111d, 9), TSEntry(11, 222d, 10), TSEntry(21, 444d, 10))
          === tri.prepend(TSEntry(-8, "Hy", 10)).entries
      )

      assert(
        Seq(TSEntry(0, "Hy", 10), TSEntry(10, 111d, 1), TSEntry(11, 222d, 10), TSEntry(21, 444d, 10))
          === tri.prepend(TSEntry(0, "Hy", 10)).entries
      )

      assert(
        Seq(TSEntry(1, "Hy", 10), TSEntry(11, 222d, 10), TSEntry(21, 444d, 10))
          === tri.prepend(TSEntry(1, "Hy", 10)).entries
      )

      // ... second entry
      assert(
        Seq(TSEntry(2, "Hy", 10), TSEntry(12, 222d, 9), TSEntry(21, 444d, 10))
          === tri.prepend(TSEntry(2, "Hy", 10)).entries
      )

      assert(
        Seq(TSEntry(10, "Hy", 10), TSEntry(20, 222d, 1), TSEntry(21, 444d, 10))
          === tri.prepend(TSEntry(10, "Hy", 10)).entries
      )

      assert(
        Seq(TSEntry(11, "Hy", 10), TSEntry(21, 444d, 10))
          === tri.prepend(TSEntry(11, "Hy", 10)).entries
      )

      // ... third entry
      assert(
        Seq(TSEntry(12, "Hy", 10), TSEntry(22, 444d, 9))
          === tri.prepend(TSEntry(12, "Hy", 10)).entries
      )

      assert(
        Seq(TSEntry(20, "Hy", 10), TSEntry(30, 444d, 1))
          === tri.prepend(TSEntry(20, "Hy", 10)).entries
      )

      // Complete override
      assert(
        Seq(TSEntry(21, "Hy", 10))
          === tri.prepend(TSEntry(21, "Hy", 10)).entries
      )

      assert(
        Seq(TSEntry(22, "Hy", 10))
          === tri.prepend(TSEntry(22, "Hy", 10)).entries
      )
    }

    def testTs(startsAt: Long): TimeSeries[Double] =
      newTs(
        Seq(
          TSEntry(startsAt, 123d, 10),
          TSEntry(startsAt + 10, 234d, 10),
          TSEntry(startsAt + 20, 345d, 10)
        )
      )

    it should "append a multi-entry TS at various times on the entry with compression" in {
      val tri =
        newTs(Seq(TSEntry(11, 111d, 10), TSEntry(21, 222d, 10), TSEntry(31, 444d, 10)))

      // Append after all entries
      assert(tri.entries ++ testTs(41).entries === tri.append(testTs(41)).entries)
      assert(tri.entries ++ testTs(42).entries === tri.append(testTs(42)).entries)

      // On last
      assert(
        Seq(TSEntry(11, 111d, 10), TSEntry(21, 222d, 10), TSEntry(31, 444d, 9)) ++ testTs(40).entries
          === tri.append(testTs(40)).entries
      )

      assert(
        Seq(TSEntry(11, 111d, 10), TSEntry(21, 222d, 10), TSEntry(31, 444d, 1)) ++ testTs(32).entries
          === tri.append(testTs(32)).entries
      )

      assert(
        Seq(TSEntry(11, 111d, 10), TSEntry(21, 222d, 10)) ++ testTs(31).entries
          === tri.append(testTs(31)).entries
      )

      // On second
      assert(
        Seq(TSEntry(11, 111d, 10), TSEntry(21, 222d, 9)) ++ testTs(30).entries
          === tri.append(testTs(30)).entries
      )

      assert(
        Seq(TSEntry(11, 111d, 10), TSEntry(21, 222d, 1)) ++ testTs(22).entries
          === tri.append(testTs(22)).entries
      )

      assert(
        Seq(TSEntry(11, 111d, 10)) ++ testTs(21).entries
          === tri.append(testTs(21)).entries
      )

      // On first
      assert(
        Seq(TSEntry(11, 111d, 9)) ++ testTs(20).entries
          === tri.append(testTs(20)).entries
      )

      assert(
        Seq(TSEntry(11, 111d, 1)) ++ testTs(12).entries
          === tri.append(testTs(12)).entries
      )

      assert(testTs(11).entries === tri.append(testTs(11)).entries)
      assert(testTs(10).entries === tri.append(testTs(10)).entries)
    }

    it should "prepend a multi-entry TS at various times on the entry with compression" in {
      val tri =
        newTs(Seq(TSEntry(101, 111d, 10), TSEntry(111, 222d, 10), TSEntry(121, 444d, 10)))

      // Before all entries
      assert(testTs(70).entries ++ tri.entries === tri.prepend(testTs(70)).entries)
      assert(testTs(71).entries ++ tri.entries === tri.prepend(testTs(71)).entries)

      // On first
      assert(
        testTs(72).entries ++ Seq(TSEntry(102, 111d, 9), TSEntry(111, 222d, 10), TSEntry(121, 444d, 10))
          === tri.prepend(testTs(72)).entries
      )

      assert(
        testTs(80).entries ++ Seq(TSEntry(110, 111d, 1), TSEntry(111, 222d, 10), TSEntry(121, 444d, 10))
          === tri.prepend(testTs(80)).entries
      )

      assert(
        testTs(81).entries ++ Seq(TSEntry(111, 222d, 10), TSEntry(121, 444d, 10))
          === tri.prepend(testTs(81)).entries
      )

      // On second
      assert(
        testTs(82).entries ++ Seq(TSEntry(112, 222d, 9), TSEntry(121, 444d, 10))
          === tri.prepend(testTs(82)).entries
      )

      assert(
        testTs(90).entries ++ Seq(TSEntry(120, 222d, 1), TSEntry(121, 444d, 10))
          === tri.prepend(testTs(90)).entries
      )

      assert(
        testTs(91).entries ++ Seq(TSEntry(121, 444d, 10))
          === tri.prepend(testTs(91)).entries
      )

      // On third
      assert(
        testTs(92).entries ++ Seq(TSEntry(122, 444d, 9))
          === tri.prepend(testTs(92)).entries
      )

      assert(
        testTs(100).entries ++ Seq(TSEntry(130, 444d, 1))
          === tri.prepend(testTs(100)).entries
      )

      assert(testTs(101).entries === tri.prepend(testTs(101)).entries)
      assert(testTs(102).entries === tri.prepend(testTs(102)).entries)

    }

  }
}
