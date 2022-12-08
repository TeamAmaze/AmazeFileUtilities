/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.fileutilities.pdf_viewer;

import com.amaze.fileutilities.R;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.scroll.ScrollHandle;
import com.github.barteksc.pdfviewer.util.Util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

public class PdfScrollHandle extends RelativeLayout implements ScrollHandle {

  private static final int HANDLE_LONG = 65;
  private static final int HANDLE_SHORT = 40;
  private static final int DEFAULT_TEXT_SIZE = 16;

  private float relativeHandlerMiddle = 0f;

  protected TextView textView;
  protected Context context;
  private boolean inverted;
  private PDFView pdfView;
  private float currentPos;

  private Handler handler = new Handler();
  private Runnable hidePageScrollerRunnable =
      new Runnable() {
        @Override
        public void run() {
          hide();
        }
      };

  public PdfScrollHandle(Context context) {
    this(context, false);
  }

  public PdfScrollHandle(Context context, boolean inverted) {
    super(context);
    this.context = context;
    this.inverted = inverted;
    textView = new TextView(context);
    setVisibility(INVISIBLE);
    setTextColor(Color.BLACK);
    setTextSize(DEFAULT_TEXT_SIZE);
  }

  @Override
  public void setupLayout(PDFView pdfView) {
    int align, width, height;
    Drawable background;
    // determine handler position, default is right (when scrolling vertically) or bottom (when
    // scrolling horizontally)
    if (pdfView.isSwipeVertical()) {
      width = HANDLE_LONG;
      height = HANDLE_SHORT;
      if (inverted) { // left
        align = ALIGN_PARENT_LEFT;
        background = ContextCompat.getDrawable(context, R.drawable.default_scroll_handle_left);
      } else { // right
        align = ALIGN_PARENT_RIGHT;
        background =
            getGradientDrawableWithTintAttr(
                R.drawable.afs_md2_thumb, R.attr.colorControlActivated, context);
      }
    } else {
      width = HANDLE_SHORT;
      height = HANDLE_LONG;
      if (inverted) { // top
        align = ALIGN_PARENT_TOP;
        background = ContextCompat.getDrawable(context, R.drawable.default_scroll_handle_top);
      } else { // bottom
        align = ALIGN_PARENT_BOTTOM;
        background = ContextCompat.getDrawable(context, R.drawable.default_scroll_handle_bottom);
      }
    }

    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
      setBackgroundDrawable(background);
    } else {
      setBackground(background);
    }

    LayoutParams lp = new LayoutParams(Util.getDP(context, width), Util.getDP(context, height));
    lp.setMargins(0, 0, 0, 0);

    LayoutParams tvlp =
        new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    tvlp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);

    addView(textView, tvlp);

    lp.addRule(align);
    pdfView.addView(this, lp);

    this.pdfView = pdfView;
  }

  // Work around the bug that GradientDrawable didn't actually implement tinting until
  // Lollipop MR1 (API 22).
  @Nullable
  public static Drawable getGradientDrawableWithTintAttr(
      @DrawableRes int drawableRes, @AttrRes int tintAttrRes, @NonNull Context context) {
    Drawable drawable = AppCompatResources.getDrawable(context, drawableRes);
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1
        && drawable instanceof GradientDrawable) {
      drawable = DrawableCompat.wrap(drawable);
      drawable.setTintList(getColorStateListFromAttrRes(tintAttrRes, context));
    }
    return drawable;
  }

  @Nullable
  public static ColorStateList getColorStateListFromAttrRes(
      @AttrRes int attrRes, @NonNull Context context) {
    TypedArray a = context.obtainStyledAttributes(new int[] {attrRes});
    int resId;
    try {
      resId = a.getResourceId(0, 0);
      if (resId != 0) {
        return AppCompatResources.getColorStateList(context, resId);
      }
      return a.getColorStateList(0);
    } finally {
      a.recycle();
    }
  }

  @Override
  public void destroyLayout() {
    pdfView.removeView(this);
  }

  @Override
  public void setScroll(float position) {
    if (!shown()) {
      show();
    } else {
      handler.removeCallbacks(hidePageScrollerRunnable);
    }
    if (pdfView != null) {
      setPosition(
          (pdfView.isSwipeVertical() ? pdfView.getHeight() : pdfView.getWidth()) * position);
    }
  }

  private void setPosition(float pos) {
    if (Float.isInfinite(pos) || Float.isNaN(pos)) {
      return;
    }
    float pdfViewSize;
    if (pdfView.isSwipeVertical()) {
      pdfViewSize = pdfView.getHeight();
    } else {
      pdfViewSize = pdfView.getWidth();
    }
    pos -= relativeHandlerMiddle;

    if (pos < 0) {
      pos = 0;
    } else if (pos > pdfViewSize - Util.getDP(context, HANDLE_SHORT)) {
      pos = pdfViewSize - Util.getDP(context, HANDLE_SHORT);
    }

    if (pdfView.isSwipeVertical()) {
      setY(pos);
    } else {
      setX(pos);
    }

    calculateMiddle();
    invalidate();
  }

  private void calculateMiddle() {
    float pos, viewSize, pdfViewSize;
    if (pdfView.isSwipeVertical()) {
      pos = getY();
      viewSize = getHeight();
      pdfViewSize = pdfView.getHeight();
    } else {
      pos = getX();
      viewSize = getWidth();
      pdfViewSize = pdfView.getWidth();
    }
    relativeHandlerMiddle = ((pos + relativeHandlerMiddle) / pdfViewSize) * viewSize;
  }

  @Override
  public void hideDelayed() {
    handler.postDelayed(hidePageScrollerRunnable, 1000);
  }

  @Override
  public void setPageNum(int pageNum) {
    String text = String.valueOf(pageNum);
    if (!textView.getText().equals(text)) {
      textView.setText(text);
    }
  }

  @Override
  public boolean shown() {
    return getVisibility() == VISIBLE;
  }

  @Override
  public void show() {
    setVisibility(VISIBLE);
  }

  @Override
  public void hide() {
    setVisibility(INVISIBLE);
  }

  public void setTextColor(int color) {
    textView.setTextColor(color);
  }

  /** @param size text size in dp */
  public void setTextSize(int size) {
    textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
  }

  private boolean isPDFViewReady() {
    return pdfView != null && pdfView.getPageCount() > 0 && !pdfView.documentFitsView();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {

    if (!isPDFViewReady()) {
      return super.onTouchEvent(event);
    }

    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
      case MotionEvent.ACTION_POINTER_DOWN:
        pdfView.stopFling();
        handler.removeCallbacks(hidePageScrollerRunnable);
        if (pdfView.isSwipeVertical()) {
          currentPos = event.getRawY() - getY();
        } else {
          currentPos = event.getRawX() - getX();
        }
      case MotionEvent.ACTION_MOVE:
        if (pdfView.isSwipeVertical()) {
          setPosition(event.getRawY() - currentPos + relativeHandlerMiddle);
          pdfView.setPositionOffset(relativeHandlerMiddle / (float) getHeight(), false);
        } else {
          setPosition(event.getRawX() - currentPos + relativeHandlerMiddle);
          pdfView.setPositionOffset(relativeHandlerMiddle / (float) getWidth(), false);
        }
        return true;
      case MotionEvent.ACTION_CANCEL:
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_POINTER_UP:
        hideDelayed();
        pdfView.performPageSnap();
        return true;
    }

    return super.onTouchEvent(event);
  }
}
