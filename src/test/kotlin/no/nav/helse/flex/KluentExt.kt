package no.nav.helse.flex

import org.amshove.kluent.shouldBeEqualTo

infix fun String.`should be equal to ignoring whitespace`(expected: String) = this.filterNot { it.isWhitespace() }.shouldBeEqualTo(expected.filterNot { it.isWhitespace() })
