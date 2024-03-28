package uz.uzum.tezkor.data_paging

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import uz.uzum.tezkor.uikit.recycler.TezkorRecyclerItem
import uz.uzum.tezkor.uikit.recycler.TezkorVerticalRecyclerView

/**
 * @param AnyPagedDataItem is needed to ensure that [PagedDataRecyclerItem] is used in the recycler.
 *        Provide any of types used in it
 */
class PagedDataRecyclerViewHelper<AnyPagedDataItem>(
    private val refreshButtonText: String,
    private val noMoreContentLabel: String,
    private val onPageLoadingErrorReloadClick: () -> Unit,
    private val itemVisibilityListener: ItemVisibilityListener,
) where AnyPagedDataItem : PagedDataRecyclerItem,
        AnyPagedDataItem : TezkorRecyclerItem<*> {

    fun interface ItemVisibilityListener {
        fun onItemVisibilityChanged(firstVisibleItem: PagedDataItem, lastVisibleItem: PagedDataItem)
    }

    fun getEdgeContentRecyclerItem(
        edgeContent: PagedDataEdgeContent?,
    ): TezkorRecyclerItem<*>? {
        return when (edgeContent) {
            is PagedDataEdgeContent.Loading -> {
                PagedDataLoaderRecyclerItem()
            }

            is PagedDataEdgeContent.Error -> {
                PagedDataErrorRecyclerItem(refreshButtonText) {
                    onPageLoadingErrorReloadClick.invoke()
                }
            }

            is PagedDataEdgeContent.NoMoreContent -> {
                PagedDataNoMoreContentLabelRecyclerItem(noMoreContentLabel)
            }

            null -> {
                null
            }
        }
    }

    fun addRecyclerViewScrollListener(
        recyclerView: TezkorVerticalRecyclerView,
        layoutManager: LinearLayoutManager,
    ) {
        recyclerView.addOnChildAttachStateChangeListener(
            object : RecyclerView.OnChildAttachStateChangeListener {
                override fun onChildViewAttachedToWindow(view: View) {
                    val firstItemIndex = layoutManager.findFirstVisibleItemPosition()
                        .takeIf { it >= 0 }
                        ?.takeIf { it <= recyclerView.adapter.currentList.lastIndex }
                    val lastItemIndex = layoutManager.findLastVisibleItemPosition()
                        .takeIf { it >= 0 }
                        ?.takeIf { it <= recyclerView.adapter.currentList.lastIndex }

                    if (firstItemIndex != null && lastItemIndex != null) {
                        val visibleItems = recyclerView.adapter.currentList
                            .subList(
                                firstItemIndex,
                                lastItemIndex + 1,
                            )

                        val firstItem = visibleItems.firstNotNullOfOrNull {
                            (it as? PagedDataRecyclerItem)?.getPagedDataItem()
                        }
                        val lastItem = visibleItems.reversed().firstNotNullOfOrNull {
                            (it as? PagedDataRecyclerItem)?.getPagedDataItem()
                        }

                        if (firstItem != null && lastItem != null) {
                            itemVisibilityListener.onItemVisibilityChanged(
                                firstVisibleItem = firstItem,
                                lastVisibleItem = lastItem,
                            )
                        }
                    }
                }

                override fun onChildViewDetachedFromWindow(view: View) {}
            }
        )
    }
}
