// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.ui

import com.intellij.openapi.wm.ToolWindow
import java.util.*
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel


class MergeRequestsToolWindow(toolWindow: ToolWindow) {
    private var refreshToolWindowButton: JButton? = null
    private var hideToolWindowButton: JButton? = null
    private var currentDate: JLabel? = null
    private var currentTime: JLabel? = null
    private var timeZone: JLabel? = null
    private var myToolWindowContent: JPanel? = null

    init {
        hideToolWindowButton!!.addActionListener { toolWindow.hide(null) }
        refreshToolWindowButton!!.addActionListener { currentDateTime() }
        currentDateTime()
    }

    fun currentDateTime() {
        // Get current date and time
        val instance: Calendar = Calendar.getInstance()
        currentDate!!.text = (instance.get(Calendar.DAY_OF_MONTH).toString() + "/"
                + (instance.get(Calendar.MONTH) + 1) + "/"
                + instance.get(Calendar.YEAR))
        currentDate!!.icon = ImageIcon(javaClass.getResource("/Calendar-icon.png"))
        val min: Int = instance.get(Calendar.MINUTE)
        val strMin = if (min < 10) "0$min" else min.toString()
        currentTime!!.text = instance.get(Calendar.HOUR_OF_DAY).toString() + ":" + strMin
        currentTime!!.icon = ImageIcon(javaClass.getResource("/Time-icon.png"))
        // Get time zone
        val gmt_Offset: Long = instance.get(Calendar.ZONE_OFFSET).toLong() // offset from GMT in milliseconds
        var str_gmt_Offset = (gmt_Offset / 3600000).toString()
        str_gmt_Offset = if (gmt_Offset > 0) "GMT + $str_gmt_Offset" else "GMT - $str_gmt_Offset"
        timeZone!!.text = str_gmt_Offset
        timeZone!!.icon = ImageIcon(javaClass.getResource("/Time-zone-icon.png"))
    }

    fun getContent(): JPanel? {
        return myToolWindowContent
    }
}