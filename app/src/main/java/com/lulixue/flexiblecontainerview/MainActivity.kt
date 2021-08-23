package com.lulixue.flexiblecontainerview

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import com.google.android.material.tabs.TabLayout
import com.lulixue.ContainerView
import com.lulixue.flexiblecontainerview.databinding.ActivityMainBinding
import kotlin.random.Random

const val LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod " +
        "tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, " +
        "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo " +
        "consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse " +
        "cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat " +
        "non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."

val TEXT_COLORS = arrayOf(Color.BLACK, Color.BLUE, Color.RED, Color.GRAY, Color.GREEN, Color.YELLOW)
val TEXT_SIZES = arrayOf(14f, 15f, 16f, 17f, 18f, 19f)

fun getRandomColor(): Int {
    val next = Random.nextInt(TEXT_COLORS.size)
    return TEXT_COLORS[next]
}

fun getRandomSize(): Float {
    val next = Random.nextInt(TEXT_SIZES.size)
    return TEXT_SIZES[next]
}

fun View.setVisible(visible: Boolean) {
    this.visibility = if (visible) View.VISIBLE else View.GONE
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val radioButtons: Array<RadioButton> by lazy {
        arrayOf(binding.leftRadio, binding.centerRadio, binding.rightRadio)
    }
    private var contentAlignment = ContainerView.ContentAlignment.Start
        set(value) {
            field = value
            binding.containerView.contentAlignment = value
            binding.nestContainerView.contentAlignment = value
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fillSplitTextViews()
        fillScrollTextView()

        binding.leftRadio.setOnClickListener {
            val radio = it as RadioButton
            checkRadio(radio)
            contentAlignment = ContainerView.ContentAlignment.Start
        }
        binding.centerRadio.setOnClickListener {
            val radio = it as RadioButton
            checkRadio(radio)
            contentAlignment = ContainerView.ContentAlignment.Center
        }
        binding.rightRadio.setOnClickListener {
            val radio = it as RadioButton
            checkRadio(radio)
            contentAlignment = ContainerView.ContentAlignment.End
        }

        binding.tabViewMode.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val scroll = binding.tabViewMode.selectedTabPosition == 1
                binding.scrollView.setVisible(scroll)
                binding.containerView.setVisible(!scroll)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

        })
    }

    private fun checkRadio(radio: RadioButton) {
        radioButtons.forEach {
            it.isChecked = it == radio
        }
    }

    private fun fillScrollTextView() {
        val views = ArrayList<View>()
        views.addAll(getSplitTextViews())
        views.addAll(getSplitTextViews())
        binding.nestContainerView.addSubviews(views)
    }

    private fun getSplitTextViews(): List<View> {
        val split = LOREM_IPSUM.split(" ").filter { it.isNotEmpty() }

        val views = ArrayList<View>()
        val lp = ViewGroup.MarginLayoutParams(
            ViewGroup.MarginLayoutParams.WRAP_CONTENT,
            ViewGroup.MarginLayoutParams.WRAP_CONTENT
        )
        for (single in split) {
            val view = TextView(this).apply {
                layoutParams = lp
                setTextSize(TypedValue.COMPLEX_UNIT_SP, getRandomSize())
                text = single
                setTextColor(getRandomColor())
            }
            views.add(view)
            if (single.contains(".")) {
                views.add(ContainerView.getNewLineView(this).apply {
                    setBackgroundColor(getRandomColor())
                })
            }
        }
        return views
    }

    private fun fillSplitTextViews() {
        val views = getSplitTextViews()
        binding.containerView.addSubviews(views)
    }




}