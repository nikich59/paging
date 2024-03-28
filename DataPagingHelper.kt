package uz.uzum.tezkor.data_paging

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transformLatest
import uz.uzum.tezkor.common.flow.BehaviorFlow

open class DataPagingHelper<SourceItem, DataItem : PagedDataItem, StaticData>(
    private val dataProvider: PagedDataProvider<SourceItem, StaticData>,
    private val dataMapper: PagedDataMapper<SourceItem, DataItem>,
    private val exceptionHandler: PagedDataExceptionHandler,
    private val pageSize: Long = 25,
    private val initialOffset: Long = 0,
    private val initialPageCount: Long = 1,
    private val loadDataTolerance: Long = 3,
) {

    private val bounds = BehaviorFlow(PagedDataBounds(offset = initialOffset, limit = pageSize * initialPageCount))

    private var isLastPageLoaded: Boolean = false

    private var currentData: PagedData<DataItem, StaticData> = PagedData.Loading()

    @Suppress("RemoveExplicitTypeArguments")
    val data: Flow<PagedData<DataItem, StaticData>> = bounds
        .transformLatest<PagedDataBounds, PagedData<DataItem, StaticData>> { requestedBounds ->
            val dataBeforeLoad = currentData
            val itemsBeforeLoad: List<DataItem> = when (dataBeforeLoad) {
                is PagedData.Loading -> emptyList()
                is PagedData.Error -> emptyList()
                is PagedData.Content<DataItem, *> -> dataBeforeLoad.items
            }
            val bounds = reduceBounds(
                itemsBeforeLoad,
                requestedBounds,
            )
            val newData: PagedData<DataItem, StaticData> = if (bounds == null) {
                dataBeforeLoad
            } else {
                emit(
                    when (dataBeforeLoad) {
                        is PagedData.Loading -> PagedData.Loading()
                        is PagedData.Error -> PagedData.Loading()
                        is PagedData.Content<DataItem, StaticData> -> {
                            dataBeforeLoad
                                .copy(
                                    beforeStartContent = when (dataBeforeLoad.beforeStartContent) {
                                        is PagedDataEdgeContent.Loading -> PagedDataEdgeContent.Loading
                                        is PagedDataEdgeContent.Error -> PagedDataEdgeContent.Loading
                                        is PagedDataEdgeContent.NoMoreContent -> PagedDataEdgeContent.NoMoreContent
                                        null -> null
                                    },
                                    afterEndContent = when (dataBeforeLoad.afterEndContent) {
                                        is PagedDataEdgeContent.Loading -> PagedDataEdgeContent.Loading
                                        is PagedDataEdgeContent.Error -> PagedDataEdgeContent.Loading
                                        is PagedDataEdgeContent.NoMoreContent -> PagedDataEdgeContent.NoMoreContent
                                        null -> null
                                    },
                                )
                        }
                    }
                )

                try {
                    val sourceData = dataProvider.getData(bounds)

                    this@DataPagingHelper.isLastPageLoaded = if (sourceData.totalItemCount == null) {
                        sourceData.items.size < bounds.limit
                    } else {
                        sourceData.totalItemCount <= bounds.offset + bounds.limit
                    }

                    val dataItemsToAdd = sourceData.items.mapIndexed { sourceItemIndex, sourceItem ->
                        val absoluteIndex = bounds.offset + sourceItemIndex

                        dataMapper.mapItem(sourceItem, absoluteIndex)
                    }

                    val items = itemsBeforeLoad
                        .plus(dataItemsToAdd)
                        .distinctBy {
                            it.absoluteIndex
                        }
                        .sortedBy {
                            it.absoluteIndex
                        }

                    createSuccessfullyLoadedContent(items, sourceData.staticData)
                } catch (e: Exception) {
                    exceptionHandler.onException(e)

                    when (dataBeforeLoad) {
                        is PagedData.Loading -> {
                            PagedData.Error(e)
                        }

                        is PagedData.Error -> {
                            PagedData.Error(e)
                        }

                        is PagedData.Content<DataItem, StaticData> -> {
                            createContentWithLoadingErrors(itemsBeforeLoad, dataBeforeLoad.staticData)
                        }
                    }
                }
            }

            this@DataPagingHelper.currentData = newData

            emit(newData)
        }
        .distinctUntilChanged { a, b ->
            a == b
        }

    suspend fun setVisibleItems(firstVisibleItem: PagedDataItem, lastVisibleItem: PagedDataItem) {
        val currentBounds = bounds.first()
        val newOffset = ((firstVisibleItem.absoluteIndex - loadDataTolerance) / pageSize * pageSize).coerceAtLeast(0)
        val newLastIndex = ((lastVisibleItem.absoluteIndex + loadDataTolerance + 1) / pageSize + 1) * pageSize - 1
        val newLimit = newLastIndex - newOffset + 1

        if (currentBounds.offset != newOffset || currentBounds.limit != newLimit) {
            bounds.emit(
                PagedDataBounds(
                    offset = newOffset,
                    limit = newLimit,
                )
            )
        }
    }

    suspend fun resetDataToInitialBoundsAndReload() {
        isLastPageLoaded = false
        currentData = PagedData.Loading()
        bounds.emit(PagedDataBounds(offset = initialOffset, limit = pageSize * initialPageCount))
    }

    suspend fun reloadDataWithCurrentBounds() {
        bounds.emit(bounds.first())
    }

    private fun reduceBounds(currentData: List<PagedDataItem>, bounds: PagedDataBounds): PagedDataBounds? {
        if (currentData.isEmpty()) {
            return bounds
        }

        val offset = if (bounds.offset >= currentData.first().absoluteIndex) {
            bounds.offset.coerceAtLeast(currentData.last().absoluteIndex + 1)
        } else {
            bounds.offset
        }
        val boundsLastIndex = bounds.offset + bounds.limit - 1
        val lastIndex = if (boundsLastIndex <= currentData.last().absoluteIndex || isLastPageLoaded) {
            boundsLastIndex.coerceAtMost(currentData.first().absoluteIndex - 1)
        } else {
            boundsLastIndex
        }

        val newBounds = PagedDataBounds(
            offset = offset,
            limit = lastIndex - offset + 1,
        )

        return if (newBounds.isEmpty()) {
            null
        } else {
            newBounds
        }
    }

    private fun createSuccessfullyLoadedContent(
        items: List<DataItem>,
        staticData: StaticData,
    ): PagedData.Content<DataItem, StaticData> {
        val (canLoadBeforeStart, canLoadAfterEnd) = when {
            items.isEmpty() -> {
                false to false
            }

            items.size == 1 -> {
                Pair(
                    false,
                    !isLastPageLoaded,
                )
            }

            else -> {
                Pair(
                    items.first().absoluteIndex > 0,
                    !isLastPageLoaded,
                )
            }
        }

        return PagedData.Content(
            items = items,
            beforeStartContent = if (canLoadBeforeStart) {
                PagedDataEdgeContent.Loading
            } else {
                null
            },
            afterEndContent = if (canLoadAfterEnd) {
                PagedDataEdgeContent.Loading
            } else if (items.size > pageSize) {
                PagedDataEdgeContent.NoMoreContent
            } else {
                null
            },
            staticData = staticData,
        )
    }

    private fun createContentWithLoadingErrors(
        items: List<DataItem>,
        staticData: StaticData,
    ): PagedData.Content<DataItem, StaticData> {
        val (canLoadBeforeStart, canLoadAfterEnd) = when {
            items.isEmpty() -> {
                false to false
            }

            items.size == 1 -> {
                Pair(
                    false,
                    !isLastPageLoaded,
                )
            }

            else -> {
                Pair(
                    items.first().absoluteIndex > 0,
                    !isLastPageLoaded,
                )
            }
        }

        return PagedData.Content(
            items = items,
            beforeStartContent = if (canLoadBeforeStart) {
                PagedDataEdgeContent.Error
            } else {
                null
            },
            afterEndContent = if (canLoadAfterEnd) {
                PagedDataEdgeContent.Error
            } else {
                null
            },
            staticData = staticData,
        )
    }
}
