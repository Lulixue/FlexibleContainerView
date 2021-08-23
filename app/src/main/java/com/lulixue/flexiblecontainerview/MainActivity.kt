package com.lulixue.flexiblecontainerview

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.lulixue.flexiblecontainerview.databinding.ActivityMainBinding

const val LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod " +
        "tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, " +
        "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo " +
        "consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse " +
        "cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat " +
        "non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."

val TEXT_COLORS = arrayOf(Color.BLACK, Color.BLUE, Color.RED, Color.GRAY, Color.GREEN, Color.YELLOW)
val TEXT_SIZES = arrayOf(14f, 15f, 16f, 17f, 18f, 19f)
class MainActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fillSplitTextViews()
    }

    private fun fillSplitTextViews() {
        val split = LOREM_IPSUM.split(" ", ".", ",").filter { it.isNotEmpty() }

        val views = ArrayList<View>()
        val lp = ViewGroup.MarginLayoutParams(
            ViewGroup.MarginLayoutParams.WRAP_CONTENT,
            ViewGroup.MarginLayoutParams.WRAP_CONTENT
        )
        for (single in split) {
            val view = TextView(this).apply {
                layoutParams = lp
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                text = single
            }
            views.add(view)
        }
        binding.containerView.addSubviews(views)
    }




}