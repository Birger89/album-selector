
package no.birg.albumselector.utility

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

@Suppress("PropertyName")
open class CoroutineContextProvider {
    open val Main: CoroutineContext by lazy { Dispatchers.Main }
    open val IO: CoroutineContext by lazy { Dispatchers.IO }
}
