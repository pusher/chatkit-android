package mockitox

import org.mockito.BDDMockito.mock
import org.mockito.BDDMockito.withSettings

inline fun <reified T> stub(): T =
    mock(T::class.java, withSettings().stubOnly())


