/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.idea.editors.hprof;

import com.android.tools.idea.debug.BitmapEvaluator;
import com.android.tools.perflib.heap.ArrayInstance;
import com.android.tools.perflib.heap.ClassInstance;
import com.android.tools.perflib.heap.Instance;
import com.android.tools.perflib.heap.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;

public class HprofBitmapEvaluator {
  @Nullable
  public static BufferedImage getBitmap(@NotNull Instance value) {
    ClassInstance bitmap = getBitmapClassInstance(value);
    if (bitmap == null) return null;

    ArrayInstance buffer = null;
    Integer width = null;
    Integer height = null;
    Boolean mutable = null;

    for (ClassInstance.FieldValue field : bitmap.getValues()) {
      Object bitmapFieldValue = field.getValue();
      String bitmapFieldName = field.getField().getName();
      if ("mBuffer".equals(bitmapFieldName) && (bitmapFieldValue instanceof ArrayInstance)) {
        ArrayInstance arrayInstance = (ArrayInstance)bitmapFieldValue;
        if (arrayInstance.getArrayType() == Type.BYTE) {
          buffer = arrayInstance;
        }
      }
      else if ("mWidth".equals(bitmapFieldName) && (bitmapFieldValue instanceof Integer)) {
        width = (Integer)bitmapFieldValue;
      }
      else if ("mHeight".equals(bitmapFieldName) && (bitmapFieldValue instanceof Integer)) {
        height = (Integer)bitmapFieldValue;
      }
      else if ("mIsMutable".equals(bitmapFieldName) && (bitmapFieldValue instanceof Boolean)) {
        mutable = (Boolean)bitmapFieldValue;
      }
    }

    if (buffer == null || width == null || height == null || mutable == null) {
      return null;
    }

    Object[] bufferValues = buffer.getValues();

    if (mutable) {
      if (width * height * 4 > bufferValues.length) {
        // Wrong Bitmap.Config
        return null;
      }
    }
    else if (width * height * 4 != bufferValues.length) {
      // Wrong Bitmap.Config
      return null;
    }

    byte[] rgba = new byte[bufferValues.length];
    for (int i = 0; i < bufferValues.length; i++) {
      Object bufferValue = bufferValues[i];
      if (bufferValue instanceof Byte) {
        rgba[i] = (Byte)bufferValue;
      }
      else {
        return null;
      }
    }
    return BitmapEvaluator.createBufferedImage(width, height, rgba);
  }

  public static boolean canGetBitmapFromInstance(@NotNull Instance value) {
    return getBitmapClassInstance(value) != null;
  }

  @Nullable
  private static ClassInstance getBitmapClassInstance(@NotNull Instance value) {
    if (!(value instanceof ClassInstance)) {
      return null;
    }
    ClassInstance selectedObject = (ClassInstance)value;
    String className = value.getClassObj().getClassName();
    ClassInstance bitmap = null;
    if ("android.graphics.Bitmap".equals(className)) {
      bitmap = selectedObject;
    }
    else if ("android.graphics.drawable.BitmapDrawable".equals(className)) {
      bitmap = getBitmapFromBitmapDrawable(selectedObject);
    }
    return bitmap;
  }

  @Nullable
  private static ClassInstance getBitmapFromBitmapDrawable(@NotNull ClassInstance bitmapDrawable) {
    ClassInstance bitmapState = getBitmapStateFromBitmapDrawable(bitmapDrawable);
    if (bitmapState == null) {
      return null;
    }
    for (ClassInstance.FieldValue field : bitmapState.getValues()) {
      String fieldName = field.getField().getName();
      Object fieldValue = field.getValue();
      if ("mBitmap".equals(fieldName) && (fieldValue instanceof ClassInstance)) {
        ClassInstance result = (ClassInstance)fieldValue;
        String className = result.getClassObj().getClassName();
        if ("android.graphics.Bitmap".equals(className)) {
          return result;
        }
      }
    }
    return null;
  }

  @Nullable
  private static ClassInstance getBitmapStateFromBitmapDrawable(@NotNull ClassInstance bitmapDrawable) {
    for (ClassInstance.FieldValue field : bitmapDrawable.getValues()) {
      String fieldName = field.getField().getName();
      Object fieldValue = field.getValue();
      if ("mBitmapState".equals(fieldName) && (fieldValue instanceof ClassInstance)) {
        ClassInstance result = (ClassInstance)fieldValue;
        String className = result.getClassObj().getClassName();
        if ("android.graphics.drawable.BitmapDrawable$BitmapState".equals(className)) {
          return result;
        }
      }
    }
    return null;
  }
}