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

import com.android.tools.perflib.heap.*;
import com.android.tools.perflib.heap.io.MemoryMappedFileBuffer;
import junit.framework.TestCase;
import org.jetbrains.android.AndroidTestBase;

import java.awt.image.BufferedImage;
import java.io.File;

import static com.intellij.openapi.util.io.FileUtil.toCanonicalPath;
import static com.intellij.openapi.util.io.FileUtil.toSystemDependentName;

public class HprofBitmapEvaluatorTest extends TestCase {
  public static final int ARGB_565_INSTANCE = 0x12cff7c0;
  public static final int ARGB_8888_MUTABLE = 0x12cff780;
  public static final int ARGB_8888_INSTANCE = 0x12cff740;
  public static final int BITMAP_DRAWABLE_INSTANCE = 0x12cb0c40;
  public static final int ACTIVITY_INSTANCE = 0x12c722a0;

  private Heap appHeap;

  @Override
  protected void setUp() throws Exception {
    String testDataPath = toCanonicalPath(toSystemDependentName(AndroidTestBase.getTestDataPath()));
    File testHprofFile = new File(testDataPath, toSystemDependentName("hprof/bitmap_evaluator.hprof"));
    assert testHprofFile.exists();
    Snapshot mySnapshot = new HprofParser(new MemoryMappedFileBuffer(testHprofFile)).parse();
    appHeap = mySnapshot.getHeap("app");
    assert appHeap != null;
  }

  public void testGetBitmapFromBitmapBitmap() {
    Instance bitmapDrawable = appHeap.getInstance(ARGB_565_INSTANCE);
    boolean isBitmap = HprofBitmapEvaluator.canGetBitmapFromInstance(bitmapDrawable);
    assert isBitmap;
  }

  public void testGetBitmapFromBitmapDrawable() {
    Instance bitmapDrawable = appHeap.getInstance(BITMAP_DRAWABLE_INSTANCE);
    boolean isBitmap = HprofBitmapEvaluator.canGetBitmapFromInstance(bitmapDrawable);
    assert isBitmap;
  }

  public void testFailGetBitmapFromWrongObject() {
    Instance bitmapDrawable = appHeap.getInstance(ACTIVITY_INSTANCE);
    boolean isBitmap = HprofBitmapEvaluator.canGetBitmapFromInstance(bitmapDrawable);
    assert !isBitmap;
  }

  public void testDecodeARGB888() throws Exception {
    Instance argb888Instance = appHeap.getInstance(ARGB_8888_INSTANCE);
    BufferedImage bitmap = HprofBitmapEvaluator.getBitmap(argb888Instance);
    assert bitmap != null;
  }

  public void testDecodeMutableBitmapWithBigBuffer() throws Exception {
    // 360x360 mutable bitmap with buffer length 640000
    Instance argb888MutableBitmap = appHeap.getInstance(ARGB_8888_MUTABLE);
    BufferedImage bitmap = HprofBitmapEvaluator.getBitmap(argb888MutableBitmap);
    assert bitmap != null;
  }

  public void testFailDecodeRGB565() throws Exception {
    Instance argb565Bitmap = appHeap.getInstance(ARGB_565_INSTANCE);
    BufferedImage bitmap = HprofBitmapEvaluator.getBitmap(argb565Bitmap);
    //we do not decode rgb565
    assert bitmap == null;
  }

  public void testFailDecodeWrongInstance() throws Exception {
    Instance argb565Bitmap = appHeap.getInstance(ACTIVITY_INSTANCE);
    BufferedImage bitmap = HprofBitmapEvaluator.getBitmap(argb565Bitmap);
    assert bitmap == null;
  }
}