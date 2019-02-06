package mockitox

import org.mockito.BDDMockito.*

inline fun <reified T> mock(f: T.(T) -> Unit): T =
        mock(T::class.java).also { it.f(it) }

inline fun <reified T> stub(): T =
        mock(T::class.java, withSettings().stubOnly())

inline fun <reified T> stub(name: String): T =
        mock(T::class.java, withSettings().stubOnly().name(name))

infix fun <T> T.returns(value: T) {
    given(this).willReturn(value)
}

inline infix fun <reified T> T.returnsStub(f: T.() -> Unit) {
    val mock = mock(T::class.java, withSettings().stubOnly())
    given(this).willReturn(mock)
    f(mock)
}