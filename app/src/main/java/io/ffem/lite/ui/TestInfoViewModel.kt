package io.ffem.lite.ui

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.text.SpannableString
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import io.ffem.lite.data.AppDatabase
import io.ffem.lite.data.CalibrationDatabase
import io.ffem.lite.data.TestResult
import io.ffem.lite.model.Instruction
import io.ffem.lite.model.ResultInfo
import io.ffem.lite.model.TestInfo
import io.ffem.lite.util.StringUtil.getStringResourceByName
import io.ffem.lite.util.StringUtil.toInstruction
import io.ffem.lite.widget.InstructionRowView
import java.util.regex.Pattern

class TestInfoViewModel(application: Application) : AndroidViewModel(application) {
    lateinit var db: AppDatabase
    lateinit var form: TestResult

    var currentResult: ResultInfo? = null
    var calibrationColor: Int = 0
    var sampleColor: Int = 0
    var resultInfoList: ArrayList<ResultInfo> = ArrayList()
    var isCalibration: Boolean = false

    @JvmField
    val test = ObservableField<TestInfo>()
    var calibrationPoint: Double = 0.0

    fun setTest(testInfo: TestInfo) {
        test.set(testInfo)
        Companion.testInfo = testInfo
    }

    fun loadCalibrations() {
        val db = CalibrationDatabase.getDatabase(getApplication())
        try {
            val dao = db.calibrationDao()
            val calibrationInfo = dao.getCalibrations(testInfo.uuid)
            if (calibrationInfo != null) {
                for (colorItem in testInfo.subTest().colors) {
                    colorItem.color = Color.TRANSPARENT
                }

                for (calibration in calibrationInfo.calibrations) {
                    for (colorItem in testInfo.subTest().colors) {
                        if (colorItem.value == calibration.value) {
                            colorItem.color = calibration.color
                        }
                    }
                }
            }
        } finally {
            db.close()
        }
    }

//    fun getTestInfo(id: String?): TestInfo? {
//        testInfo = null
//        val input = getApplication<Application>().resources.openRawResource(R.raw.water_tests)
//        val content = FileUtil.readTextFile(input)
//        val tests = Gson().fromJson(content, TestConfig::class.java).tests
//        for (test in tests) {
//            if (test.uuid == id) {
//                setTest(test)
//                break
//            }
//        }
//
//        return testInfo
//    }

    fun setContent(linearLayout: LinearLayout, instruction: Instruction?) {
        if (instruction?.section == null) {
            return
        }
        val context = linearLayout.context
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val size = Point()
        windowManager.defaultDisplay?.getRealSize(size)
        val displayMetrics = context.resources.displayMetrics
        for (i in instruction.section.indices) {
            var text = instruction.section[i]
            val tag = Pattern.compile("~.*?~").matcher(text)
            when {
                tag.find() -> {
                    return
                }
                text.contains("image:") -> {
                    insertImage(linearLayout, context, size, displayMetrics, i, text)
                }
                else -> {
                    val rowView = InstructionRowView(context)
                    val m = Pattern.compile("^(\\d*?[a-zA-Z]{1,3}\\.\\s*)(.*)").matcher(text)
                    val m1 = Pattern.compile("^(\\d+?\\.\\s*)(.*)").matcher(text)
                    val m2 = Pattern.compile("^(\\.\\s*)(.*)").matcher(text)

                    when {
                        m.find() -> {
                            rowView.setNumber(m.group(1)?.trim { it <= ' ' })
                            text = m.group(2)?.trim { it <= ' ' }.toString()
                        }
                        m1.find() -> {
                            rowView.setNumber(m1.group(1)?.trim { it <= ' ' })
                            text = m1.group(2)?.trim { it <= ' ' }.toString()
                        }
                        m2.find() -> {
                            rowView.setNumber("   ")
                            text = m2.group(2)?.trim { it <= ' ' }.toString()
                        }
                    }

                    val sentences = "$text. ".split("\\.\\s+".toPattern()).toTypedArray()
                    val labelView = LinearLayout(context)
                    for (j in sentences.indices) {

                        if (sentences[j].isEmpty()) {
                            continue
                        }

                        if (j > 0) {
                            rowView.append(SpannableString(" "))
                        }

                        rowView.append(
                            toInstruction((context as AppCompatActivity),
                                testInfo, sentences[j].trim { it <= ' ' })
                        )
                        val sentence = getStringResourceByName(context, sentences[j]).toString()
                        if (sentence.contains("[/a]")) {
                            rowView.enableLinks()
                        }
                    }

                    // set an id for the view to be able to find it for unit testing
                    rowView.id = i
                    linearLayout.addView(rowView)
                    linearLayout.addView(labelView)
                }
            }
        }
    }

    companion object {
        private var testInfo: TestInfo = TestInfo()

//        @JvmStatic
//        @BindingAdapter("testSubtitle")
//        fun setSubtitle(view: TextView, testInfo: TestInfo) {
//            var subTitle = testInfo.minMaxRange
//            //        if (testInfo.getBrand() != null) {
////            subTitle = testInfo.getBrand() + ", ";
////        }
//            if (testInfo.minMaxRange.isNotEmpty()) {
//                val matcher =
//                        Pattern.compile("<dilutionRange>(.*?)</dilutionRange>").matcher(subTitle)
//                if (matcher.find()) {
//                    subTitle = matcher.replaceAll(
//                            String.format(
//                                    view.resources
//                                            .getString(R.string.up_to_with_dilution), matcher.group(1)
//                            )
//                    )
//                }
//            }
//            view.text = subTitle
//        }

        private fun insertImage(
            linearLayout: LinearLayout, context: Context, size: Point,
            displayMetrics: DisplayMetrics, i: Int, text: String
        ) {
            val imageName = text.substring(text.indexOf(":") + 1)
            val resourceId = context.resources.getIdentifier(
                "drawable/in_$imageName",
                "id", context.packageName
            )
            if (resourceId > 0) {
                var divisor = 3.0
                if (displayMetrics.densityDpi > 250) {
                    divisor = 2.4
                }
                if (size.y > displayMetrics.heightPixels) {
                    divisor += 0.3
                }
                val llp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (displayMetrics.heightPixels / divisor).toInt()
                )
                llp.setMargins(0, 0, 0, 20)
                val imageView = AppCompatImageView(context)
                imageView.setImageResource(resourceId)
                imageView.layoutParams = llp
                imageView.contentDescription = imageName

                // set an id for the view to be able to find it for unit testing
                imageView.id = i
                linearLayout.addView(imageView)
            }
        }

//        /**
//         * Sets the image scale.
//         *
//         * @param imageView the image view
//         * @param scaleType the scale type
//         */
//        @JvmStatic
//        @BindingAdapter("imageScale")
//        fun setImageScale(imageView: ImageView, scaleType: String?) {
//            if (scaleType != null) {
//                imageView.scaleType =
//                        if ("fitCenter" == scaleType) ImageView.ScaleType.FIT_CENTER else ImageView.ScaleType.CENTER_CROP
//            } else {
//                imageView.scaleType = ImageView.ScaleType.FIT_CENTER
//            }
//        }

//        private fun setImage(imageView: ImageView, theName: String?) {
//            if (theName != null) {
//                val context = imageView.context
//                try {
//                    val name = theName.replace(" ", "-")
//                    val ims = context.assets.open(name)
//                    imageView.setImageDrawable(Drawable.createFromStream(ims, null))
//                } catch (e: IOException) {
//                    e.printStackTrace()
//                }
//            }
//        }
    }
}