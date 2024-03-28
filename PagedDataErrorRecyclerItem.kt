package uz.uzum.tezkor.data_paging

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.updateMargins
import androidx.recyclerview.widget.RecyclerView
import uz.uzum.tezkor.uikit.drawable.RoundedCornersDrawable
import uz.uzum.tezkor.uikit.recycler.TezkorRecyclerItem
import uz.uzum.tezkor.uikit.recycler.TezkorRecyclerViewItem
import uz.uzum.tezkor.uikit.util.dpToPx
import kotlin.math.roundToInt

class PagedDataErrorRecyclerItem(
    private val text: String,
    private val onClick: () -> Unit,
) : TezkorRecyclerViewItem<View>() {

    override fun getItemId() = null

    override fun getViewCreator() = { parent: ViewGroup ->
        val layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            parent.context.dpToPx(72f).roundToInt(),
        )

        @Suppress("USELESS_CAST")
        PagedDataErrorView(parent.context).also { rootView ->
            rootView.layoutParams = layoutParams
        } as View
    }

    override fun bindView(view: View) {
        view as PagedDataErrorView

        view.setText(text)
        view.setOnClickListener {
            onClick()
        }
    }

    override fun isEqual(other: TezkorRecyclerItem<*>): Boolean {
        return other is PagedDataErrorRecyclerItem
    }
}

internal class PagedDataErrorView(context: Context) : FrameLayout(context) {

    private val textView = AppCompatTextView(context).also { textView ->
        textView.setTextAppearance(R.style.TextAppearance_Uzum_SmallSubtitle)
        textView.setTextColor(context.getColor(R.color.neutral_400))
    }

    init {
        val linearLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        addView(
            linearLayout,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER).apply {
                updateMargins(
                    left = context.dpToPx(16f).roundToInt(),
                    top = context.dpToPx(8f).roundToInt(),
                    right = context.dpToPx(16f).roundToInt(),
                    bottom = context.dpToPx(8f).roundToInt(),
                )
            },
        )

        linearLayout.addView(
            AppCompatImageView(context).also { imageView ->
                imageView.setImageResource(R.drawable.data_paging_refresh_icon)
            },
            LayoutParams(
                context.dpToPx(32f).roundToInt(),
                context.dpToPx(32f).roundToInt(),
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
            },
        )
        linearLayout.addView(
            textView,
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginStart = context.dpToPx(12f).roundToInt()
            },
        )

        linearLayout.background = RippleDrawable(
            ColorStateList.valueOf(context.getColor(R.color.press_ripple)),
            null,
            RoundedCornersDrawable(
                leftTopRadius = context.dpToPx(8f),
                rightTopRadius = context.dpToPx(8f),
                rightBottomRadius = context.dpToPx(8f),
                leftBottomRadius = context.dpToPx(8f),
                fillColor = Color.BLACK,
                border = null,
            )
        )
    }

    fun setText(text: String) {
        textView.text = text
    }
}
