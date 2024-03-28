package uz.uzum.tezkor.data_paging

data class PagedDataBounds(
    val offset: Long,
    val limit: Long,
) {
    fun isEmpty(): Boolean = limit <= 0
}
