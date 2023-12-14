package no.nav.helse.flex

import org.amshove.kluent.shouldBeEqualTo

@Suppress("ktlint:standard:function-naming")
infix fun String.`should be equal to ignoring whitespace`(expected: String) =
    this.filterNot {
        it.isWhitespace()
    }.shouldBeEqualTo(expected.filterNot { it.isWhitespace() })
