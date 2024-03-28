package uz.uzum.tezkor.data_paging

import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import uz.uzum.tezkor.uikit.recycler.TezkorRecyclerItem
import uz.uzum.tezkor.uikit.recycler.TezkorRecyclerViewItem
import uz.uzum.tezkor.uikit.util.dpToPx
import kotlin.math.roundToInt

class PagedDataNoMoreContentLabelRecyclerItem(
    private val text: String,
) : TezkorRecyclerViewItem<AppCompatTextView>() {

    override fun getItemId() = null

    override fun getViewCreator() = { parent: ViewGroup ->
        val layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            parent.context.dpToPx(72f).roundToInt(),
        )

        AppCompatTextView(parent.context).also { textView ->
            textView.setTextAppearance(R.style.TextAppearance_Uzum_SmallSubtitle)
            textView.setTextColor(parent.context.getColor(R.color.neutral_400))
            textView.layoutParams = layoutParams
            textView.gravity = Gravity.CENTER
            textView.updatePadding(
                left = parent.context.dpToPx(20f).roundToInt(),
                top = parent.context.dpToPx(8f).roundToInt(),
                right = parent.context.dpToPx(20f).roundToInt(),
                bottom = parent.context.dpToPx(8f).roundToInt(),
            )
        }
    }

    override fun bindView(view: AppCompatTextView) {
        view.text = text
    }

    override fun isEqual(other: TezkorRecyclerItem<*>): Boolean {
        return other is PagedDataErrorRecyclerItem
    }
}
