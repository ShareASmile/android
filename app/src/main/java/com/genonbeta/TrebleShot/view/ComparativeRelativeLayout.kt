/*
 * Copyright (C) 2019 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.genonbeta.TrebleShot.view

import android.content.*
import android.content.res.TypedArray
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.genonbeta.TrebleShot.R

/**
 * created by: Veli
 * date: 27.03.2018 22:32
 */
class ComparativeRelativeLayout(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {
    private var alwaysUseWidth = true

    private var baseOnSmaller = false

    private var tallerExtraLength = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Set a proportional layout.
        var widthMeasureSpec = widthMeasureSpec
        var heightMeasureSpec = heightMeasureSpec
        if (baseOnSmaller) {
            if (widthMeasureSpec > heightMeasureSpec) widthMeasureSpec =
                heightMeasureSpec + tallerExtraLength else if (heightMeasureSpec > widthMeasureSpec) heightMeasureSpec =
                widthMeasureSpec + tallerExtraLength
        } else if (alwaysUseWidth) heightMeasureSpec = widthMeasureSpec + tallerExtraLength else widthMeasureSpec =
            heightMeasureSpec + tallerExtraLength
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    init {
        val typedAttributes: TypedArray = context.theme
            .obtainStyledAttributes(attrs, R.styleable.ComparativeRelativeLayout, defStyleAttr, 0)
        baseOnSmaller = typedAttributes.getBoolean(
            R.styleable.ComparativeRelativeLayout_baseOnSmallerLength, baseOnSmaller
        )
        tallerExtraLength = typedAttributes.getDimensionPixelSize(
            R.styleable.ComparativeRelativeLayout_tallerLengthExtra, tallerExtraLength
        )
        alwaysUseWidth = typedAttributes.getBoolean(
            R.styleable.ComparativeRelativeLayout_alwaysUseWidth, alwaysUseWidth
        )
    }
}