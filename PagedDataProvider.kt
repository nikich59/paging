package uz.uzum.tezkor.data_paging

fun interface PagedDataProvider<T, StaticData> {

    suspend fun getData(bounds: PagedDataBounds): PagedSourceData<T, StaticData>
}

fun interface PagedDataProviderNoStaticData<T> : PagedDataProvider<T, NoStaticData> {

    override suspend fun getData(bounds: PagedDataBounds): PagedSourceDataNoStaticData<T>
}

open class PagedSourceData<T, StaticData>(
    val items: List<T>,
    val totalItemCount: Long?,
    val staticData: StaticData,
)

class PagedSourceDataNoStaticData<T>(
    items: List<T>,
    totalItemCount: Long?,
) : PagedSourceData<T, NoStaticData>(items, totalItemCount, NoStaticData)
