package sh.uffle.koms

import io.kotest.core.spec.style.DescribeSpec
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class IntExtensionSpec : DescribeSpec({
    listOf(Int.MIN_VALUE, -1, 0, 1, 255, 65000, Int.MAX_VALUE).forEach { number ->
        describe("converting $number to a byte array") {
            it("can be converted back to an integer") {
                expectThat(number.asByteArray().asInt())
                    .isEqualTo(number)
            }
        }

        describe("converting $number to a byte list") {
            it("can be converted back to an integer") {
                expectThat(number.asByteList().asInt())
                    .isEqualTo(number)
            }
        }
    }
})
