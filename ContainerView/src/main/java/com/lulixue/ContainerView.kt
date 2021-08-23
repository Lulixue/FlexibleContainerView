package com.lulixue

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.isVisible
import kotlin.math.max
import kotlin.math.min

class ContainerView(context: Context, attrs: AttributeSet?) : ViewGroup(context, attrs) {
    constructor(context: Context) : this(context, null)
    companion object {
        private val Float.dp
            get() = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, this, Resources.getSystem().displayMetrics
            )
        private fun View.setVisible(visible: Boolean) {
            this.visibility = if (visible) VISIBLE else GONE
        }
        private val Int.dp
            get() = this.toFloat().dp

        private val Int.rect
            get() = Rect(this, this, this, this)

        private const val VIEW_NEW_LINE = "NewLine"
        fun getSeparatorView(context: Context, height: Int): View {
            return View(context).apply {
                tag = VIEW_NEW_LINE
                layoutParams = MarginLayoutParams(height, MarginLayoutParams.WRAP_CONTENT)
            }
        }
        fun getNewLineView(context: Context): View {
            return getSeparatorView(context, 0)
        }
    }

    private val childrenBounds = mutableListOf<Rect>()
    private val childrenBoundMap = LinkedHashMap<Int, ArrayList<Rect>>()
    private val childrenBoundMaxHeight = LinkedHashMap<Int, Int>()
    private var itemPadding = 1.dp.toInt().rect
    var enableLazyLoading = false
    set(value) {
        field = value
        requestLayout()
    }
    var loadMoreView: View = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F)
        setTextColor(Color.BLUE)
        layoutParams = MarginLayoutParams(MarginLayoutParams.WRAP_CONTENT, MarginLayoutParams.WRAP_CONTENT)
    }

    init {
        attrs?.also {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ContainerView)
            itemPadding = typedArray.getDimensionPixelSize(R.styleable.ContainerView_itemPadding, 1.dp.toInt()).rect
            itemPadding.left = typedArray.getDimensionPixelOffset(R.styleable.ContainerView_itemPaddingStart, itemPadding.left)
            itemPadding.right = typedArray.getDimensionPixelOffset(R.styleable.ContainerView_itemPaddingEnd, itemPadding.right)
            itemPadding.top = typedArray.getDimensionPixelOffset(R.styleable.ContainerView_itemPaddingTop, itemPadding.top)
            itemPadding.bottom = typedArray.getDimensionPixelOffset(R.styleable.ContainerView_itemPaddingBottom, itemPadding.bottom)
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
        childrenBounds.add(Rect())
        child.setVisible(false)
    }

    fun setItemPadding(padding: Int) {
        setItemPadding(padding, padding, padding, padding)
    }
    fun setItemPadding(left: Int, top: Int, right: Int, bottom: Int) {
        itemPadding.left = left
        itemPadding.right = right
        itemPadding.top = top
        itemPadding.bottom = bottom
        requestLayout()
    }

    private fun getMapChildren(index: Int) : ArrayList<Rect> {
        if (childrenBoundMap.containsKey(index)) {
            return childrenBoundMap[index]!!
        }
        val newChildren = ArrayList<Rect>()
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

        var maxWidthUsed = 0
        var heightUsed = paddingTop + itemPadding.top
        var lineWidthUsed = paddingStart + itemPadding.left
        var lineMaxHeight = 0
        var mapIndex = 0

        childrenBoundMap.clear()
        childrenBoundMaxHeight.clear()

        var leftWidth: Int
        var measuredWidth: Int
        var measuredHeight: Int
        var measuredView: View

        for ((index, child) in children.withIndex()) {
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
            leftWidth = specWidthSize - (paddingEnd + lineWidthUsed + itemPadding.right)

            measuredView = child
            if (child.tag == VIEW_NEW_LINE || (specWidthMode != MeasureSpec.UNSPECIFIED && leftWidth < child.measuredWidth)) {
                // 当前控件宽度不足以安放一个子view
                if (child.tag != VIEW_NEW_LINE && (lineWidthUsed == paddingStart + itemPadding.left)) {
                    child.layoutParams.width = leftWidth
                } else {
                    lineWidthUsed = paddingStart + itemPadding.left
                    heightUsed += lineMaxHeight + itemPadding.top
                    childrenBoundMaxHeight[mapIndex] = lineMaxHeight
                    lineMaxHeight = 0
                    mapIndex++
                    measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
                    leftWidth = specWidthSize - (paddingEnd + lineWidthUsed + itemPadding.right)
                }
            }
            if (child == loadMoreView) {
                (child.layoutParams as MarginLayoutParams).marginStart = (leftWidth - child.measuredWidth)
            }

            measuredWidth = child.measuredWidth
            measuredHeight = child.measuredHeight
            val childBounds = childrenBounds[index]
            childBounds.set(
                lineWidthUsed,
                heightUsed,
                lineWidthUsed + measuredWidth,
                heightUsed + measuredHeight
            )
            getMapChildren(mapIndex).add(childBounds)
            lineWidthUsed += measuredWidth + itemPadding.right
            maxWidthUsed = max(maxWidthUsed, lineWidthUsed)
            lineMaxHeight = max(lineMaxHeight, measuredHeight)
            measuredView.setVisible(true)
            if (measuredView == loadMoreView) {
                break
            }
        }

        childrenBoundMaxHeight[mapIndex] = lineMaxHeight
        for ((index, bounds) in childrenBoundMap.entries) {
            val maxHeight = childrenBoundMaxHeight[index]!!
            for (bound in bounds) {
                if (bound.height() < maxHeight) {
                    bound.top += (maxHeight - bound.height()) / 2
                    bound.bottom += (maxHeight - bound.height()) / 2
                }
            }
        }
        val viewsWidth = maxWidthUsed + paddingEnd + itemPadding.right
        val widthMatchParent = layoutParams.width == LayoutParams.MATCH_PARENT
        val selfWidth = if (widthMatchParent) {
            specWidthSize
        } else {
            viewsWidth
        }
        val selfHeight = heightUsed + lineMaxHeight + paddingBottom + itemPadding.bottom
        setMeasuredDimension(selfWidth, selfHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (child in children.withIndex()) {
            if (!child.value.isVisible) {
                return
            }
            val bound = childrenBounds[child.index]
            child.value.layout(bound.left, bound.top, bound.right, bound.bottom)
        }
    }


    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }
}