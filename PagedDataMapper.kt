package uz.uzum.tezkor.data_paging

fun interface PagedDataMapper<SourceItem, DataItem : PagedDataItem> {
    fun mapItem(sourceItem: SourceItem, absoluteIndex: Long): DataItem
}
