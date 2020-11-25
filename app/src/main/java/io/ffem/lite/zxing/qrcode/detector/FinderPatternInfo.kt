/*
 * Copyright 2007 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.ffem.lite.zxing.qrcode.detector

import kotlin.math.max

/**
 *
 * Encapsulates information about finder patterns in an image, including the location of
 * the three finder patterns, and their estimated module size.
 *
 * @author Sean Owen
 */
class FinderPatternInfo(centers: Array<FinderPattern>) {
    lateinit var bottomLeft: FinderPattern
    lateinit var bottomRight: FinderPattern
    lateinit var topLeft: FinderPattern
    lateinit var topRight: FinderPattern
    var testId: String = ""
    var width: Int = 0
    var height: Int = 0

    init {
        val bottom = max(centers[0].y, max(centers[1].y, max(centers[2].y, centers[3].y)))
        val right = max(centers[0].x, max(centers[1].x, max(centers[2].x, centers[3].x)))

        val centerList = centers.toMutableList()
        for (center in centerList) {
            if (center.x < right / 2 && center.y < bottom / 2) {
                topLeft = center
                centerList.remove(center)
                break
            }
        }

        for (center in centerList) {
            if (center.x > right / 2 && center.y < bottom / 2) {
                topRight = center
                centerList.remove(center)
                break
            }
        }

        for (center in centerList) {
            if (center.x < right / 2 && center.y > bottom / 2) {
                bottomLeft = center
                centerList.remove(center)
                break
            }
        }

        for (center in centers) {
            if (center.x > right / 2 && center.y > bottom / 2) {
                bottomRight = center
                break
            }
        }
    }
}