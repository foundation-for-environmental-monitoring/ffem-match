package io.ffem.lite.helper

import android.content.Context
import io.ffem.lite.model.Instruction
import io.ffem.lite.model.PageIndex
import io.ffem.lite.model.TestInfo
import io.ffem.lite.model.TestType
import io.ffem.lite.preference.AppPreferences

object InstructionHelper {

    fun setupInstructions(
        testInfo: TestInfo, isExternalSurvey: Boolean, instructions: ArrayList<Instruction>,
        pageIndex: PageIndex, currentDilution: Int, isCalibration: Boolean, redo: Boolean,
        context: Context
    ) {
        var instructionIndex = 0
        instructions.clear()
        pageIndex.clear()

        val list = ArrayList<Instruction>()
        if (!redo) {
            list.add(Instruction("<list>", ""))
        }

        if (testInfo.subtype == TestType.CARD) {
            list.add(Instruction("<calibrateOption>", ""))
        }

        if (isCalibration && testInfo.subtype != TestType.CARD) {
            list.add(Instruction("<calibration>", ""))
        }

        if (testInfo.instructions!!.isEmpty()) {
            if (testInfo.dilutions.isNotEmpty()) {
                list.add(Instruction("<dilution>", ""))
            }
            if (testInfo.subtype == TestType.CARD) {
                list.add(Instruction("<instruction>", ""))
            }
            if (AppPreferences.useFaceDownMode(context) && testInfo.subTest().timeDelay > 10) {
                list.add(Instruction("c_start_timer,<start_timer>", ""))
            }
            list.add(Instruction("<test>", ""))
            if (testInfo.subtype == TestType.CARD) {
                list.add(Instruction("<confirm>", ""))
            }
            list.add(Instruction("<result>", ""))

            list.add(Instruction(value = "c_empty", "image:c_empty"))

        } else {
            for (instruction in testInfo.instructions!!) {
                list.add(instruction)
            }
        }

        for (element in list) {
            var instruction: Instruction
            var text1 = ""
            try {
                val sections: ArrayList<String> = ArrayList()
                for (section in element.section) {
                    sections.add(section)
                }
                instruction = element.copy()
                instruction.section = sections
                val text = instruction.section[0]
                if (instruction.section.size > 1) {
                    text1 = instruction.section[1]
                }
                if (text.contains("<calibrateOption>")) {
                    pageIndex.calibrateOptionPage = instructionIndex
                } else if (text.contains("<confirm>")) {
                    pageIndex.confirmationPage = instructionIndex
                } else if (text.contains("<instruction>")) {
                    pageIndex.instruction = instructionIndex
                } else if (text.contains("<list>")) {
                    pageIndex.listPage = instructionIndex
                } else if (text1.contains("<start_timer>")) {
                    pageIndex.startTimerPage = instructionIndex
                } else if (text.contains("<calibration>")) {
                    pageIndex.calibrationPage = instructionIndex
                    pageIndex.instructionFirstIndex = instructionIndex
                } else if (text.contains("<test>")) {
                    pageIndex.testPage = instructionIndex
                } else if (text.contains("<result>")) {
                    pageIndex.resultPage = instructionIndex
                    if (!isExternalSurvey) {
                        pageIndex.submitPage = instructionIndex + 1
                    }
                } else if (text.contains("<dilution>")) {
                    if (isCalibration) {
                        continue
                    } else {
                        pageIndex.dilutionPage = instructionIndex
                        pageIndex.instructionFirstIndex = instructionIndex
                    }
                } else if (currentDilution == 1 && text.contains("dilution")) {
                    continue
                } else if (currentDilution != 1 && text.contains("normal")) {
                    continue
                } else if (pageIndex.resultPage < 1) {
                    if (instructionIndex == 0) {
                        pageIndex.instructionFirstIndex = 1
                    }
                    instruction.section[0] =
                        (instructionIndex + pageIndex.instructionFirstIndex)
                            .toString() + ". " + instruction.section[0]
                }

                instructions.add(instruction)
                instructionIndex++
            } catch (e: CloneNotSupportedException) {
                e.printStackTrace()
            }
        }

//        if (redo) {
//            pageIndex.listPage = -1
//        }
        pageIndex.totalPageCount = instructions.size
    }
}