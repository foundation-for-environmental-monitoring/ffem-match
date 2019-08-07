/*
 * Copyright 2008 ZXing authors
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

package com.google.zxing.oned;

import com.google.zxing.*;
import com.google.zxing.common.BitArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class MultiFormatOneDReader extends OneDReader {

    private static final OneDReader[] EMPTY_ONED_ARRAY = new OneDReader[0];

    private final OneDReader[] readers;

    public MultiFormatOneDReader(Map<DecodeHintType, ?> hints) {
        @SuppressWarnings("unchecked")
        Collection<BarcodeFormat> possibleFormats = hints == null ? null :
                (Collection<BarcodeFormat>) hints.get(DecodeHintType.POSSIBLE_FORMATS);
        boolean useCode39CheckDigit = hints != null &&
                hints.get(DecodeHintType.ASSUME_CODE_39_CHECK_DIGIT) != null;
        Collection<OneDReader> readers = new ArrayList<>();
        if (possibleFormats != null) {
            if (possibleFormats.contains(BarcodeFormat.CODE_128)) {
                readers.add(new Code128Reader());
            }
        }
        if (readers.isEmpty()) {
            readers.add(new Code128Reader());
        }
        this.readers = readers.toArray(EMPTY_ONED_ARRAY);
    }

    @Override
    public Result decodeRow(int rowNumber,
                            BitArray row,
                            Map<DecodeHintType, ?> hints) throws NotFoundException {
        for (OneDReader reader : readers) {
            try {
                return reader.decodeRow(rowNumber, row, hints);
            } catch (ReaderException re) {
                // continue
            }
        }

        throw NotFoundException.getNotFoundInstance();
    }

    @Override
    public void reset() {
        for (Reader reader : readers) {
            reader.reset();
        }
    }

}
