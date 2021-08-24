package com.lulixue

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.isVisible
import kotlin.math.max

class ContainerView(context: Context, attrs: AttributeSet?) : ViewGroup(context, attrs) {
    constructor(context: Context) : this(context, null)

    enum class ContentAlignment(val value: Int) {
        Start(0),
        Center(1),
        End(2)
    }
    companion object {
        private val DisplayWidth: Int
            get() = Resources.getSystem().displayMetrics.widthPixels
        private val Float.dp
            get() = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, this, Resources.getSystem().displayMetrics
            )
        private fun View.setVisible(visible: Boolean) {
            this.visibility = if (visible) VISIBLE else GONE
        }
        private val Int.dp
            get() = this.toFloat().dp

        private const val VIEW_NEW_LINE = "NewLine"
        fun getSeparatorView(context: Context, height: Int): View {
            return View(context).apply {
                tag = VIEW_NEW_LINE
                layoutParams = MarginLayoutParams(MarginLayoutParams.WRAP_CONTENT, height)
            }
        }
        fun getNewLineView(context: Context): View {
            return getSeparatorView(context, 0)
        }

        private val DEFAULT_ITEM_SPACING = 3.dp.toInt()
        private val DEFAULT_LINE_SPACING = 5.dp.toInt()
    }
    private val childrenBounds = mutableListOf<RectF>()
    private val childrenBoundMap = LinkedHashMap<Int, ArrayList<RectF>>()
    private val childrenBoundMaxHeight = LinkedHashMap<Int, Int>()
    var itemSpacing: Int = DEFAULT_ITEM_SPACING
        set(value) {
            if (value == field) {
                return
            }
            field = value
            requestLayout()
        }
    var lineSpacing: Int = DEFAULT_LINE_SPACING
        set(value) {
            if (value == field) {
                return
            }
            field = value
            requestLayout()
        }
    var enableLazyLoading: Boolean = false
        set(value) {
            if (value == field) {
                return
            }
            field = value
            requestLayout()
        }

    var contentAlignment: ContentAlignment = ContentAlignment.Start
        set(value) {
            if (value == field) {
                return
            }
            field = value
            requestLayout()
        }

    var loadMoreView: TextView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F)
        setTextColor(Color.BLUE)
        text = loadMoreText
        layoutParams = MarginLayoutParams(MarginLayoutParams.WRAP_CONTENT, MarginLayoutParams.WRAP_CONTENT)
    }

    var loadMoreText = "More..."
        set(value) {
            field = value
            loadMoreView.text = value
        }


    init {
        attrs?.also {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ContainerView)
            itemSpacing = typedArray.getDimensionPixelSize(R.styleable.ContainerView_itemSpacing, DEFAULT_ITEM_SPACING)
            lineSpacing = typedArray.getDimensionPixelOffset(R.styleable.ContainerView_lineSpacing, DEFAULT_LINE_SPACING)
            enableLazyLoading = typedArray.getBoolean(R.styleable.ContainerView_enableLazyLoading, false)
            typedArray.recycle()
        }
    }

    override fun removeAllViews() {
        super.removeAllViews()
        childrenBounds.clear()
        childrenBoundMaxHeight.clear()
        childrenBoundMap.clear()
    }

    override fun addView(child: View, index: Int, params: LayoutParams?) {
        super.addView(child, index, params)
        childrenBounds.add(RectF())
        child.setVisible(false)
    }

    private fun getMapChildren(index: Int) : ArrayList<RectF> {
        if (childrenBoundMap.containsKey(index)) {
            return childrenBoundMap[index]!!
        }
        val newChildren = ArrayList<RectF>()
        childrenBoundMap[index] = newChildren
        return newChildren
    }

    fun addSubviews(views: List<View>) {
        removeAllViews()
        for (view in views) {
            addView(view)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val specWidthSize = MeasureSpec.getSize(widthMeasureSpec)       // 父view宽度
        val specWidthMode = MeasureSpec.getMode(widthMeasureSpec)       // 父view宽度模式

        var maxWidthUsed: Int = paddingStart
        var heightUsed: Int = paddingTop
        var lineWidthUsed: Int = paddingStart
        var lineMaxHeight: Int = 0
        var mapIndex = 0

        childrenBoundMap.clear()
        childrenBoundMaxHeight.clear()

        var leftWidth: Int
        var measuredWidth: Int
        var measuredHeight: Int
        var measuredView: View

        for ((index, child) in children.withIndex()) {
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
            leftWidth = specWidthSize - (paddingEnd + lineWidthUsed)

            measuredView = child
            if (child.tag == VIEW_NEW_LINE || (specWidthMode != MeasureSpec.UNSPECIFIED && leftWidth < child.measuredWidth)) {
                // parent width is not enough to fill one view
                if (child.tag != VIEW_NEW_LINE && (lineWidthUsed == paddingStart)) {
                    child.layoutParams.width = leftWidth
                    maxWidthUsed = leftWidth
                } else {
                    if (lineWidthUsed > paddingStart) {
                        maxWidthUsed = max(maxWidthUsed, lineWidthUsed-itemSpacing)
                    }
                    lineWidthUsed = paddingStart
                    heightUsed += lineMaxHeight
                    if (child.tag != VIEW_NEW_LINE) {
                        heightUsed += lineSpacing
                    }
                    childrenBoundMaxHeight[mapIndex] = lineMaxHeight
                    lineMaxHeight = 0
                    mapIndex++
                    measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
                    leftWidth = specWidthSize - (paddingEnd + lineWidthUsed)
                }
            }
            if (child == loadMoreView) {
                (child.layoutParams as MarginLayoutParams).marginStart = (leftWidth - child.measuredWidth)
            }

            measuredWidth = child.measuredWidth
            measuredHeight = child.measuredHeight
            val childBounds = childrenBounds[index]
            childBounds.set(
                lineWidthUsed.toFloat(),
                heightUsed.toFloat(),
                (lineWidthUsed + measuredWidth).toFloat(),
                (heightUsed + measuredHeight).toFloat()
            )
            getMapChildren(mapIndex).add(childBounds)
            lineWidthUsed += measuredWidth + itemSpacing
            lineMaxHeight = max(lineMaxHeight, measuredHeight)
            measuredView.setVisible(true)
            if (measuredView == loadMoreView) {
                break
            }
        }

        if (lineWidthUsed > paddingStart) {
            maxWidthUsed = max(maxWidthUsed, lineWidthUsed-itemSpacing)
        }
        val viewsWidth = maxWidthUsed + paddingEnd
        val widthMatchParent = layoutParams.width != LayoutParams.WRAP_CONTENT
        val selfWidth = if (widthMatchParent) {
            specWidthSize
        } else {
            viewsWidth
        }


        // vertically center all views in the line
        childrenBoundMaxHeight[mapIndex] = lineMaxHeight
        for ((index, bounds) in childrenBoundMap.entries) {
            val maxHeight = childrenBoundMaxHeight[index]!!
            for (bound in bounds) {
                if (bound.height() < maxHeight) {
                    bound.top += (maxHeight - bound.height()) / 2.0F
                    bound.bottom += (maxHeight - bound.height()) / 2.0F
                }
            }
            val offset: Float = when (contentAlignment) {
                ContentAlignment.Center -> {
                    (selfWidth - paddingEnd - bounds.last().right) / 2f
                }
                ContentAlignment.End -> {
                    (selfWidth - bounds.last().right - paddingEnd)
                }
                ContentAlignment.Start -> {
                    continue
                }
            }
            for (bound in bounds) {
                bound.left += offset
                bound.right += offset
            }
        }

        val selfHeight = heightUsed + lineMaxHeight + paddingBottom
        setMeasuredDimension(selfWidth, selfHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (child in children.withIndex()) {
            if (!child.value.isVisible) {
                return
            }
            val bound = childrenBounds[child.index]
            child.value.layout(bound.left.toInt(), bound.top.toInt(),
                                    bound.right.toInt(), bound.bottom.toInt())
        }
    }


    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }
}