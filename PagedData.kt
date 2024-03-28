package uz.uzum.tezkor.data_paging

sealed interface PagedData<T, StaticData> {

    class Loading<T, StaticData> : PagedData<T, StaticData> {
        override fun equals(other: Any?): Boolean {
            return other is Loading<*, *>
        }

        override fun hashCode(): Int {
            return javaClass.hashCode()
        }
    }

    data class Error<T, StaticData>(
        val cause: Throwable,
    ) : PagedData<T, StaticData>

    data class Content<T, StaticData>(
        val items: List<T>,
        val beforeStartContent: PagedDataEdgeContent?,
        val afterEndContent: PagedDataEdgeContent?,
        val staticData: StaticData,
    ) : PagedData<T, StaticData>
}

data object NoStaticData
