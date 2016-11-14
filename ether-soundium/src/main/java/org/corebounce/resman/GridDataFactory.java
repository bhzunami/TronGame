package org.corebounce.resman;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;

public class GridDataFactory {
	public static GridData fill(boolean horizontal, boolean vertical) {
		return fill(horizontal, vertical, SWT.DEFAULT, SWT.DEFAULT);
	}

	public static GridData fill(boolean horizontal, boolean vertical, int width, int height) {
		GridData result = new GridData(width, height);
		if(horizontal) {
			result.grabExcessHorizontalSpace = true;
			result.horizontalAlignment = SWT.FILL;
		}
		if(vertical) {
			result.grabExcessVerticalSpace = true;
			result.verticalAlignment = SWT.FILL;
		}
		return result;
	}

	public static GridData fillSpan(boolean horizontal, boolean vertical, int cols, int rows) {
		GridData result       = fill(horizontal, vertical, SWT.DEFAULT, SWT.DEFAULT);
		result.horizontalSpan = cols;
		result.verticalSpan   = rows;
		return result;
	}
}
