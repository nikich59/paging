package uz.uzum.tezkor.data_paging

class DataPagingHelperNoStaticData<SourceItem, DataItem : PagedDataItem>(
    dataProvider: PagedDataProviderNoStaticData<SourceItem>,
    dataMapper: PagedDataMapper<SourceItem, DataItem>,
    exceptionHandler: PagedDataExceptionHandler,
    pageSize: Long = 25,
    initialOffset: Long = 0,
    initialPageCount: Long = 1,
    loadDataTolerance: Long = 3,
) : DataPagingHelper<SourceItem, DataItem, NoStaticData>(
    dataProvider = dataProvider,
    dataMapper = dataMapper,
    exceptionHandler = exceptionHandler,
    pageSize = pageSize,
    initialOffset = initialOffset,
    initialPageCount = initialPageCount,
    loadDataTolerance = loadDataTolerance,
)
