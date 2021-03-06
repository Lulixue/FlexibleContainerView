package com.lulixue

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import java.lang.ref.WeakReference
import kotlin.math.max

class ContainerView(context: Context, attrs: AttributeSet?) : ViewGroup(context, attrs) {
    constructor(context: Context) : this(context, null)

    enum class ContentAlignment(val value: Int) {
        Start(0),
        Center(1),
        End(2)
    }
    companion object {
        private val Float.dp
            get() = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, this, Resources.getSystem().displayMetrics
            )
        private val Int.dp
            get() = this.toFloat().dp
        private fun View.setVisible(visible: Boolean) {
            this.visibility = if (visible) VISIBLE else GONE
        }

        private const val DEFAULT_LOAD_MORE_TEXT = "More..."
        private const val DEFAULT_INIT_LAZY_ITEM_SIZE = 150
        private const val DEFAULT_LAZY_LOAD_ITEM_SIZE = 50
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
    private val lazyLoadViews = ArrayList<WeakReference<View>>()
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
    var initLazyLoadItemSize: Int = DEFAULT_INIT_LAZY_ITEM_SIZE
        set(value) {
            if (value == field) {
                return
            }
            field = value
        }
    var lazyLoadMoreItemSize: Int = DEFAULT_LAZY_LOAD_ITEM_SIZE
        set(value) {
            if (value == field) {
                return
            }
            field = value
        }

    var contentAlignment: ContentAlignment = ContentAlignment.Start
        set(value) {
            if (value == field) {
                return
            }
            field = value
            requestLayout()
        }

    var loadMoreView: View = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F)
        setTextColor(Color.WHITE)
        setTypeface(Typeface.DEFAULT, Typeface.ITALIC)
        setPadding(5.dp.toInt())
        text = loadMoreText
        background = ContextCompat.getDrawable(context, R.drawable.load_more_bg)
        layoutParams = MarginLayoutParams(MarginLayoutParams.WRAP_CONTENT, MarginLayoutParams.WRAP_CONTENT)
    }
    set(value) {
        field = value
        value.setOnClickListener { loadMore() }
    }

    var loadMoreText = DEFAULT_LOAD_MORE_TEXT
        set(value) {
            field = value
            if (loadMoreView is TextView) {
                (loadMoreView as TextView).text = value
            }
        }

    init {
        loadMoreView.setOnClickListener { loadMore() }
        attrs?.also {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ContainerView)
            itemSpacing = typedArray.getDimensionPixelSize(R.styleable.ContainerView_itemSpacing, DEFAULT_ITEM_SPACING)
            lineSpacing = typedArray.getDimensionPixelOffset(R.styleable.ContainerView_lineSpacing, DEFAULT_LINE_SPACING)
            enableLazyLoading = typedArray.getBoolean(R.styleable.ContainerView_enableLazyLoading, false)
            loadMoreText = typedArray.getString(R.styleable.ContainerView_loadMoreText) ?: DEFAULT_LOAD_MORE_TEXT
            initLazyLoadItemSize = typedArray.getInteger(R.styleable.ContainerView_initLazyLoadItemSize, DEFAULT_INIT_LAZY_ITEM_SIZE)
            lazyLoadMoreItemSize = typedArray.getInteger(R.styleable.ContainerView_lazyLoadMoreItemSize, DEFAULT_LAZY_LOAD_ITEM_SIZE)
            typedArray.recycle()
        }
    }

    override fun removeAllViews() {
        super.removeAllViews()
        childrenBounds.clear()
        childrenBoundMaxHeight.clear()
        childrenBoundMap.clear()
        lazyLoadViews.clear()
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

    fun loadMore() {
        removeView(loadMoreView)
        var addedCount = 0
        val leftViews = ArrayList<WeakReference<View>>()
        leftViews.addAll(lazyLoadViews)
        for (value in leftViews) {
            lazyLoadViews.remove(value)
            value.get()?.also {
                addView(it)
                addedCount ++
                if (addedCount >= lazyLoadMoreItemSize) {
                    addView(loadMoreView)
                    return
                }
            }
        }
    }

    fun addSubviews(views: List<View>) {
        removeAllViews()
        var addView = true
        for (view in views) {
            if (addView) {
                addView(view)
                if (enableLazyLoading && children.count() >= initLazyLoadItemSize) {
                    addView(loadMoreView)
                    addView = false
                }
                continue
            } else {
                lazyLoadViews.add(WeakReference(view))
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val specWidthSize = MeasureSpec.getSize(widthMeasureSpec)       // parent view width
        val specWidthMode = MeasureSpec.getMode(widthMeasureSpec)       // parent view width mode

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

            if (enableLazyLoading) {
                if (child == loadMoreView) {
                    (child.layoutParams as MarginLayoutParams).marginStart = (leftWidth - child.measuredWidth)
                    break
                }
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