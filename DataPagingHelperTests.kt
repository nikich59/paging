package uz.uzum.tezkor.data_paging

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

private class DataPagingHelperTests {

    companion object {
        @JvmStatic
        fun provideScrollDownValues(): Stream<Arguments> = Stream.of(
            Arguments.of(SCROLL_CONFIG_THREE_PAGES),
            Arguments.of(SCROLL_CONFIG_EXACTLY_THREE_PAGES),
            Arguments.of(SCROLL_CONFIG_ZERO_TOLERANCE),
            Arguments.of(SCROLL_CONFIG_ZERO_TOLERANCE_EXACTYL_THREE_PAGES),
            Arguments.of(SCROLL_CONFIG_WITHOUT_TOTAL),
            Arguments.of(SCROLL_CONFIG_WITHOUT_TOTAL_EXACTLY_THREE_PAGES),
        )
    }

    @ParameterizedTest
    @MethodSource("provideScrollDownValues")
    fun testScrollDown(testConfig: ScrollTestConfig) = runTest(UnconfinedTestDispatcher()) {
        val dataRequestBounds: MutableList<PagedDataBounds> = mutableListOf()
        val itemCount = testConfig.itemCount
        val dataProvider = PagedDataProvider { bounds ->
            dataRequestBounds.add(bounds)

            // Allow to skip loadings
            delay(10)

            PagedSourceDataNoStaticData(
                items = (bounds.offset until (bounds.offset + bounds.limit))
                    .filter { it < itemCount }
                    .map { index ->
                        SourceItem(id = index.toString())
                    },
                totalItemCount = testConfig.totalItemCount,
            )
        }
        val dataMapper = PagedDataMapper<SourceItem, PagedDataItem> { sourceItem, absoluteIndex ->
            PagedItem(id = sourceItem.id, absoluteIndex = absoluteIndex)
        }

        val pagingHelper = DataPagingHelper(
            dataProvider,
            dataMapper,
            exceptionHandler = { throw it },
            pageSize = testConfig.pageSize,
            initialOffset = testConfig.initialOffset,
            initialPageCount = testConfig.initialPageCount,
            loadDataTolerance = testConfig.loadDataTolerance,
        )

        val collectedData: MutableList<PagedData<PagedDataItem, NoStaticData>> = mutableListOf()
        val dataCollectingJob = launch {
            pagingHelper.data.collect {
                collectedData.add(it)
            }
        }
        testScheduler.runCurrent()
        testScheduler.advanceUntilIdle()

        repeat(itemCount) { index ->
            pagingHelper.setVisibleItems(
                firstVisibleItem = PagedItem(id = "", absoluteIndex = 0),
                lastVisibleItem = PagedItem(id = "", absoluteIndex = index.toLong()),
            )
            testScheduler.runCurrent()

            // Skip 4 out of 5 data loads
            if (index % 5 == 0) {
                testScheduler.advanceUntilIdle()
            }
        }
        testScheduler.advanceUntilIdle()

        Assertions.assertArrayEquals(
            testConfig.expectedDataStream.toTypedArray(),
            collectedData.toTypedArray(),
        )
        Assertions.assertArrayEquals(
            testConfig.expectedDataRequests.toTypedArray(),
            dataRequestBounds.toTypedArray(),
        )

        dataCollectingJob.cancel()
    }

    @Test
    fun testSinglePage() = runTest(UnconfinedTestDispatcher()) {
        val dataRequestBounds: MutableList<PagedDataBounds> = mutableListOf()
        val itemCount = 15
        val dataProvider = PagedDataProvider { bounds ->
            dataRequestBounds.add(bounds)

            // Allow to skip loadings
            delay(10)

            PagedSourceData(
                items = (bounds.offset until (bounds.offset + bounds.limit))
                    .filter { it < itemCount }
                    .map { index ->
                        SourceItem(id = index.toString())
                    },
                totalItemCount = itemCount.toLong(),
                staticData = NoStaticData,
            )
        }
        val dataMapper = PagedDataMapper<SourceItem, PagedDataItem> { sourceItem, absoluteIndex ->
            PagedItem(id = sourceItem.id, absoluteIndex = absoluteIndex)
        }

        val pagingHelper = DataPagingHelper(
            dataProvider,
            dataMapper,
            exceptionHandler = { throw it },
            pageSize = 25,
            initialOffset = 0,
            initialPageCount = 1,
            loadDataTolerance = 3,
        )

        val collectedData: MutableList<PagedData<PagedDataItem, NoStaticData>> = mutableListOf()
        val dataCollectingJob = launch {
            pagingHelper.data.collect {
                collectedData.add(it)
            }
        }
        testScheduler.runCurrent()
        testScheduler.advanceUntilIdle()

        repeat(itemCount) { index ->
            pagingHelper.setVisibleItems(
                firstVisibleItem = PagedItem(id = "", absoluteIndex = 0),
                lastVisibleItem = PagedItem(id = "", absoluteIndex = index.toLong()),
            )
            testScheduler.runCurrent()

            // Skip 4 out of 5 data loads
            if (index % 5 == 0) {
                testScheduler.advanceUntilIdle()
            }
        }
        testScheduler.advanceUntilIdle()

        Assertions.assertArrayEquals(
            arrayOf(
                PagedData.Loading(),
                PagedData.Content(
                    (0 until itemCount).map { index ->
                        PagedItem(id = index.toString(), absoluteIndex = index.toLong())
                    },
                    beforeStartContent = null,
                    afterEndContent = null,
                    staticData = NoStaticData,
                ),
            ),
            collectedData.toTypedArray(),
        )
        Assertions.assertArrayEquals(
            arrayOf(
                PagedDataBounds(0, 25),
            ),
            dataRequestBounds.toTypedArray(),
        )

        dataCollectingJob.cancel()
    }

    @Test
    fun testScrollUp() = runTest(UnconfinedTestDispatcher()) {
        val dataRequestBounds: MutableList<PagedDataBounds> = mutableListOf()
        val itemCount = 101
        val dataProvider = PagedDataProvider { bounds ->
            dataRequestBounds.add(bounds)

            // Allow to skip loadings
            delay(10)

            PagedSourceData(
                (bounds.offset until (bounds.offset + bounds.limit))
                    .filter { it < itemCount }
                    .map { index ->
                        SourceItem(id = index.toString())
                    },
                totalItemCount = itemCount.toLong(),
                staticData = NoStaticData,
            )
        }
        val dataMapper = PagedDataMapper<SourceItem, PagedDataItem> { sourceItem, absoluteIndex ->
            PagedItem(id = sourceItem.id, absoluteIndex = absoluteIndex)
        }

        val pagingHelper = DataPagingHelper(
            dataProvider,
            dataMapper,
            exceptionHandler = { throw it },
            pageSize = 25,
            initialOffset = 75,
            initialPageCount = 1,
            loadDataTolerance = 3,
        )

        val collectedData: MutableList<PagedData<PagedDataItem, NoStaticData>> = mutableListOf()
        val dataCollectingJob = launch {
            pagingHelper.data.collect {
                collectedData.add(it)
            }
        }
        testScheduler.runCurrent()
        testScheduler.advanceUntilIdle()

        (75 downTo 0).map { index ->
            pagingHelper.setVisibleItems(
                firstVisibleItem = PagedItem(id = "", absoluteIndex = index.toLong()),
                lastVisibleItem = PagedItem(id = "", absoluteIndex = 90),
            )
            testScheduler.runCurrent()

            if (index % 5 == 0) {
                testScheduler.advanceUntilIdle()
            }
        }
        testScheduler.advanceUntilIdle()

        Assertions.assertArrayEquals(
            listOf(
                PagedData.Loading(),
                PagedData.Content(
                    (75 until 100).map { index ->
                        PagedItem(id = index.toString(), absoluteIndex = index.toLong())
                    },
                    beforeStartContent = PagedDataEdgeContent.Loading,
                    afterEndContent = PagedDataEdgeContent.Loading,
                    staticData = NoStaticData,
                ),
                PagedData.Content(
                    (50 until 100).map { index ->
                        PagedItem(id = index.toString(), absoluteIndex = index.toLong())
                    },
                    beforeStartContent = PagedDataEdgeContent.Loading,
                    afterEndContent = PagedDataEdgeContent.Loading,
                    staticData = NoStaticData,
                ),
                PagedData.Content(
                    (25 until 100).map { index ->
                        PagedItem(id = index.toString(), absoluteIndex = index.toLong())
                    },
                    beforeStartContent = PagedDataEdgeContent.Loading,
                    afterEndContent = PagedDataEdgeContent.Loading,
                    staticData = NoStaticData,
                ),
                PagedData.Content(
                    (0 until 100).map { index ->
                        PagedItem(id = index.toString(), absoluteIndex = index.toLong())
                    },
                    beforeStartContent = null,
                    afterEndContent = PagedDataEdgeContent.Loading,
                    staticData = NoStaticData,
                ),
            ).toTypedArray(),
            collectedData.toTypedArray(),
        )
        Assertions.assertArrayEquals(
            arrayOf(
                PagedDataBounds(75, 25),
                PagedDataBounds(50, 25),
                PagedDataBounds(25, 25),
                PagedDataBounds(0, 25),
            ),
            dataRequestBounds.toTypedArray(),
        )

        dataCollectingJob.cancel()
    }

    @Test
    fun testLoadingSimultaneouslyFromStartAndEnd() = runTest(UnconfinedTestDispatcher()) {
        val dataRequestBounds: MutableList<PagedDataBounds> = mutableListOf()
        val itemCount = 101
        val dataProvider = PagedDataProvider { bounds ->
            dataRequestBounds.add(bounds)

            PagedSourceData(
                (bounds.offset until (bounds.offset + bounds.limit))
                    .filter { it < itemCount }
                    .map { index ->
                        SourceItem(id = index.toString())
                    },
                totalItemCount = itemCount.toLong(),
                staticData = NoStaticData,
            )
        }
        val dataMapper = PagedDataMapper<SourceItem, PagedDataItem> { sourceItem, absoluteIndex ->
            PagedItem(id = sourceItem.id, absoluteIndex = absoluteIndex)
        }

        val pagingHelper = DataPagingHelper(
            dataProvider,
            dataMapper,
            exceptionHandler = { throw it },
            pageSize = 25,
            initialOffset = 50,
            initialPageCount = 1,
            loadDataTolerance = 3,
        )

        val collectedData: MutableList<PagedData<PagedDataItem, NoStaticData>> = mutableListOf()
        val dataCollectingJob = launch {
            pagingHelper.data.collect {
                collectedData.add(it)
            }
        }

        pagingHelper.setVisibleItems(
            firstVisibleItem = PagedItem(id = "", absoluteIndex = 50),
            lastVisibleItem = PagedItem(id = "", absoluteIndex = 75),
        )
        testScheduler.runCurrent()
        testScheduler.advanceUntilIdle()

        Assertions.assertArrayEquals(
            listOf(
                PagedData.Loading(),
                PagedData.Content(
                    (50 until 75).map { index ->
                        PagedItem(id = index.toString(), absoluteIndex = index.toLong())
                    },
                    beforeStartContent = PagedDataEdgeContent.Loading,
                    afterEndContent = PagedDataEdgeContent.Loading,
                    staticData = NoStaticData,
                ),
                PagedData.Content(
                    (25 until 100).map { index ->
                        PagedItem(id = index.toString(), absoluteIndex = index.toLong())
                    },
                    beforeStartContent = PagedDataEdgeContent.Loading,
                    afterEndContent = PagedDataEdgeContent.Loading,
                    staticData = NoStaticData,
                ),
            ).toTypedArray(),
            collectedData.toTypedArray(),
        )
        Assertions.assertArrayEquals(
            arrayOf(
                PagedDataBounds(offset = 50, limit = 25),
                PagedDataBounds(offset = 25, limit = 75),
            ),
            dataRequestBounds.toTypedArray(),
        )

        dataCollectingJob.cancel()
    }

    @Test
    fun testInitialDataLoadingErrorAndPagingAfterSuccessfulRequest() = runTest(UnconfinedTestDispatcher()) {
        val dataRequestBounds: MutableList<PagedDataBounds> = mutableListOf()
        val dataProviderException = IllegalStateException("Test exception")
        val itemCount = 35
        var isDataProviderExceptionThrown = false
        val dataProvider = PagedDataProvider { bounds ->
            dataRequestBounds.add(bounds)

            if (isDataProviderExceptionThrown) {
                PagedSourceData(
                    (bounds.offset until (bounds.offset + bounds.limit))
                        .filter { it < itemCount }
                        .map { index ->
                            SourceItem(id = index.toString())
                        },
                    totalItemCount = itemCount.toLong(),
                    staticData = NoStaticData,
                )
            } else {
                isDataProviderExceptionThrown = true

                throw dataProviderException
            }
        }
        val dataMapper = PagedDataMapper<SourceItem, PagedDataItem> { sourceItem, absoluteIndex ->
            PagedItem(id = sourceItem.id, absoluteIndex = absoluteIndex)
        }

        val pagingHelper = DataPagingHelper(
            dataProvider,
            dataMapper,
            exceptionHandler = { exception ->
                if (exception != dataProviderException) {
                    throw exception
                }
            },
            pageSize = 25,
            initialOffset = 0,
            initialPageCount = 1,
            loadDataTolerance = 3,
        )

        val collectedData: MutableList<PagedData<PagedDataItem, NoStaticData>> = mutableListOf()
        val dataCollectingJob = launch {
            pagingHelper.data.collect {
                collectedData.add(it)
            }
        }

        pagingHelper.reloadDataWithCurrentBounds()

        pagingHelper.setVisibleItems(
            PagedItem("", 0),
            PagedItem("", 25),
        )

        testScheduler.runCurrent()
        testScheduler.advanceUntilIdle()

        Assertions.assertArrayEquals(
            listOf<PagedData<PagedDataItem, NoStaticData>>(
                PagedData.Loading(),
                PagedData.Error(dataProviderException),
                PagedData.Loading(),
                PagedData.Content(
                    (0 until 25).map { index ->
                        PagedItem(id = index.toString(), absoluteIndex = index.toLong())
                    },
                    beforeStartContent = null,
                    afterEndContent = PagedDataEdgeContent.Loading,
                    staticData = NoStaticData,
                ),
                PagedData.Content(
                    (0 until 35).map { index ->
                        PagedItem(id = index.toString(), absoluteIndex = index.toLong())
                    },
                    beforeStartContent = null,
                    afterEndContent = PagedDataEdgeContent.NoMoreContent,
                    staticData = NoStaticData,
                ),
            ).toTypedArray(),
            collectedData.toTypedArray(),
        )
        Assertions.assertArrayEquals(
            arrayOf(
                PagedDataBounds(offset = 0, limit = 25),
                PagedDataBounds(offset = 0, limit = 25),
                PagedDataBounds(offset = 25, limit = 25),
            ),
            dataRequestBounds.toTypedArray(),
        )

        dataCollectingJob.cancel()
    }

    @Test
    fun testErrorWhenScrolling() = runTest(UnconfinedTestDispatcher()) {
        val dataRequestBounds: MutableList<PagedDataBounds> = mutableListOf()
        val itemCount = 35
        val dataProviderException = IllegalStateException("Test exception")
        var isDataProviderExceptionThrown = false
        val dataProvider = PagedDataProvider { bounds ->
            dataRequestBounds.add(bounds)

            if (bounds.offset > 0) {
                if (isDataProviderExceptionThrown) {
                    PagedSourceData(
                        (bounds.offset until (bounds.offset + bounds.limit))
                            .filter { it < itemCount }
                            .map { index ->
                                SourceItem(id = index.toString())
                            },
                        totalItemCount = itemCount.toLong(),
                        staticData = NoStaticData,
                    )
                } else {
                    isDataProviderExceptionThrown = true

                    throw dataProviderException
                }
            } else {
                PagedSourceData(
                    (bounds.offset until (bounds.offset + bounds.limit))
                        .filter { it < itemCount }
                        .map { index ->
                            SourceItem(id = index.toString())
                        },
                    totalItemCount = itemCount.toLong(),
                    staticData = NoStaticData,
                )
            }
        }
        val dataMapper = PagedDataMapper<SourceItem, PagedDataItem> { sourceItem, absoluteIndex ->
            PagedItem(id = sourceItem.id, absoluteIndex = absoluteIndex)
        }

        val pagingHelper = DataPagingHelper(
            dataProvider,
            dataMapper,
            exceptionHandler = { exception ->
                if (exception != dataProviderException) {
                    throw exception
                }
            },
            pageSize = 25,
            initialOffset = 0,
            initialPageCount = 1,
            loadDataTolerance = 3,
        )

        val collectedData: MutableList<PagedData<PagedDataItem, NoStaticData>> = mutableListOf()
        val dataCollectingJob = launch {
            pagingHelper.data.collect {
                collectedData.add(it)
            }
        }

        pagingHelper.setVisibleItems(
            firstVisibleItem = PagedItem(id = "", absoluteIndex = 0),
            lastVisibleItem = PagedItem(id = "", absoluteIndex = 25),
        )
        pagingHelper.reloadDataWithCurrentBounds()
        testScheduler.runCurrent()
        testScheduler.advanceUntilIdle()

        Assertions.assertArrayEquals(
            listOf(
                PagedData.Loading(),
                PagedData.Content(
                    (0 until 25).map { index ->
                        PagedItem(id = index.toString(), absoluteIndex = index.toLong())
                    },
                    beforeStartContent = null,
                    afterEndContent = PagedDataEdgeContent.Loading,
                    staticData = NoStaticData,
                ),
                PagedData.Content(
                    (0 until 25).map { index ->
                        PagedItem(id = index.toString(), absoluteIndex = index.toLong())
                    },
                    beforeStartContent = null,
                    afterEndContent = PagedDataEdgeContent.Error,
                    staticData = NoStaticData,
                ),
                PagedData.Content(
                    (0 until 25).map { index ->
                        PagedItem(id = index.toString(), absoluteIndex = index.toLong())
                    },
                    beforeStartContent = null,
                    afterEndContent = PagedDataEdgeContent.Loading,
                    staticData = NoStaticData,
                ),
                PagedData.Content(
                    (0 until 35).map { index ->
                        PagedItem(id = index.toString(), absoluteIndex = index.toLong())
                    },
                    beforeStartContent = null,
                    afterEndContent = PagedDataEdgeContent.NoMoreContent,
                    staticData = NoStaticData,
                ),
            ).toTypedArray(),
            collectedData.toTypedArray(),
        )
        Assertions.assertArrayEquals(
            arrayOf(
                PagedDataBounds(offset = 0, limit = 25),
                PagedDataBounds(offset = 25, limit = 25),
                PagedDataBounds(offset = 25, limit = 25),
            ),
            dataRequestBounds.toTypedArray(),
        )

        dataCollectingJob.cancel()
    }
}

private data class SourceItem(
    val id: String,
)

private data class PagedItem(
    val id: String,
    override val absoluteIndex: Long,
) : PagedDataItem

private data class ScrollTestConfig(
    val itemCount: Int,
    val totalItemCount: Long?,
    val pageSize: Long,
    val initialOffset: Long,
    val initialPageCount: Long,
    val loadDataTolerance: Long,
    val expectedDataStream: List<PagedData<PagedItem, NoStaticData>>,
    val expectedDataRequests: List<PagedDataBounds>,
)

private val SCROLL_CONFIG_THREE_PAGES = ScrollTestConfig(
    itemCount = 51,
    totalItemCount = 51,
    pageSize = 25,
    initialOffset = 0,
    initialPageCount = 1,
    loadDataTolerance = 3,
    expectedDataStream = listOf(
        PagedData.Loading(),
        PagedData.Content(
            (0 until 25).map { index ->
                PagedItem(id = index.toString(), absoluteIndex = index.toLong())
            },
            beforeStartContent = null,
            afterEndContent = PagedDataEdgeContent.Loading,
            staticData = NoStaticData,
        ),
        PagedData.Content(
            (0 until 50).map { index ->
                PagedItem(id = index.toString(), absoluteIndex = index.toLong())
            },
            beforeStartContent = null,
            afterEndContent = PagedDataEdgeContent.Loading,
            staticData = NoStaticData,
        ),
        PagedData.Content(
            (0 until 51).map { index ->
                PagedItem(id = index.toString(), absoluteIndex = index.toLong())
            },
            beforeStartContent = null,
            afterEndContent = PagedDataEdgeContent.NoMoreContent,
            staticData = NoStaticData,
        ),
    ),
    expectedDataRequests = listOf(
        PagedDataBounds(0, 25),
        PagedDataBounds(25, 25),
        PagedDataBounds(50, 25),
    )
)

private val SCROLL_CONFIG_EXACTLY_THREE_PAGES = ScrollTestConfig(
    itemCount = 75,
    totalItemCount = 75,
    pageSize = 25,
    initialOffset = 0,
    initialPageCount = 1,
    loadDataTolerance = 3,
    expectedDataStream = listOf(
        PagedData.Loading(),
        PagedData.Content(
            (0 until 25).map { index ->
                PagedItem(id = index.toString(), absoluteIndex = index.toLong())
            },
            beforeStartContent = null,
            afterEndContent = PagedDataEdgeContent.Loading,
            staticData = NoStaticData,
        ),
        PagedData.Content(
            (0 until 50).map { index ->
                PagedItem(id = index.toString(), absoluteIndex = index.toLong())
            },
            beforeStartContent = null,
            afterEndContent = PagedDataEdgeContent.Loading,
            staticData = NoStaticData,
        ),
        PagedData.Content(
            (0 until 75).map { index ->
                PagedItem(id = index.toString(), absoluteIndex = index.toLong())
            },
            beforeStartContent = null,
            afterEndContent = PagedDataEdgeContent.NoMoreContent,
            staticData = NoStaticData,
        ),
    ),
    expectedDataRequests = listOf(
        PagedDataBounds(0, 25),
        PagedDataBounds(25, 25),
        PagedDataBounds(50, 25),
    )
)

private val SCROLL_CONFIG_ZERO_TOLERANCE = ScrollTestConfig(
    itemCount = 51,
    totalItemCount = 51,
    pageSize = 25,
    initialOffset = 0,
    initialPageCount = 1,
    loadDataTolerance = 0,
    expectedDataStream = listOf(
        PagedData.Loading(),
        PagedData.Content(
            (0 until 25).map { index ->
                PagedItem(id = index.toString(), absoluteIndex = index.toLong())
            },
            beforeStartContent = null,
            afterEndContent = PagedDataEdgeContent.Loading,
            staticData = NoStaticData,
        ),
        PagedData.Content(
            (0 until 50).map { index ->
                PagedItem(id = index.toString(), absoluteIndex = index.toLong())
            },
            beforeStartContent = null,
            afterEndContent = PagedDataEdgeContent.Loading,
            staticData = NoStaticData,
        ),
        PagedData.Content(
            (0 until 51).map { index ->
                PagedItem(id = index.toString(), absoluteIndex = index.toLong())
            },
            beforeStartContent = null,
            afterEndContent = PagedDataEdgeContent.NoMoreContent,
            staticData = NoStaticData,
        ),
    ),
    expectedDataRequests = listOf(
        PagedDataBounds(0, 25),
        PagedDataBounds(25, 25),
        PagedDataBounds(50, 25),
    )
)

private val SCROLL_CONFIG_ZERO_TOLERANCE_EXACTYL_THREE_PAGES = ScrollTestConfig(
    itemCount = 75,
    totalItemCount = 75,
    pageSize = 25,
    initialOffset = 0,
    initialPageCount = 1,
    loadDataTolerance = 0,
    expectedDataStream = listOf(
        PagedData.Loading(),
        PagedData.Content(
            (0 until 25).map { index ->
                PagedItem(id = index.toString(), absoluteIndex = index.toLong())
            },
            beforeStartContent = null,
            afterEndContent = PagedDataEdgeContent.Loading,
            staticData = NoStaticData,
        ),
        PagedData.Content(
            (0 until 50).map { index ->
                PagedItem(id = index.toString(), absoluteIndex = index.toLong())
            },
            beforeStartContent = null,
            afterEndContent = PagedDataEdgeContent.Loading,
            staticData = NoStaticData,
        ),
        PagedData.Content(
            (0 until 75).map { index ->
                PagedItem(id = index.toString(), absoluteIndex = index.toLong())
            },
            beforeStartContent = null,
            afterEndContent = PagedDataEdgeContent.NoMoreContent,
            staticData = NoStaticData,
        ),
    ),
    expectedDataRequests = listOf(
        PagedDataBounds(0, 25),
        PagedDataBounds(25, 25),
        PagedDataBounds(50, 25),
    )
)

private val SCROLL_CONFIG_WITHOUT_TOTAL = ScrollTestConfig(
    itemCount = 55,
    totalItemCount = null,
    pageSize = 25,
    initialOffset = 0,
    initialPageCount = 1,
    loadDataTolerance = 0,
    expectedDataStream = listOf(
        PagedData.Loading(),
        PagedData.Content(
            (0 until 25).map { index ->
                PagedItem(id = index.toString(), absoluteIndex = index.toLong())
            },
            beforeStartContent = null,
            afterEndContent = PagedDataEdgeContent.Loading,
            staticData = NoStaticData,
        ),
        PagedData.Content(
            (0 until 50).map { index ->
                PagedItem(id = index.toString(), absoluteIndex = index.toLong())
            },
            beforeStartContent = null,
            afterEndContent = PagedDataEdgeContent.Loading,
            staticData = NoStaticData,
        ),
        PagedData.Content(
            (0 until 55).map { index ->
                PagedItem(id = index.toString(), absoluteIndex = index.toLong())
            },
            beforeStartContent = null,
            afterEndContent = PagedDataEdgeContent.NoMoreContent,
            staticData = NoStaticData,
        ),
    ),
    expectedDataRequests = listOf(
        PagedDataBounds(0, 25),
        PagedDataBounds(25, 25),
        PagedDataBounds(50, 25),
    )
)

private val SCROLL_CONFIG_WITHOUT_TOTAL_EXACTLY_THREE_PAGES = ScrollTestConfig(
    itemCount = 75,
    totalItemCount = null,
    pageSize = 25,
    initialOffset = 0,
    initialPageCount = 1,
    loadDataTolerance = 0,
    expectedDataStream = listOf(
        PagedData.Loading(),
        PagedData.Content(
            (0 until 25).map { index ->
                PagedItem(id = index.toString(), absoluteIndex = index.toLong())
            },
            beforeStartContent = null,
            afterEndContent = PagedDataEdgeContent.Loading,
            staticData = NoStaticData,
        ),
        PagedData.Content(
            (0 until 50).map { index ->
                PagedItem(id = index.toString(), absoluteIndex = index.toLong())
            },
            beforeStartContent = null,
            afterEndContent = PagedDataEdgeContent.Loading,
            staticData = NoStaticData,
        ),
        PagedData.Content(
            (0 until 75).map { index ->
                PagedItem(id = index.toString(), absoluteIndex = index.toLong())
            },
            beforeStartContent = null,
            afterEndContent = PagedDataEdgeContent.Loading,
            staticData = NoStaticData,
        ),
        PagedData.Content(
            (0 until 75).map { index ->
                PagedItem(id = index.toString(), absoluteIndex = index.toLong())
            },
            beforeStartContent = null,
            afterEndContent = PagedDataEdgeContent.NoMoreContent,
            staticData = NoStaticData,
        ),
    ),
    expectedDataRequests = listOf(
        PagedDataBounds(0, 25),
        PagedDataBounds(25, 25),
        PagedDataBounds(50, 25),
        PagedDataBounds(75, 25),
    )
)
