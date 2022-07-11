package io.ffem.lite.ui

import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.recyclerview.widget.RecyclerView
import io.ffem.lite.R
import io.ffem.lite.model.TestInfo
import io.ffem.lite.model.TestType
import io.ffem.lite.preference.isDiagnosticMode
import io.ffem.lite.util.toLocalString
import io.github.luizgrp.sectionedrecyclerviewadapter.Section
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters
import java.util.regex.Pattern

internal class TestInfoSection(
    private val title: String, private val list: List<TestInfo>,
    private val clickListener: (TestInfoSection, Int) -> Unit
) : Section(
    SectionParameters.builder()
        .itemResourceId(R.layout.test_item)
        .headerResourceId(R.layout.section_header)
        .build()
) {
    override fun getContentItemsTotal(): Int {
        return list.size
    }

    override fun getItemViewHolder(view: View): RecyclerView.ViewHolder {
        return ItemViewHolder(view)
    }

    override fun onBindItemViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val itemHolder = holder as ItemViewHolder

        val testInfo = list[position]

        itemHolder.rootView.setOnClickListener {
            clickListener(
                this,
                itemHolder.adapterPosition
            )
        }

        if (testInfo.subtype == TestType.TITRATION || testInfo.subtype == TestType.API) {
            itemHolder.subtitleView.setText(R.string.titration)
        } else {
            var subTitle = testInfo.minMaxRange
            if (testInfo.minMaxRange.isNotEmpty()) {
                val matcher =
                    Pattern.compile("<dilutionRange>(.*?)</dilutionRange>").matcher(subTitle)
                if (matcher.find()) {
                    subTitle = matcher.replaceAll(
                        String.format(
                            itemHolder.contentView.context.resources
                                .getString(R.string.up_to_with_dilution), matcher.group(1)
                        )
                    )
                }
            }
            itemHolder.subtitleView.text = subTitle

            when {
                testInfo.subTest().timeDelay > 59 -> {
                    val subTitle2 = (testInfo.subTest().timeDelay / 60).toString() + " " +
                            itemHolder.contentView.context.getString(R.string.minutes)
                    itemHolder.subtitle2View.text = subTitle2
                }
                testInfo.subTest().timeDelay > 0 -> {
                    val subTitle2 = testInfo.subTest().timeDelay.toString() + " " +
                            itemHolder.contentView.context.getString(R.string.seconds)
                    itemHolder.subtitle2View.text = subTitle2
                }
                else -> {
                    itemHolder.subtitle2View.visibility = GONE
                }
            }
        }

        if (isDiagnosticMode()) {
            if (!testInfo.subTest().formula.isNullOrEmpty()) {
                var text = "Formula: " + testInfo.subTest().formula
                text = text.replace("%1\$f", "Result").replace("*", "x")
                itemHolder.formulaView.text = text
                itemHolder.formulaView.visibility = VISIBLE
            } else {
                itemHolder.formulaView.visibility = GONE
            }

            itemHolder.parameterIdView.text = testInfo.uuid
            itemHolder.parameterIdView.visibility = VISIBLE
        }

        itemHolder.contentView.text = testInfo.name!!.toLocalString()

        itemHolder.layoutRow.setOnClickListener { clickListener(this, position) }
    }

    override fun getHeaderViewHolder(view: View): RecyclerView.ViewHolder {
        return HeaderViewHolder(view)
    }

    override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder) {
        val headerHolder = holder as HeaderViewHolder
        headerHolder.titleText.text = title
    }

    internal fun getItemAt(i: Int): TestInfo {
        return list[i]
    }

    internal interface ClickListener {
        fun onItemRootViewClicked(section: TestInfoSection, itemAdapterPosition: Int)
    }
}