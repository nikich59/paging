package uz.uzum.tezkor.data_paging

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import uz.uzum.tezkor.uikit.recycler.TezkorRecyclerItem
import uz.uzum.tezkor.uikit.recycler.TezkorRecyclerViewItem
import uz.uzum.tezkor.uikit.util.dpToPx
import uz.uzum.tezkor.uikit.view.ProgressBar
import kotlin.math.roundToInt

class PagedDataLoaderRecyclerItem : TezkorRecyclerViewItem<View>() {

    override fun getItemId() = null

    override fun getViewCreator() = { parent: ViewGroup ->
        val layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            parent.context.dpToPx(72f).roundToInt()
        )

        FrameLayout(parent.context).also { container ->
            container.layoutParams = layoutParams
            container.addView(
                ProgressBar(parent.context),
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER,
                ),
            )
        }
    }

    override fun bindView(view: View) {}

    override fun isEqual(other: TezkorRecyclerItem<*>): Boolean {
        return other is PagedDataLoaderRecyclerItem
    }
}
